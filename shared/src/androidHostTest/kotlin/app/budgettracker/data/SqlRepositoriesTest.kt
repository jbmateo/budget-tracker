package app.budgettracker.data

import app.budgettracker.data.repository.SqlBudgetRepository
import app.budgettracker.data.repository.SqlExpenseRepository
import app.budgettracker.data.repository.SqlPreferencesRepository
import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.ExpenseCategory
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.Period
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class SqlRepositoriesTest {
    private val db = inMemoryDb()
    private val expenses = SqlExpenseRepository(db, Dispatchers.Unconfined)
    private val budgets = SqlBudgetRepository(db, Dispatchers.Unconfined)
    private val prefs = SqlPreferencesRepository(db, Dispatchers.Unconfined)
    private fun d(s: String) = LocalDate.parse(s)
    private fun e(id: String, amount: Long, date: String, created: Long = 0) =
        Expense(id, amount, ExpenseCategory.FOOD, "note", d(date), Instant.fromEpochMilliseconds(created))

    @Test
    fun dailyTotalsGroupByDateWithinRange() = runTest {
        expenses.upsert(e("a", 100, "2026-10-20"))
        expenses.upsert(e("b", 250, "2026-10-20"))
        expenses.upsert(e("c", 70, "2026-10-21"))
        expenses.upsert(e("d", 999, "2026-10-22"))
        assertEquals(
            mapOf(d("2026-10-20") to 350L, d("2026-10-21") to 70L),
            expenses.observeDailyTotals(d("2026-10-20"), d("2026-10-21")).first(),
        )
    }

    @Test
    fun upsertEditsAndDeleteRemoves() = runTest {
        expenses.upsert(e("a", 100, "2026-10-20"))
        expenses.upsert(e("a", 300, "2026-10-19"))
        assertEquals(300, expenses.get("a")!!.amount)
        assertEquals(d("2026-10-19"), expenses.get("a")!!.date)
        expenses.delete("a")
        assertNull(expenses.get("a"))
    }

    @Test
    fun recentIsNewestFirst() = runTest {
        expenses.upsert(e("old", 1, "2026-10-01"))
        expenses.upsert(e("new1", 1, "2026-10-21", created = 1))
        expenses.upsert(e("new2", 1, "2026-10-21", created = 2))
        assertEquals(listOf("new2", "new1"), expenses.observeRecent(2).first().map { it.id })
    }

    @Test
    fun unknownCategoryReadsAsOther() = runTest {
        db.expenseQueries.upsert(app.budgettracker.data.db.Expense("x", 5, "PETS", "", "2026-10-21", 0))
        assertEquals(ExpenseCategory.OTHER, expenses.get("x")!!.category)
    }

    @Test
    fun sameDayBudgetChangeReplacesVersion() = runTest {
        budgets.put(BudgetVersion(Period.DAILY, 500, d("2026-10-21")))
        budgets.put(BudgetVersion(Period.DAILY, 700, d("2026-10-21")))
        budgets.put(BudgetVersion(Period.MONTHLY, null, d("2026-10-21")))
        assertEquals(
            listOf(BudgetVersion(Period.DAILY, 700, d("2026-10-21")), BudgetVersion(Period.MONTHLY, null, d("2026-10-21"))),
            budgets.observeVersions().first(),
        )
    }

    @Test
    fun currencyDefaultsToPesoAndPersists() = runTest {
        assertEquals(Currency.PHP, prefs.observeCurrency().first())
        prefs.setCurrency(Currency.USD)
        assertEquals(Currency.USD, prefs.observeCurrency().first())
    }
}
