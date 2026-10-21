package app.budgettracker.di

import app.budgettracker.data.db.deleteBudgetDatabase
import app.budgettracker.presentation.budgets.BudgetsViewModel
import app.budgettracker.presentation.expenseform.ExpenseFormViewModel
import app.budgettracker.presentation.expenses.ExpensesViewModel
import app.budgettracker.presentation.home.HomeViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf
import org.koin.mp.KoinPlatformTools

/** Entry point for the iOS app. */
object KoinHelper {
    /**
     * Starts Koin once; safe to call repeatedly. [resetDatabase] deletes the database first (UI tests).
     * [fixedToday] (ISO date, UI tests only) pins "today".
     */
    fun start(resetDatabase: Boolean = false, fixedToday: String? = null) {
        if (KoinPlatformTools.defaultContext().getOrNull() != null) return
        if (resetDatabase) deleteBudgetDatabase()
        initKoin(fixedToday = fixedToday?.let(LocalDate::parse))
    }

    private val koin get() = KoinPlatformTools.defaultContext().get()

    fun home(): ViewModelHandle<HomeViewModel> = handle { koin.get<HomeViewModel>() }
    fun expenses(): ViewModelHandle<ExpensesViewModel> = handle { koin.get<ExpensesViewModel>() }
    fun budgets(): ViewModelHandle<BudgetsViewModel> = handle { koin.get<BudgetsViewModel>() }
    fun expenseForm(id: String?): ViewModelHandle<ExpenseFormViewModel> =
        handle { koin.get<ExpenseFormViewModel> { parametersOf(*arrayOf<Any?>(id)) } }
}
