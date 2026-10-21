package app.budgettracker.domain.model

import kotlinx.datetime.LocalDate

enum class Period { DAILY, WEEKLY, MONTHLY }

/** Inclusive date range of one period. */
data class PeriodRange(val start: LocalDate, val end: LocalDate) {
    operator fun contains(date: LocalDate): Boolean = date in start..end
}

/** One change to a period's budget. [amount] null means the budget was removed from [effectiveFrom] on. */
data class BudgetVersion(val period: Period, val amount: Long?, val effectiveFrom: LocalDate)

/**
 * The current period of one budget. [carry] is the signed sum of every earlier budgeted period's (budget − spent):
 * positive = rolled-over savings, negative = earlier overspending deducted now.
 */
data class PeriodStatus(
    val period: Period,
    val range: PeriodRange,
    val budget: Long,
    val carry: Long,
    val spent: Long,
) {
    val available: Long get() = budget + carry
    val left: Long get() = available - spent
}
