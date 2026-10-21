package app.budgettracker.domain.logic

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.model.PeriodRange
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PeriodsTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun ranges() {
        assertEquals(PeriodRange(d("2026-10-21"), d("2026-10-21")), Period.DAILY.rangeOf(d("2026-10-21")))
        assertEquals(PeriodRange(d("2026-10-19"), d("2026-10-25")), Period.WEEKLY.rangeOf(d("2026-10-21")))
        assertEquals(PeriodRange(d("2026-10-19"), d("2026-10-25")), Period.WEEKLY.rangeOf(d("2026-10-25")))
        assertEquals(PeriodRange(d("2026-12-28"), d("2027-01-03")), Period.WEEKLY.rangeOf(d("2027-01-01")))
        assertEquals(PeriodRange(d("2028-02-01"), d("2028-02-29")), Period.MONTHLY.rangeOf(d("2028-02-10")))
    }

    @Test
    fun nextAndDaysLeft() {
        val week = Period.WEEKLY.rangeOf(d("2026-10-21"))
        assertEquals(PeriodRange(d("2026-10-26"), d("2026-11-01")), week.next(Period.WEEKLY))
        assertEquals(5, week.daysLeft(d("2026-10-21")))
        assertEquals(1, Period.MONTHLY.rangeOf(d("2026-10-31")).daysLeft(d("2026-10-31")))
    }

    private fun v(amount: Long?, from: String) = BudgetVersion(Period.MONTHLY, amount, d(from))

    @Test
    fun midPeriodChangeAppliesToCurrentPeriodOnly() {
        val versions = listOf(v(100, "2026-08-05"), v(200, "2026-10-15"))
        val today = d("2026-10-21")
        assertEquals(200, budgetFor(versions, Period.MONTHLY.rangeOf(today), today))
        assertEquals(100, budgetFor(versions, Period.MONTHLY.rangeOf(d("2026-09-10")), today))
        assertEquals(100, budgetFor(versions, Period.MONTHLY.rangeOf(d("2026-08-01")), today))
        assertNull(budgetFor(versions, Period.MONTHLY.rangeOf(d("2026-07-01")), today))
    }

    @Test
    fun removedBudgetResolvesToNull() {
        val versions = listOf(v(100, "2026-08-05"), v(null, "2026-10-02"))
        assertNull(budgetFor(versions, Period.MONTHLY.rangeOf(d("2026-10-21")), d("2026-10-21")))
    }
}
