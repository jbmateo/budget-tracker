package app.budgettracker.android

import android.app.Application
import app.budgettracker.di.initKoin
import kotlinx.datetime.LocalDate
import org.koin.android.ext.koin.androidContext

open class BudgetApp : Application() {
    /** Pins "today"; only the instrumented-test application overrides this. */
    protected open val fixedToday: LocalDate? = null

    override fun onCreate() {
        super.onCreate()
        initKoin(fixedToday) { androidContext(this@BudgetApp) }
    }
}
