import XCTest

/// Each test launches on a fresh database with "today" pinned to Wed 2026-10-21.
final class SmokeUITests: XCTestCase {
    override func setUp() {
        continueAfterFailure = false
    }

    @MainActor
    private func launchFresh() -> XCUIApplication {
        let app = XCUIApplication()
        app.launchArguments = ["-uiTestResetDB", "-uiTestToday", "2026-10-21"]
        app.launch()
        return app
    }

    @MainActor
    func testSetDailyBudgetThenAddExpenseUpdatesLeftToday() {
        let app = launchFresh()

        app.tabBars.buttons["Budgets"].tap()
        let daily = app.buttons["budgetRow_DAILY"]
        XCTAssertTrue(daily.waitForExistence(timeout: 10))
        daily.tap()
        let budgetField = app.textFields["budgetAmountField"]
        XCTAssertTrue(budgetField.waitForExistence(timeout: 5))
        budgetField.typeText("500")
        app.buttons["saveBudgetButton"].tap()
        XCTAssertTrue(budgetField.waitForNonExistence(timeout: 5))

        app.tabBars.buttons["Home"].tap()
        let left = app.staticTexts["leftToday"]
        XCTAssertTrue(left.waitForExistence(timeout: 10))
        XCTAssertEqual(left.label, "₱500.00")

        app.buttons["addExpenseButton"].tap()
        let amount = app.textFields["amountField"]
        XCTAssertTrue(amount.waitForExistence(timeout: 5))
        amount.typeText("120")
        app.buttons["saveButton"].tap()
        XCTAssertTrue(amount.waitForNonExistence(timeout: 5))

        XCTAssertTrue(app.staticTexts["₱380.00"].waitForExistence(timeout: 5))
        XCTAssertEqual(app.staticTexts["leftToday"].label, "₱380.00")
    }

    @MainActor
    func testInvalidAmountShowsErrorAndKeepsSheetOpen() {
        let app = launchFresh()
        app.buttons["addExpenseButton"].tap()
        let amount = app.textFields["amountField"]
        XCTAssertTrue(amount.waitForExistence(timeout: 5))
        amount.typeText("0")
        app.buttons["saveButton"].tap()
        XCTAssertTrue(app.staticTexts["Enter an amount above zero, with at most 2 decimals."].waitForExistence(timeout: 5))
        XCTAssertTrue(amount.exists)
    }
}
