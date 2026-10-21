package app.budgettracker.domain.logic

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.model.PeriodRange
import app.budgettracker.domain.model.PeriodStatus
import kotlinx.datetime.LocalDate

/**
 * Current status of the [period] budget (spec §3.4). Walks every completed period from the first version on;
 * each one with a budget adds (budget − spent) to the carry, periods without a budget are skipped.
 * Returns null when the current period has no budget. [dailyTotals] must cover the first version's period onward.
 */
fun computeStatus(
    period: Period,
    versions: List<BudgetVersion>,
    dailyTotals: Map<LocalDate, Long>,
    today: LocalDate,
): PeriodStatus? {
    val own = versions.filter { it.period == period }
    val first = own.minByOrNull { it.effectiveFrom } ?: return null
    val current = period.rangeOf(today)
    var range = period.rangeOf(first.effectiveFrom)
    var carry = 0L
    while (range.start < current.start) {
        budgetFor(own, range, today)?.let { carry += it - dailyTotals.spentIn(range) }
        range = range.next(period)
    }
    val budget = budgetFor(own, current, today) ?: return null
    return PeriodStatus(period, current, budget, carry, dailyTotals.spentIn(current), current.daysLeft(today))
}

internal fun Map<LocalDate, Long>.spentIn(range: PeriodRange): Long = range.dates().sumOf { this[it] ?: 0L }
