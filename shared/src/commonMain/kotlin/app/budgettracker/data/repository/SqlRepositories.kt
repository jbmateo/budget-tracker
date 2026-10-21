package app.budgettracker.data.repository

import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.coroutines.mapToOneOrNull
import app.budgettracker.data.db.BudgetDatabase
import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Category
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.repository.BudgetRepository
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate
import kotlin.time.Instant
import app.budgettracker.data.db.Expense as ExpenseRow

class SqlExpenseRepository(private val db: BudgetDatabase, private val io: CoroutineDispatcher) : ExpenseRepository {
    private val q = db.expenseQueries

    override fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>> =
        q.dailyTotals(from.toString(), to.toString()) { date, total -> LocalDate.parse(date) to total }
            .asFlow().mapToList(io).map { it.toMap() }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Expense>> =
        q.selectBetween(from.toString(), to.toString()).asFlow().mapToList(io).map { rows -> rows.map(::toDomain) }

    override fun observeRecent(limit: Int): Flow<List<Expense>> =
        q.selectRecent(limit.toLong()).asFlow().mapToList(io).map { rows -> rows.map(::toDomain) }

    override suspend fun get(id: String): Expense? = withContext(io) { q.selectById(id).executeAsOneOrNull()?.let(::toDomain) }

    override suspend fun upsert(expense: Expense) = withContext(io) {
        q.upsert(
            ExpenseRow(
                expense.id, expense.amount, expense.category.key, expense.note, expense.date.toString(),
                expense.createdAt.toEpochMilliseconds(),
            ),
        )
        Unit
    }

    override suspend fun delete(id: String) = withContext(io) {
        q.delete(id)
        Unit
    }

    private fun toDomain(row: ExpenseRow) = Expense(
        id = row.id,
        amount = row.amount,
        category = Category.fromKey(row.category),
        note = row.note,
        date = LocalDate.parse(row.date),
        createdAt = Instant.fromEpochMilliseconds(row.created_at),
    )
}

class SqlBudgetRepository(private val db: BudgetDatabase, private val io: CoroutineDispatcher) : BudgetRepository {
    private val q = db.budgetVersionQueries

    override fun observeVersions(): Flow<List<BudgetVersion>> =
        q.selectAll().asFlow().mapToList(io).map { rows ->
            rows.mapNotNull { row ->
                Period.entries.firstOrNull { it.name == row.period }
                    ?.let { BudgetVersion(it, row.amount, LocalDate.parse(row.effective_from)) }
            }
        }

    override suspend fun put(version: BudgetVersion) = withContext(io) {
        q.upsert(version.period.name, version.amount, version.effectiveFrom.toString())
        Unit
    }
}

class SqlPreferencesRepository(private val db: BudgetDatabase, private val io: CoroutineDispatcher) : PreferencesRepository {
    private val q = db.preferenceQueries

    override fun observeCurrency(): Flow<Currency> =
        q.selectValue(CURRENCY).asFlow().mapToOneOrNull(io).map(Currency::fromCode).distinctUntilChanged()

    override suspend fun setCurrency(currency: Currency) = withContext(io) {
        q.upsert(CURRENCY, currency.code)
        Unit
    }

    private companion object {
        const val CURRENCY = "currency"
    }
}
