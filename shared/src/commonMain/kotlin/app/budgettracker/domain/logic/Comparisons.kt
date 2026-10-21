package app.budgettracker.domain.logic

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Category
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.model.PeriodRange
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlin.math.roundToInt

/** Spending over a span of days vs the same span in the previous period. */
data class Comparison(val current: Long, val previous: Long) {
    /** Percent change, or null when there was nothing to compare with. */
    val deltaPercent: Int? get() = if (previous == 0L) null else ((current - previous) * 100.0 / previous).roundToInt()
}

data class CategoryComparison(val category: Category, val current: Long, val previous: Long)

/** One bar of the recent-days chart. [budget] is that day's daily budget, if one was set. */
data class DayBar(val date: LocalDate, val spent: Long, val budget: Long?)

fun compareDay(today: LocalDate, totals: Map<LocalDate, Long>): Comparison =
    Comparison(totals[today] ?: 0L, totals[today.minus(DatePeriod(days = 1))] ?: 0L)

fun compareWeekToDate(today: LocalDate, totals: Map<LocalDate, Long>): Comparison {
    val (current, previous) = weekSpans(today)
    return Comparison(totals.spentIn(current), totals.spentIn(previous))
}

fun compareMonthToDate(today: LocalDate, totals: Map<LocalDate, Long>): Comparison {
    val (current, previous) = monthSpans(today)
    return Comparison(totals.spentIn(current), totals.spentIn(previous))
}

/** Per-category month-to-date vs the same span last month; biggest current first, rows with no spending dropped. */
fun categoryMonthToDate(today: LocalDate, expenses: List<Expense>): List<CategoryComparison> {
    val (current, previous) = monthSpans(today)
    return Category.entries
        .map { cat ->
            val mine = expenses.filter { it.category == cat }
            CategoryComparison(
                cat,
                mine.filter { it.date in current }.sumOf { it.amount },
                mine.filter { it.date in previous }.sumOf { it.amount },
            )
        }
        .filter { it.current > 0 || it.previous > 0 }
        .sortedWith(compareByDescending<CategoryComparison> { it.current }.thenByDescending { it.previous })
}

/** The last [n] days ending today, oldest first. */
fun lastDays(n: Int, today: LocalDate, totals: Map<LocalDate, Long>, versions: List<BudgetVersion>): List<DayBar> {
    val daily = versions.filter { it.period == Period.DAILY }
    return (n - 1 downTo 0).map { back ->
        val date = today.minus(DatePeriod(days = back))
        DayBar(date, totals[date] ?: 0L, budgetFor(daily, PeriodRange(date, date), today))
    }
}

private fun weekSpans(today: LocalDate): Pair<PeriodRange, PeriodRange> {
    val current = PeriodRange(Period.WEEKLY.rangeOf(today).start, today)
    val week = DatePeriod(days = 7)
    return current to PeriodRange(current.start.minus(week), today.minus(week))
}

/** Month-to-date and the same days last month, clamped to last month's length. */
internal fun monthSpans(today: LocalDate): Pair<PeriodRange, PeriodRange> {
    val current = PeriodRange(Period.MONTHLY.rangeOf(today).start, today)
    val prevMonth = YearMonth(today.year, today.month).let { it.firstDay.minus(DatePeriod(days = 1)) }
        .let { YearMonth(it.year, it.month) }
    val prevEnd = prevMonth.firstDay.plus(DatePeriod(days = minOf(today.day, prevMonth.lastDay.day) - 1))
    return current to PeriodRange(prevMonth.firstDay, prevEnd)
}
