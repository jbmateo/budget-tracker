package app.budgettracker.domain.model

import kotlin.math.absoluteValue

/** Largest accepted amount: 999,999,999.99 in minor units. */
const val MAX_AMOUNT: Long = 99_999_999_999L

/** Display currencies. All use 2 decimals; switching only changes the symbol, amounts are never converted. */
enum class Currency(val symbol: String) {
    PHP("₱"), USD("$"), EUR("€"), GBP("£"), SGD("S$"), AUD("A$"), CAD("C$");

    val code: String get() = name

    companion object {
        fun fromCode(code: String?): Currency = entries.firstOrNull { it.name == code } ?: PHP
    }
}

/** Parses user input such as `1,234.50` into minor units. Returns null unless 0 < amount ≤ [MAX_AMOUNT]. */
object MoneyParser {
    private val pattern = Regex("""^(\d*)(?:\.(\d{0,2}))?$""")

    fun parse(input: String): Long? {
        val cleaned = input.trim().replace(",", "")
        val match = pattern.matchEntire(cleaned) ?: return null
        val whole = match.groupValues[1]
        val fraction = match.groupValues[2]
        if (whole.isEmpty() && fraction.isEmpty()) return null
        if (whole.length > 9) return null
        val minor = (whole.ifEmpty { "0" }.toLong()) * 100 + fraction.padEnd(2, '0').toLong()
        return minor.takeIf { it in 1..MAX_AMOUNT }
    }
}

object MoneyFormatter {
    /** `₱1,234.50`, `-₱80.00`. */
    fun format(minor: Long, currency: Currency): String {
        val sign = if (minor < 0) "-" else ""
        val abs = minor.absoluteValue
        val whole = (abs / 100).toString().reversed().chunked(3).joinToString(",").reversed()
        return "$sign${currency.symbol}$whole.${fraction(abs)}"
    }

    /** `1234.50` — the form's editable text for an existing amount. */
    fun plain(minor: Long): String = "${minor / 100}.${fraction(minor.absoluteValue)}"

    private fun fraction(abs: Long) = (abs % 100).toString().padStart(2, '0')
}
