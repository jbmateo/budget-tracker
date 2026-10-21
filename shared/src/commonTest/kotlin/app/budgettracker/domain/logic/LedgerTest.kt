package app.budgettracker.domain.logic

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Period
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LedgerTest {
    private fun d(s: String) = LocalDate.parse(s)
    private fun daily(amount: Long?, from: String) = BudgetVersion(Period.DAILY, amount, d(from))

    // Mon 2026-10-19 .. Thu 2026-10-22, daily ₱500 (spec §3.4 worked example).
    private val versions = listOf(daily(50_000, "2026-10-19"))
    private val totals = mapOf(d("2026-10-19") to 30_000L, d("2026-10-20") to 45_000L, d("2026-10-21") to 90_000L)

    @Test
    fun workedExample() {
        val tue = computeStatus(Period.DAILY, versions, totals, d("2026-10-20"))!!
        assertEquals(20_000, tue.carry)
        assertEquals(70_000, tue.available)
        assertEquals(25_000, tue.left)

        val wed = computeStatus(Period.DAILY, versions, totals, d("2026-10-21"))!!
        assertEquals(25_000, wed.carry)
        assertEquals(-15_000, wed.left)

        val thu = computeStatus(Period.DAILY, versions, totals, d("2026-10-22"))!!
        assertEquals(50_000, thu.budget)
        assertEquals(-15_000, thu.carry)
        assertEquals(35_000, thu.available)
        assertEquals(0, thu.spent)
    }

    @Test
    fun dayWithNoSpendAddsWholeBudget() {
        val s = computeStatus(Period.DAILY, versions, emptyMap(), d("2026-10-22"))!!
        assertEquals(150_000, s.carry)
    }

    @Test
    fun gapKeepsCarryAndSkipsSpending() {
        val v = listOf(daily(50_000, "2026-10-19"), daily(null, "2026-10-20"), daily(50_000, "2026-10-22"))
        val t = mapOf(d("2026-10-19") to 10_000L, d("2026-10-20") to 999_999L, d("2026-10-21") to 999_999L)
        val s = computeStatus(Period.DAILY, v, t, d("2026-10-23"))!!
        // Mon +40,000; Tue/Wed no budget (skipped); Thu +50,000.
        assertEquals(90_000, s.carry)
    }

    @Test
    fun noBudgetTodayReturnsNull() {
        val v = listOf(daily(50_000, "2026-10-19"), daily(null, "2026-10-21"))
        assertNull(computeStatus(Period.DAILY, v, totals, d("2026-10-21")))
        assertNull(computeStatus(Period.DAILY, emptyList(), totals, d("2026-10-21")))
    }

    @Test
    fun otherPeriodsVersionsAreIgnored() {
        val v = versions + BudgetVersion(Period.MONTHLY, 1, d("2026-01-01"))
        assertEquals(25_000, computeStatus(Period.DAILY, v, totals, d("2026-10-21"))!!.carry)
    }

    @Test
    fun weeklyCarryAcrossMonthBoundary() {
        val v = listOf(BudgetVersion(Period.WEEKLY, 300_000, d("2026-09-30")))
        // Week Sep 28..Oct 4: spent 100,000 split across the month boundary.
        val t = mapOf(d("2026-09-30") to 40_000L, d("2026-10-02") to 60_000L, d("2026-10-06") to 5_000L)
        val s = computeStatus(Period.WEEKLY, v, t, d("2026-10-07"))!!
        assertEquals(200_000, s.carry)
        assertEquals(5_000, s.spent)
        assertEquals(495_000, s.left)
    }

    @Test
    fun monthlyCarry() {
        val v = listOf(BudgetVersion(Period.MONTHLY, 1_000_000, d("2026-09-12")))
        val t = mapOf(d("2026-09-15") to 900_000L, d("2026-10-03") to 50_000L)
        val s = computeStatus(Period.MONTHLY, v, t, d("2026-10-21"))!!
        assertEquals(100_000, s.carry)
        assertEquals(1_050_000, s.left)
        assertEquals(11, s.range.daysLeft(d("2026-10-21")))
    }

    @Test
    fun editingPastDayChangesCurrentCarry() {
        val edited = totals + (d("2026-10-19") to 10_000L)
        assertEquals(45_000, computeStatus(Period.DAILY, versions, edited, d("2026-10-21"))!!.carry)
    }
}
