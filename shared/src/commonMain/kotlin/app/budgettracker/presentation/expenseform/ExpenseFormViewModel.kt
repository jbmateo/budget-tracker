package app.budgettracker.presentation.expenseform

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.budgettracker.domain.model.ExpenseCategory
import app.budgettracker.domain.model.DomainError
import app.budgettracker.domain.model.MoneyFormatter
import app.budgettracker.domain.model.Outcome
import app.budgettracker.domain.time.AppClock
import app.budgettracker.domain.usecase.DeleteExpense
import app.budgettracker.domain.usecase.GetExpense
import app.budgettracker.domain.usecase.SaveExpense
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * @property maxDate the latest pickable date (today).
 * @property done true once saved or deleted: the screen closes.
 */
data class ExpenseFormUiState(
    val isEdit: Boolean,
    val isLoading: Boolean,
    val amountText: String,
    val category: ExpenseCategory,
    val date: LocalDate,
    val note: String,
    val maxDate: LocalDate,
    val error: DomainError?,
    val isSaving: Boolean,
    val done: Boolean,
    val categories: List<ExpenseCategory> = ExpenseCategory.entries,
)

/** @param expenseId the expense to edit, or null to add one. */
class ExpenseFormViewModel(
    private val expenseId: String?,
    private val getExpense: GetExpense,
    private val saveExpense: SaveExpense,
    private val deleteExpense: DeleteExpense,
    clock: AppClock,
) : ViewModel() {
    private val today = clock.today()
    private val _state = MutableStateFlow(
        ExpenseFormUiState(
            isEdit = expenseId != null,
            isLoading = expenseId != null,
            amountText = "",
            category = ExpenseCategory.FOOD,
            date = today,
            note = "",
            maxDate = today,
            error = null,
            isSaving = false,
            done = false,
        ),
    )
    val state: StateFlow<ExpenseFormUiState> = _state.asStateFlow()

    init {
        if (expenseId != null) viewModelScope.launch {
            val e = getExpense(expenseId)
            _state.update {
                if (e == null) it.copy(isLoading = false, error = DomainError.NotFound)
                else it.copy(
                    isLoading = false,
                    amountText = MoneyFormatter.plain(e.amount),
                    category = e.category,
                    date = e.date,
                    note = e.note,
                    maxDate = maxOf(today, e.date),
                )
            }
        }
    }

    fun setAmount(text: String) = _state.update { it.copy(amountText = text, error = null) }
    fun setCategory(category: ExpenseCategory) = _state.update { it.copy(category = category) }
    fun setNote(note: String) = _state.update { it.copy(note = note, error = null) }
    fun setDate(year: Int, month: Int, day: Int) = _state.update { it.copy(date = LocalDate(year, month, day), error = null) }

    fun save() {
        val s = _state.value
        if (s.isSaving || s.isLoading || s.done) return
        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            when (val r = saveExpense(expenseId, s.amountText, s.category, s.note, s.date)) {
                is Outcome.Success -> _state.update { it.copy(isSaving = false, done = true) }
                is Outcome.Failure -> _state.update { it.copy(isSaving = false, error = r.error) }
            }
        }
    }

    fun delete() {
        val id = expenseId ?: return
        viewModelScope.launch {
            deleteExpense(id)
            _state.update { it.copy(done = true) }
        }
    }
}
