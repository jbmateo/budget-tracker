package app.budgettracker.domain.usecase

import app.budgettracker.domain.logic.Comparison
import app.budgettracker.domain.logic.DayBar
import app.budgettracker.domain.logic.compareDay
import app.budgettracker.domain.logic.compareMonthToDate
import app.budgettracker.domain.logic.compareWeekToDate
import app.budgettracker.domain.logic.computeStatus
import app.budgettracker.domain.logic.lastDays
import app.budgettracker.domain.logic.rangeOf
import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.model.PeriodStatus
import app.budgettracker.domain.repository.BudgetRepository
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.repository.PreferencesRepository
import app.budgettracker.domain.time.AppClock
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

const val RECENT_COUNT = 5
const val BAR_DAYS = 7

data class Dashboard(
    val today: LocalDate,
    val currency: Currency,
    val daily: PeriodStatus?,
    val weekly: PeriodStatus?,
    val monthly: PeriodStatus?,
    val compareDay: Comparison,
    val compareWeek: Comparison,
    val compareMonth: Comparison,
    val bars: List<DayBar>,
    val recent: List<Expense>,
)

/** Everything the dashboard (and the budgets screen) shows, recomputed whenever data or the date changes. */
class ObserveDashboard(
    private val expenses: ExpenseRepository,
    private val budgets: BudgetRepository,
    private val prefs: PreferencesRepository,
    private val clock: AppClock,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<Dashboard> =
        clock.todayFlow.flatMapLatest { today ->
            budgets.observeVersions().flatMapLatest { versions ->
                combine(
                    expenses.observeDailyTotals(totalsFrom(versions, today), today),
                    expenses.observeRecent(RECENT_COUNT),
                    prefs.observeCurrency(),
                ) { totals, recent, currency ->
                    Dashboard(
                        today = today,
                        currency = currency,
                        daily = computeStatus(Period.DAILY, versions, totals, today),
                        weekly = computeStatus(Period.WEEKLY, versions, totals, today),
                        monthly = computeStatus(Period.MONTHLY, versions, totals, today),
                        compareDay = compareDay(today, totals),
                        compareWeek = compareWeekToDate(today, totals),
                        compareMonth = compareMonthToDate(today, totals),
                        bars = lastDays(BAR_DAYS, today, totals, versions),
                        recent = recent,
                    )
                }
            }
        }

    /** Totals must cover the ledger (from the first budgeted month) and the comparisons (last month, last 7 days). */
    private fun totalsFrom(versions: List<BudgetVersion>, today: LocalDate): LocalDate {
        val comparisons = Period.MONTHLY.rangeOf(today).start.minus(DatePeriod(months = 1))
            .let { minOf(it, today.minus(DatePeriod(days = 14))) }
        val ledger = versions.minOfOrNull { it.effectiveFrom }?.let { Period.MONTHLY.rangeOf(it).start }
        return if (ledger == null) comparisons else minOf(ledger, comparisons)
    }
}
