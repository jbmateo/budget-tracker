package app.budgettracker.di

import app.budgettracker.data.db.BudgetDatabase
import app.budgettracker.data.db.DriverFactory
import app.budgettracker.data.repository.SqlBudgetRepository
import app.budgettracker.data.repository.SqlExpenseRepository
import app.budgettracker.data.repository.SqlPreferencesRepository
import app.budgettracker.data.time.SystemAppClock
import app.budgettracker.domain.repository.BudgetRepository
import app.budgettracker.domain.repository.ExpenseRepository
import app.budgettracker.domain.repository.PreferencesRepository
import app.budgettracker.domain.time.AppClock
import app.budgettracker.domain.usecase.DeleteExpense
import app.budgettracker.domain.usecase.GetExpense
import app.budgettracker.domain.usecase.ObserveDashboard
import app.budgettracker.domain.usecase.ObserveMonthExpenses
import app.budgettracker.domain.usecase.SaveExpense
import app.budgettracker.domain.usecase.SetBudget
import app.budgettracker.domain.usecase.SetCurrency
import app.budgettracker.presentation.budgets.BudgetsViewModel
import app.budgettracker.presentation.expenseform.ExpenseFormViewModel
import app.budgettracker.presentation.expenses.ExpensesViewModel
import app.budgettracker.presentation.home.HomeViewModel
import kotlinx.datetime.TimeZone
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Dispatcher qualifier for blocking database work. */
val IoQualifier = named("io")

/** Provides DriverFactory, DayChangeSource and the `named("io")` CoroutineDispatcher. */
expect val platformModule: Module

@OptIn(ExperimentalUuidApi::class)
val sharedModule: Module = module {
    single<BudgetDatabase> { BudgetDatabase(get<DriverFactory>().create()) }
    single<ExpenseRepository> { SqlExpenseRepository(get(), get(IoQualifier)) }
    single<BudgetRepository> { SqlBudgetRepository(get(), get(IoQualifier)) }
    single<PreferencesRepository> { SqlPreferencesRepository(get(), get(IoQualifier)) }
    single<AppClock> { SystemAppClock(Clock.System, { TimeZone.currentSystemDefault() }, get()) }

    factory { GetExpense(get()) }
    factory { SaveExpense(get(), get(), newId = { Uuid.random().toString() }, now = { Clock.System.now() }) }
    factory { DeleteExpense(get()) }
    factory { SetBudget(get(), get()) }
    factory { SetCurrency(get()) }
    factory { ObserveDashboard(get(), get(), get(), get()) }
    factory { ObserveMonthExpenses(get(), get()) }

    viewModelOf(::HomeViewModel)
    viewModelOf(::ExpensesViewModel)
    viewModelOf(::BudgetsViewModel)
    viewModel { (id: String?) -> ExpenseFormViewModel(id, get(), get(), get(), get()) }
}
