package app.budgettracker.fakes

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.repository.BudgetRepository
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.datetime.LocalDate

class FakeExpenseRepository : ExpenseRepository {
    val items = MutableStateFlow<Map<String, Expense>>(emptyMap())

    override fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>> = items.map { all ->
        all.values.filter { it.date in from..to }.groupBy { it.date }.mapValues { (_, v) -> v.sumOf { it.amount } }
    }

    override fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Expense>> = items.map { all ->
        all.values.filter { it.date in from..to }.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt })
    }

    override fun observeRecent(limit: Int): Flow<List<Expense>> = items.map { all ->
        all.values.sortedWith(compareByDescending<Expense> { it.date }.thenByDescending { it.createdAt }).take(limit)
    }

    override suspend fun get(id: String): Expense? = items.value[id]
    override suspend fun upsert(expense: Expense) = items.update { it + (expense.id to expense) }
    override suspend fun delete(id: String) = items.update { it - id }
}

class FakeBudgetRepository : BudgetRepository {
    val versions = MutableStateFlow<List<BudgetVersion>>(emptyList())
    override fun observeVersions(): Flow<List<BudgetVersion>> = versions
    override suspend fun put(version: BudgetVersion) = versions.update { list ->
        list.filterNot { it.period == version.period && it.effectiveFrom == version.effectiveFrom } + version
    }
}

class FakePreferencesRepository : PreferencesRepository {
    val currency = MutableStateFlow(Currency.PHP)
    override fun observeCurrency(): Flow<Currency> = currency
    override suspend fun setCurrency(currency: Currency) { this.currency.value = currency }
}
