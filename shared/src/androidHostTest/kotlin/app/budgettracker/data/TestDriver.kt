package app.budgettracker.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import app.budgettracker.data.db.BudgetDatabase
import java.util.Properties

fun inMemoryDb(): BudgetDatabase {
    val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY, Properties().apply { put("foreign_keys", "true") })
    BudgetDatabase.Schema.create(driver)
    return BudgetDatabase(driver)
}
