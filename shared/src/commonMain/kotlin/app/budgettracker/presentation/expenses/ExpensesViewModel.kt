package app.budgettracker.presentation.expenses

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.budgettracker.domain.time.AppClock
import app.budgettracker.domain.usecase.DeleteExpense
import app.budgettracker.domain.usecase.MonthExpenses
import app.budgettracker.domain.usecase.ObserveMonthExpenses
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.YearMonth
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** [data] is null while the month loads. [canGoNext] is false on the current month. */
data class ExpensesUiState(val year: Int, val monthNumber: Int, val canGoNext: Boolean, val data: MonthExpenses?)

@OptIn(ExperimentalCoroutinesApi::class)
class ExpensesViewModel(
    private val observeMonth: ObserveMonthExpenses,
    private val deleteExpense: DeleteExpense,
    private val clock: AppClock,
) : ViewModel() {
    private val month = MutableStateFlow(clock.today().yearMonth())

    val state: StateFlow<ExpensesUiState> =
        combine(month, clock.todayFlow) { m, today -> m to today }
            .flatMapLatest { (m, today) ->
                observeMonth(m, today).map { ExpensesUiState(m.year, m.month.ordinal + 1, m < today.yearMonth(), it) }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial())

    fun previousMonth() {
        month.value = month.value.minus(1, DateTimeUnit.MONTH)
    }

    fun nextMonth() {
        if (month.value < clock.today().yearMonth()) month.value = month.value.plus(1, DateTimeUnit.MONTH)
    }

    fun delete(id: String) {
        viewModelScope.launch { deleteExpense(id) }
    }

    private fun initial() = month.value.let { ExpensesUiState(it.year, it.month.ordinal + 1, false, null) }
}

private fun LocalDate.yearMonth() = YearMonth(year, month)
