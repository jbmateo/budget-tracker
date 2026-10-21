package app.budgettracker.presentation

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.ExpenseCategory
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.DomainError
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.Period
import app.budgettracker.domain.usecase.DeleteExpense
import app.budgettracker.domain.usecase.GetExpense
import app.budgettracker.domain.usecase.ObserveDashboard
import app.budgettracker.domain.usecase.ObserveMonthExpenses
import app.budgettracker.domain.usecase.SaveExpense
import app.budgettracker.domain.usecase.SetBudget
import app.budgettracker.domain.usecase.SetCurrency
import app.budgettracker.fakes.FakeAppClock
import app.budgettracker.fakes.FakeBudgetRepository
import app.budgettracker.fakes.FakeExpenseRepository
import app.budgettracker.fakes.FakePreferencesRepository
import app.budgettracker.presentation.budgets.BudgetsViewModel
import app.budgettracker.presentation.expenseform.ExpenseFormViewModel
import app.budgettracker.presentation.expenses.ExpensesViewModel
import app.budgettracker.presentation.home.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    private fun d(s: String) = LocalDate.parse(s)
    private val expenses = FakeExpenseRepository()
    private val budgets = FakeBudgetRepository()
    private val prefs = FakePreferencesRepository()
    private val clock = FakeAppClock(d("2026-10-21"))
    private var ids = 0
    private val save = SaveExpense(expenses, clock, newId = { "e${++ids}" }, now = { Instant.fromEpochMilliseconds(ids.toLong()) })
    private val dashboard = ObserveDashboard(expenses, budgets, prefs, clock)

    @BeforeTest fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @AfterTest fun tearDown() { Dispatchers.resetMain() }

    /** Keeps a WhileSubscribed state flow active for the test. */
    private fun <T> TestScope.collect(flow: StateFlow<T>) {
        backgroundScope.launch { flow.collect {} }
        runCurrent()
    }

    @Test
    fun homeUpdatesAfterAddingExpense() = runTest {
        budgets.put(BudgetVersion(Period.DAILY, 50_000, d("2026-10-21")))
        val home = HomeViewModel(dashboard)
        collect(home.state)
        assertEquals(50_000, home.state.value.dashboard!!.daily!!.left)
        save(null, "120", ExpenseCategory.FOOD, "", d("2026-10-21"))
        advanceUntilIdle()
        assertEquals(38_000, home.state.value.dashboard!!.daily!!.left)
        assertEquals(1, home.state.value.dashboard!!.recent.size)
    }

    @Test
    fun dayChangeWhileOpenRollsLeftoverIntoCarry() = runTest {
        budgets.put(BudgetVersion(Period.DAILY, 50_000, d("2026-10-21")))
        save(null, "300", ExpenseCategory.FOOD, "", d("2026-10-21"))
        val home = HomeViewModel(dashboard)
        collect(home.state)
        assertEquals(0, home.state.value.dashboard!!.daily!!.carry)
        clock.setToday(d("2026-10-22"))
        advanceUntilIdle()
        val daily = home.state.value.dashboard!!.daily!!
        assertEquals(d("2026-10-22"), home.state.value.dashboard!!.today)
        assertEquals(20_000, daily.carry)
        assertEquals(70_000, daily.left)
    }

    @Test
    fun editingPastExpenseChangesCarry() = runTest {
        clock.setToday(d("2026-10-20"))
        budgets.put(BudgetVersion(Period.DAILY, 50_000, d("2026-10-20")))
        save(null, "300", ExpenseCategory.FOOD, "", d("2026-10-20"))
        clock.setToday(d("2026-10-21"))
        val home = HomeViewModel(dashboard)
        collect(home.state)
        assertEquals(20_000, home.state.value.dashboard!!.daily!!.carry)

        val form = ExpenseFormViewModel("e1", GetExpense(expenses), save, DeleteExpense(expenses), clock)
        advanceUntilIdle()
        assertEquals("300.00", form.state.value.amountText)
        form.setAmount("600")
        form.save()
        advanceUntilIdle()
        assertTrue(form.state.value.done)
        assertEquals(-10_000, home.state.value.dashboard!!.daily!!.carry)
    }

    @Test
    fun formRejectsBadAmountsAndFutureDates() = runTest {
        val form = ExpenseFormViewModel(null, GetExpense(expenses), save, DeleteExpense(expenses), clock)
        for (bad in listOf("", "0", "1.234", "abc")) {
            form.setAmount(bad)
            form.save()
            advanceUntilIdle()
            assertEquals(DomainError.InvalidAmount, form.state.value.error, "for '$bad'")
            assertFalse(form.state.value.done)
        }
        form.setAmount("10")
        form.setDate(2026, 10, 22)
        form.save()
        advanceUntilIdle()
        assertEquals(DomainError.FutureDate, form.state.value.error)
        form.setNote("x".repeat(81))
        form.setDate(2026, 10, 21)
        form.save()
        advanceUntilIdle()
        assertEquals(DomainError.NoteTooLong, form.state.value.error)
        assertTrue(expenses.items.value.isEmpty())
    }

    @Test
    fun formSavesTrimmedNoteAndDeletes() = runTest {
        val form = ExpenseFormViewModel(null, GetExpense(expenses), save, DeleteExpense(expenses), clock)
        form.setAmount("1,250.5")
        form.setCategory(ExpenseCategory.TRANSPORT)
        form.setNote("  taxi  ")
        form.save()
        advanceUntilIdle()
        val saved = expenses.items.value.values.single()
        assertEquals(125_050, saved.amount)
        assertEquals("taxi", saved.note)
        assertEquals(ExpenseCategory.TRANSPORT, saved.category)

        val edit = ExpenseFormViewModel(saved.id, GetExpense(expenses), save, DeleteExpense(expenses), clock)
        advanceUntilIdle()
        edit.delete()
        advanceUntilIdle()
        assertTrue(edit.state.value.done)
        assertTrue(expenses.items.value.isEmpty())
    }

    @Test
    fun budgetsSaveValidatesAndRemoves() = runTest {
        val vm = BudgetsViewModel(dashboard, SetBudget(budgets, clock), SetCurrency(prefs))
        collect(vm.state)
        assertFalse(vm.save(Period.WEEKLY, "abc"))
        assertEquals(Period.WEEKLY, vm.state.value.invalidPeriod)
        assertTrue(vm.save(Period.WEEKLY, "3500"))
        advanceUntilIdle()
        assertNull(vm.state.value.invalidPeriod)
        assertEquals(350_000, vm.state.value.rows.single { it.period == Period.WEEKLY }.amount)
        vm.remove(Period.WEEKLY)
        advanceUntilIdle()
        assertNull(vm.state.value.rows.single { it.period == Period.WEEKLY }.amount)
        vm.selectCurrency(Currency.USD)
        advanceUntilIdle()
        assertEquals(Currency.USD, vm.state.value.currency)
    }

    private fun e(id: String, amount: Long, date: String) =
        Expense(id, amount, ExpenseCategory.FOOD, "", d(date), Instant.fromEpochMilliseconds(0))

    @Test
    fun expensesMonthNavigationAndGrouping() = runTest {
        expenses.upsert(e("a", 100, "2026-10-21"))
        expenses.upsert(e("b", 50, "2026-10-21"))
        expenses.upsert(e("c", 70, "2026-10-02"))
        expenses.upsert(e("d", 40, "2026-09-15"))
        val vm = ExpensesViewModel(ObserveMonthExpenses(expenses, prefs), DeleteExpense(expenses), clock)
        collect(vm.state)
        val oct = vm.state.value
        assertEquals(10, oct.monthNumber)
        assertFalse(oct.canGoNext)
        assertEquals(220, oct.data!!.total)
        assertEquals(40, oct.data!!.previousTotal)
        assertEquals(listOf(d("2026-10-21"), d("2026-10-02")), oct.data!!.days.map { it.date })
        assertEquals(150, oct.data!!.days.first().total)

        vm.nextMonth() // blocked on the current month
        assertEquals(10, vm.state.value.monthNumber)
        vm.previousMonth()
        advanceUntilIdle()
        assertEquals(9, vm.state.value.monthNumber)
        assertTrue(vm.state.value.canGoNext)
        assertEquals(40, vm.state.value.data!!.total)

        vm.nextMonth()
        vm.delete("a")
        advanceUntilIdle()
        assertEquals(120, vm.state.value.data!!.total)
    }
}
