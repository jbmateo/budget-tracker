package app.budgettracker.android

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Rule
import org.junit.Test

/** Runs on a fresh DB (the orchestrator clears app data before each test) with "today" = 2026-10-21. */
class SmokeTest {
    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun waitForTag(tag: String) =
        rule.waitUntil(5_000) { rule.onAllNodesWithTag(tag).fetchSemanticsNodes().isNotEmpty() }

    @Test
    fun setDailyBudgetThenAddExpenseUpdatesLeftToday() {
        rule.onNodeWithTag("tab_budgets").performClick()
        waitForTag("budgetRow_DAILY")
        rule.onNodeWithTag("budgetRow_DAILY").performClick()
        rule.onNodeWithTag("budgetAmountField").performTextInput("500")
        rule.onNodeWithTag("saveBudgetButton").performClick()

        rule.onNodeWithTag("tab_home").performClick()
        waitForTag("leftToday")
        rule.onNodeWithTag("leftToday").assertTextEquals("₱500.00")

        rule.onNodeWithTag("addExpenseButton").performClick()
        waitForTag("amountField")
        rule.onNodeWithTag("amountField").performTextInput("120")
        rule.onNodeWithTag("saveButton").performClick()

        rule.waitUntil(5_000) { rule.onAllNodesWithText("₱380.00").fetchSemanticsNodes().isNotEmpty() }
        rule.onNodeWithTag("leftToday").assertTextEquals("₱380.00")
    }

    @Test
    fun invalidAmountShowsErrorAndStaysOnForm() {
        waitForTag("addExpenseButton")
        rule.onNodeWithTag("addExpenseButton").performClick()
        waitForTag("amountField")
        rule.onNodeWithTag("amountField").performTextInput("0")
        rule.onNodeWithTag("saveButton").performClick()
        rule.waitUntil(5_000) {
            rule.onAllNodesWithText("Enter an amount above zero, with at most 2 decimals.").fetchSemanticsNodes().isNotEmpty()
        }
        rule.onNodeWithTag("amountField").assertExists()
    }
}
