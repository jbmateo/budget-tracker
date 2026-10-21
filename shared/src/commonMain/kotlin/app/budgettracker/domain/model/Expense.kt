package app.budgettracker.domain.model

import kotlinx.datetime.LocalDate
import kotlin.time.Instant

const val MAX_NOTE_LENGTH = 80

data class Expense(
    val id: String,
    val amount: Long,
    val category: Category,
    val note: String,
    val date: LocalDate,
    val createdAt: Instant,
)

/** Fixed expense categories. Stored by [key]; unknown keys resolve to [OTHER]. */
enum class Category(val label: String) {
    FOOD("Food"),
    GROCERIES("Groceries"),
    TRANSPORT("Transport"),
    SHOPPING("Shopping"),
    BILLS("Bills"),
    ENTERTAINMENT("Fun"),
    HEALTH("Health"),
    OTHER("Other");

    val key: String get() = name

    companion object {
        fun fromKey(key: String): Category = entries.firstOrNull { it.name == key } ?: OTHER
    }
}
