package app.budgettracker.di

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.ExpenseCategory
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.repository.BudgetRepository
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.time.AppClock
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.minus
import org.koin.core.Koin
import kotlin.time.Instant

/**
 * Debug builds only: fills an empty database with ~6 weeks of believable spending and three budgets, for
 * screenshots and trying the app. Does nothing when any budget already exists.
 */
suspend fun seedSampleData(koin: Koin) {
    val budgets = koin.get<BudgetRepository>()
    if (budgets.observeVersions().first().isNotEmpty()) return
    val expenses = koin.get<ExpenseRepository>()
    val today = koin.get<AppClock>().today()
    budgets.put(BudgetVersion(Period.DAILY, 50_000, today.minus(DatePeriod(days = 41))))
    budgets.put(BudgetVersion(Period.WEEKLY, 320_000, today.minus(DatePeriod(days = 41))))
    budgets.put(BudgetVersion(Period.MONTHLY, 1_400_000, today.minus(DatePeriod(days = 41))))

    val pattern = listOf(
        Triple(ExpenseCategory.FOOD, 9_500L, "Lunch"),
        Triple(ExpenseCategory.TRANSPORT, 6_500L, "Grab"),
        Triple(ExpenseCategory.FOOD, 6_000L, "Coffee"),
        Triple(ExpenseCategory.GROCERIES, 28_000L, "Weekly groceries"),
        Triple(ExpenseCategory.FOOD, 14_000L, "Dinner"),
        Triple(ExpenseCategory.ENTERTAINMENT, 22_000L, "Movie"),
        Triple(ExpenseCategory.SHOPPING, 39_900L, "Shoes"),
        Triple(ExpenseCategory.BILLS, 49_900L, "Internet"),
        Triple(ExpenseCategory.HEALTH, 18_000L, "Pharmacy"),
        Triple(ExpenseCategory.TRANSPORT, 2_600L, "Jeep"),
    )
    var n = 0
    for (back in 41 downTo 0) {
        val date = today.minus(DatePeriod(days = back))
        val count = if (back == 0) 2 else 1 + (back * 7 % 3)
        repeat(count) { i ->
            val (cat, base, note) = pattern[(back * 3 + i * 5) % pattern.size]
            val amount = base + (back * 137 + i * 911) % 2_500
            expenses.upsert(Expense("sample-${n++}", amount, cat, note, date, Instant.fromEpochMilliseconds(n.toLong())))
        }
    }
}
