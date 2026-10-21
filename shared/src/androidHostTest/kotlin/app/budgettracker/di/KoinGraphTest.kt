package app.budgettracker.di

import app.budgettracker.data.db.BudgetDatabase
import app.budgettracker.data.inMemoryDb
import app.budgettracker.data.time.DayChangeSource
import app.budgettracker.domain.time.AppClock
import app.budgettracker.presentation.budgets.BudgetsViewModel
import app.budgettracker.presentation.expenseform.ExpenseFormViewModel
import app.budgettracker.presentation.expenses.ExpensesViewModel
import app.budgettracker.presentation.home.HomeViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.datetime.LocalDate
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class KoinGraphTest {
    private val testPlatformModule = module {
        single<BudgetDatabase> { inMemoryDb() }
        single<DayChangeSource> { object : DayChangeSource { override val changes: Flow<Unit> = emptyFlow() } }
        single<CoroutineDispatcher>(IoQualifier) { Dispatchers.Unconfined }
    }

    private fun koin(vararg extra: org.koin.core.module.Module) =
        koinApplication { allowOverride(true); modules(sharedModule, testPlatformModule, *extra) }.koin

    @Test
    fun everyViewModelResolves() {
        val koin = koin()
        assertNotNull(koin.get<HomeViewModel>())
        assertNotNull(koin.get<ExpensesViewModel>())
        assertNotNull(koin.get<BudgetsViewModel>())
        assertNotNull(koin.get<ExpenseFormViewModel> { parametersOf(*arrayOf<Any?>(null)) })
        assertNotNull(koin.get<ExpenseFormViewModel> { parametersOf("id") })
    }

    @Test
    fun fixedClockOverridesToday() {
        assertEquals(LocalDate(2026, 10, 14), koin(fixedClockModule(LocalDate(2026, 10, 14))).get<AppClock>().today())
    }
}
