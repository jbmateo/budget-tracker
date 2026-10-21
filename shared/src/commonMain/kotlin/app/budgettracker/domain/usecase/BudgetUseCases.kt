package app.budgettracker.domain.usecase

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.repository.BudgetRepository
import app.budgettracker.domain.repository.PreferencesRepository
import app.budgettracker.domain.time.AppClock

/** Sets ([amount] > 0) or removes ([amount] null) the [period] budget from today on (spec §3.3). */
class SetBudget(private val repo: BudgetRepository, private val clock: AppClock) {
    suspend operator fun invoke(period: Period, amount: Long?) {
        require(amount == null || amount > 0) { "Budget must be positive" }
        repo.put(BudgetVersion(period, amount, clock.today()))
    }
}

class SetCurrency(private val prefs: PreferencesRepository) {
    suspend operator fun invoke(currency: Currency) = prefs.setCurrency(currency)
}
