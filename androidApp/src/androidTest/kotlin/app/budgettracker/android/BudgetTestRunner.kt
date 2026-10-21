package app.budgettracker.android

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner
import kotlinx.datetime.LocalDate

/** Instrumented tests run against [TestBudgetApp], whose "today" is fixed. */
class BudgetTestRunner : AndroidJUnitRunner() {
    override fun newApplication(cl: ClassLoader?, className: String?, context: Context?): Application =
        super.newApplication(cl, TestBudgetApp::class.java.name, context)
}

class TestBudgetApp : BudgetApp() {
    override val fixedToday: LocalDate = LocalDate(2026, 10, 21)
}
