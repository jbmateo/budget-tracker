package app.budgettracker.android

import android.app.Application
import app.budgettracker.di.initKoin
import kotlinx.datetime.LocalDate
import app.budgettracker.di.seedSampleData
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.koin.androidContext

open class BudgetApp : Application() {
    /** Pins "today"; only the instrumented-test application overrides this. */
    protected open val fixedToday: LocalDate? = null

    override fun onCreate() {
        super.onCreate()
        val koin = initKoin(fixedToday) { androidContext(this@BudgetApp) }.koin
        // Debug only: `adb shell setprop debug.budget.seed 1` before launch fills an empty DB with sample data.
        if (BuildConfig.DEBUG && readProp("debug.budget.seed") == "1") {
            runBlocking { seedSampleData(koin) }
        }
    }

    @Suppress("PrivateApi")
    private fun readProp(name: String): String? = runCatching {
        Class.forName("android.os.SystemProperties").getMethod("get", String::class.java).invoke(null, name) as String
    }.getOrNull()
}
