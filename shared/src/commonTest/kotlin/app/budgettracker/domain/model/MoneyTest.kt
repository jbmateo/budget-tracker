package app.budgettracker.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MoneyTest {
    @Test
    fun parsesWholeAndDecimalAmounts() {
        assertEquals(123400, MoneyParser.parse("1234"))
        assertEquals(123450, MoneyParser.parse("1,234.5"))
        assertEquals(1205, MoneyParser.parse(" 12.05 "))
        assertEquals(50, MoneyParser.parse(".5"))
        assertEquals(1200, MoneyParser.parse("12."))
        assertEquals(99_999_999_999, MoneyParser.parse("999999999.99"))
    }

    @Test
    fun rejectsInvalidAmounts() {
        listOf("", " ", "0", "0.00", "1.234", "abc", "-5", "1000000000", "1.2.3", ".", "1e3", "99999999999999999999")
            .forEach { assertNull(MoneyParser.parse(it), "expected null for '$it'") }
    }

    @Test
    fun formatsWithSymbolGroupingAndSign() {
        assertEquals("₱1,234.50", MoneyFormatter.format(123450, Currency.PHP))
        assertEquals("-₱80.00", MoneyFormatter.format(-8000, Currency.PHP))
        assertEquals("$0.05", MoneyFormatter.format(5, Currency.USD))
        assertEquals("₱0.00", MoneyFormatter.format(0, Currency.PHP))
        assertEquals("₱1,000,000.00", MoneyFormatter.format(100_000_000, Currency.PHP))
    }

    @Test
    fun plainFormatRoundTripsThroughParser() {
        assertEquals("1234.50", MoneyFormatter.plain(123450))
        assertEquals("0.05", MoneyFormatter.plain(5))
        assertEquals(123450, MoneyParser.parse(MoneyFormatter.plain(123450)))
    }

    @Test
    fun unknownCurrencyAndCategoryFallBack() {
        assertEquals(Currency.PHP, Currency.fromCode("XYZ"))
        assertEquals(Currency.PHP, Currency.fromCode(null))
        assertEquals(Currency.USD, Currency.fromCode("USD"))
        assertEquals(ExpenseCategory.OTHER, ExpenseCategory.fromKey("PETS"))
        assertEquals(ExpenseCategory.FOOD, ExpenseCategory.fromKey("FOOD"))
    }
}
