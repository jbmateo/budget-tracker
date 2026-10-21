package app.budgettracker.di

import app.budgettracker.data.time.FixedAppClock
import app.budgettracker.domain.time.AppClock
import kotlinx.datetime.LocalDate
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

/** Starts the dependency graph. [fixedToday] pins "today" for UI tests (debug/test builds only). */
fun initKoin(fixedToday: LocalDate? = null, appDeclaration: KoinAppDeclaration = {}): KoinApplication =
    startKoin {
        appDeclaration()
        allowOverride(true)
        modules(listOfNotNull(platformModule, sharedModule, fixedToday?.let(::fixedClockModule)))
    }

/** Overrides [AppClock] with one pinned to [date]. */
fun fixedClockModule(date: LocalDate): Module = module {
    single<AppClock> { FixedAppClock(date) }
}
