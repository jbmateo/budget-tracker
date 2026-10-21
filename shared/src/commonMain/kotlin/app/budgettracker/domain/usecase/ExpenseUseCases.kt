package app.budgettracker.domain.usecase

import app.budgettracker.domain.model.ExpenseCategory
import app.budgettracker.domain.model.DomainError
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.MAX_NOTE_LENGTH
import app.budgettracker.domain.model.MoneyParser
import app.budgettracker.domain.model.Outcome
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.time.AppClock
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class GetExpense(private val repo: ExpenseRepository) {
    suspend operator fun invoke(id: String): Expense? = repo.get(id)
}

/** Creates ([id] null) or edits an expense after validating it (spec §3.2). */
class SaveExpense(
    private val repo: ExpenseRepository,
    private val clock: AppClock,
    private val newId: () -> String,
    private val now: () -> Instant,
) {
    suspend operator fun invoke(
        id: String?,
        amountText: String,
        category: ExpenseCategory,
        note: String,
        date: LocalDate,
    ): Outcome<Expense> {
        val amount = MoneyParser.parse(amountText) ?: return Outcome.Failure(DomainError.InvalidAmount)
        val trimmed = note.trim()
        if (trimmed.length > MAX_NOTE_LENGTH) return Outcome.Failure(DomainError.NoteTooLong)
        if (date > clock.today()) return Outcome.Failure(DomainError.FutureDate)
        val createdAt = if (id == null) now() else repo.get(id)?.createdAt ?: return Outcome.Failure(DomainError.NotFound)
        val expense = Expense(id ?: newId(), amount, category, trimmed, date, createdAt)
        repo.upsert(expense)
        return Outcome.Success(expense)
    }
}

class DeleteExpense(private val repo: ExpenseRepository) {
    suspend operator fun invoke(id: String) = repo.delete(id)
}
