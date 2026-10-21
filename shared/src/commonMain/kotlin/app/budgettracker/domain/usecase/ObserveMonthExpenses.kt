package app.budgettracker.domain.usecase

import app.budgettracker.domain.logic.CategoryComparison
import app.budgettracker.domain.logic.categoryComparison
import app.budgettracker.domain.logic.monthSpansFor
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.repository.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth

data class DayGroup(val date: LocalDate, val total: Long, val items: List<Expense>)

data class MonthExpenses(
    val month: YearMonth,
    val currency: Currency,
    /** Spent in the compared span: the whole month, or month-to-date for the current month. */
    val total: Long,
    val previousTotal: Long,
    val categories: List<CategoryComparison>,
    /** Newest day first. */
    val days: List<DayGroup>,
)

class ObserveMonthExpenses(private val expenses: ExpenseRepository, private val prefs: PreferencesRepository) {
    operator fun invoke(month: YearMonth, today: LocalDate): Flow<MonthExpenses> {
        val (current, previous) = monthSpansFor(month, today)
        return combine(expenses.observeBetween(previous.start, month.lastDay), prefs.observeCurrency()) { list, currency ->
            val inMonth = list.filter { it.date >= month.firstDay }
            val categories = categoryComparison(list, current, previous)
            MonthExpenses(
                month = month,
                currency = currency,
                total = categories.sumOf { it.current },
                previousTotal = categories.sumOf { it.previous },
                categories = categories,
                days = inMonth.groupBy { it.date }.map { (date, items) -> DayGroup(date, items.sumOf { it.amount }, items) }
                    .sortedByDescending { it.date },
            )
        }
    }
}
