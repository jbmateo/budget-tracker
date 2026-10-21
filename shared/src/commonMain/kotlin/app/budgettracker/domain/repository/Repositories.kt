package app.budgettracker.domain.repository

import app.budgettracker.domain.model.BudgetVersion
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface ExpenseRepository {
    /** Sum of expenses per date in [from]..[to] (inclusive); dates without expenses are absent. */
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>>
    /** Newest first. */
    fun observeBetween(from: LocalDate, to: LocalDate): Flow<List<Expense>>
    fun observeRecent(limit: Int): Flow<List<Expense>>
    suspend fun get(id: String): Expense?
    suspend fun upsert(expense: Expense)
    suspend fun delete(id: String)
}

interface BudgetRepository {
    fun observeVersions(): Flow<List<BudgetVersion>>
    /** Replaces an existing version for the same period and date. */
    suspend fun put(version: BudgetVersion)
}

interface PreferencesRepository {
    fun observeCurrency(): Flow<Currency>
    suspend fun setCurrency(currency: Currency)
}
