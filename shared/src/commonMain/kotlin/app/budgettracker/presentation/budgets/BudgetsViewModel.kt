package app.budgettracker.presentation.budgets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.MoneyParser
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.usecase.ObserveDashboard
import app.budgettracker.domain.usecase.SetBudget
import app.budgettracker.domain.usecase.SetCurrency
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** One budget. [amount] and [carry] are null when the budget is not set. */
data class BudgetRow(val period: Period, val amount: Long?, val carry: Long?)

/** [invalidPeriod]: the editor whose last save had an invalid amount. */
data class BudgetsUiState(
    val isLoading: Boolean,
    val currency: Currency,
    val rows: List<BudgetRow>,
    val invalidPeriod: Period?,
    val currencies: List<Currency> = Currency.entries,
)

class BudgetsViewModel(
    observeDashboard: ObserveDashboard,
    private val setBudget: SetBudget,
    private val setCurrency: SetCurrency,
) : ViewModel() {
    private val invalid = MutableStateFlow<Period?>(null)

    val state: StateFlow<BudgetsUiState> = combine(observeDashboard(), invalid) { d, inv ->
        val statuses = listOf(d.daily, d.weekly, d.monthly)
        BudgetsUiState(
            isLoading = false,
            currency = d.currency,
            rows = Period.entries.zip(statuses) { p, s -> BudgetRow(p, s?.budget, s?.carry) },
            invalidPeriod = inv,
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        BudgetsUiState(true, Currency.PHP, Period.entries.map { BudgetRow(it, null, null) }, null),
    )

    /** Returns false (and flags [period] invalid) when [input] is not a valid amount. */
    fun save(period: Period, input: String): Boolean {
        val amount = MoneyParser.parse(input)
        if (amount == null) {
            invalid.value = period
            return false
        }
        invalid.value = null
        viewModelScope.launch { setBudget(period, amount) }
        return true
    }

    fun clearError() {
        invalid.value = null
    }

    fun remove(period: Period) {
        viewModelScope.launch { setBudget(period, null) }
    }

    fun selectCurrency(currency: Currency) {
        viewModelScope.launch { setCurrency(currency) }
    }
}
