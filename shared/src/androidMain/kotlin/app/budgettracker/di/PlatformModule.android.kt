package app.budgettracker.di

import app.budgettracker.data.db.DriverFactory
import app.budgettracker.data.time.AndroidDayChangeSource
import app.budgettracker.data.time.DayChangeSource
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

actual val platformModule = module {
    single { DriverFactory(androidContext()) }
    single<DayChangeSource> { AndroidDayChangeSource(androidContext()) }
    single<CoroutineDispatcher>(IoQualifier) { Dispatchers.IO }
}
