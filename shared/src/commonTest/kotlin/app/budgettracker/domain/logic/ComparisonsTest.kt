package app.budgettracker.domain.logic

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Category
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.Period
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ComparisonsTest {
    private fun d(s: String) = LocalDate.parse(s)

    @Test
    fun deltaPercent() {
        assertEquals(20, Comparison(120, 100).deltaPercent)
        assertEquals(-50, Comparison(50, 100).deltaPercent)
        assertNull(Comparison(50, 0).deltaPercent)
        assertEquals(0, Comparison(0, 0).let { if (it.previous == 0L) 0 else it.deltaPercent })
    }

    @Test
    fun dayComparison() {
        val t = mapOf(d("2026-10-21") to 300L, d("2026-10-20") to 200L)
        assertEquals(Comparison(300, 200), compareDay(d("2026-10-21"), t))
    }

    @Test
    fun weekToDateComparesSameWeekdays() {
        // Wed 2026-10-21: this week Mon..Wed vs last week Mon 10-12..Wed 10-14.
        val t = mapOf(
            d("2026-10-19") to 100L, d("2026-10-21") to 50L,
            d("2026-10-12") to 70L, d("2026-10-14") to 10L, d("2026-10-15") to 999L,
        )
        assertEquals(Comparison(150, 80), compareWeekToDate(d("2026-10-21"), t))
    }

    @Test
    fun monthToDateClampsToShorterMonth() {
        val t = mapOf(
            d("2026-03-31") to 100L, d("2026-03-01") to 1L,
            d("2026-02-28") to 40L, d("2026-02-01") to 2L, d("2026-01-31") to 999L,
        )
        assertEquals(Comparison(101, 42), compareMonthToDate(d("2026-03-31"), t))
        // Mid-month: Oct 1..21 vs Sep 1..21.
        val t2 = mapOf(d("2026-10-21") to 5L, d("2026-09-21") to 7L, d("2026-09-22") to 999L)
        assertEquals(Comparison(5, 7), compareMonthToDate(d("2026-10-21"), t2))
    }

    private fun e(cat: Category, amount: Long, date: String) =
        Expense("id-$date-$amount", amount, cat, "", d(date), Instant.fromEpochMilliseconds(0))

    @Test
    fun categoriesMonthToDateSortedByCurrent() {
        val list = listOf(
            e(Category.FOOD, 100, "2026-10-02"), e(Category.FOOD, 50, "2026-10-20"),
            e(Category.TRANSPORT, 300, "2026-10-05"),
            e(Category.FOOD, 80, "2026-09-10"), e(Category.BILLS, 500, "2026-09-03"),
            e(Category.SHOPPING, 900, "2026-09-25"), // after Sep 21 → outside last month's span
        )
        assertEquals(
            listOf(
                CategoryComparison(Category.TRANSPORT, 300, 0),
                CategoryComparison(Category.FOOD, 150, 80),
                CategoryComparison(Category.BILLS, 0, 500),
            ),
            categoryMonthToDate(d("2026-10-21"), list),
        )
    }

    @Test
    fun lastDaysBars() {
        val v = listOf(BudgetVersion(Period.DAILY, 500, d("2026-10-20")))
        val bars = lastDays(3, d("2026-10-21"), mapOf(d("2026-10-21") to 10L, d("2026-10-19") to 7L), v)
        assertEquals(
            listOf(DayBar(d("2026-10-19"), 7, null), DayBar(d("2026-10-20"), 0, 500), DayBar(d("2026-10-21"), 10, 500)),
            bars,
        )
    }
}
