package app.budgettracker.domain.model

/** Expected failures; returned, never thrown. */
enum class DomainError { InvalidAmount, NoteTooLong, FutureDate, NotFound }

sealed interface Outcome<out T> {
    data class Success<T>(val value: T) : Outcome<T>
    data class Failure(val error: DomainError) : Outcome<Nothing>
}
