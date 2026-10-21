package app.budgettracker.domain.logic

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.model.PeriodRange
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.daysUntil
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The period containing [date]. Weeks run Monday–Sunday; months are calendar months. */
fun Period.rangeOf(date: LocalDate): PeriodRange = when (this) {
    Period.DAILY -> PeriodRange(date, date)
    Period.WEEKLY -> {
        val start = date.minus(DatePeriod(days = date.dayOfWeek.isoDayNumber - 1))
        PeriodRange(start, start.plus(DatePeriod(days = 6)))
    }
    Period.MONTHLY -> YearMonth(date.year, date.month).let { PeriodRange(it.firstDay, it.lastDay) }
}

fun PeriodRange.next(period: Period): PeriodRange = period.rangeOf(end.plus(DatePeriod(days = 1)))

/** Days remaining including [today]. */
fun PeriodRange.daysLeft(today: LocalDate): Int = today.daysUntil(end) + 1

/** Every date in the range, in order. */
fun PeriodRange.dates(): Sequence<LocalDate> =
    generateSequence(start) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= end }

/**
 * Budget amount for [range], from versions of a single period type: the latest version effective on or before
 * `min(range.end, today)`. So a change applies to the current period at once and never to periods already over.
 */
fun budgetFor(versions: List<BudgetVersion>, range: PeriodRange, today: LocalDate): Long? {
    val cutoff = minOf(range.end, today)
    return versions.filter { it.effectiveFrom <= cutoff }.maxByOrNull { it.effectiveFrom }?.amount
}
