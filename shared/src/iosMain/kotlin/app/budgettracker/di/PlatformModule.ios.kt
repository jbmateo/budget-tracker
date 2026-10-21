package app.budgettracker.di

import app.budgettracker.data.db.DriverFactory
import app.budgettracker.data.time.DayChangeSource
import app.budgettracker.data.time.IosDayChangeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import org.koin.dsl.module

actual val platformModule = module {
    single { DriverFactory() }
    single<DayChangeSource> { IosDayChangeSource() }
    single<CoroutineDispatcher>(IoQualifier) { Dispatchers.IO }
}
