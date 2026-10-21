# Budget Tracker — Design (MVP)

Date: 2026-10-21. Status: approved (autopilot; the user asked to be consulted only on decisions that cost money).

## 1. Intent

A personal, offline-first budget tracker for daily use on iPhone (Android too). The user wants to:

1. Log expenses quickly.
2. See how current spending compares with earlier spending.
3. Set daily, weekly and monthly budgets.
4. Carry money forward. Unspent money rolls into the next period and keeps adding up. It is shown **separately** from that period's own budget. Overspending is deducted.

MVP scope: a dashboard, setting budgets, and adding/editing expenses. No accounts, no sync, no network, no income tracking, no
recurring expenses, no notifications.

Success: in under 10 seconds the user can add an expense, then see exactly what is left today and where that number came from
(budget, rollover, spent).

## 2. Stack (reuses the habit-tracker setup that already works on this machine)

- Kotlin Multiplatform, one `shared` module: `domain` (pure Kotlin), `data` (SQLDelight), `presentation` (shared
  AndroidX ViewModels exposing `StateFlow<UiState>`), `di` (Koin).
- Native UIs: SwiftUI (iOS 17+, XcodeGen project, SKIE for Flows/sealed types) and Jetpack Compose + Material 3.
- Kotlin 2.4.20, SKIE 0.10.15, SQLDelight 2.4.1, Koin 4.2.2, kotlinx-datetime 0.8.0, coroutines 1.11.0, Lifecycle 2.11.0,
  AGP 9.4.1, Compose BOM 2026.09.00.

Why not Compose Multiplatform for the UI? Most published KMP production reports recommend shared logic and ViewModels with
native UI. That approach is already proven in the habit tracker, and it keeps the iPhone app feeling native. All money math
lives in `shared`, so both UIs only render it.

Offline-first means the local SQLite database is the only source of truth. Nothing needs the network.

## 3. Domain

### 3.1 Money
- Stored as `Long` minor units (centavos/cents). No floating point anywhere.
- Input parsing (`MoneyParser`): accepts `1234`, `1,234.5`, `1234.50`; at most 2 decimals; must be > 0 and ≤ 999,999,999.99.
- Formatting (`MoneyFormatter`, shared): currency symbol + grouped thousands + 2 decimals, e.g. `₱1,234.50`, `-₱80.00`.
- Currency: user preference, chosen from PHP (default), USD, EUR, GBP, SGD, AUD, CAD, all 2-decimal. Changing it only changes
  the symbol; amounts are never converted.

### 3.2 Expense
`Expense(id: String (UUID), amount: Long, category: Category, note: String, date: LocalDate, createdAt: Instant)`.
- `date` ≤ today (past dates allowed, future refused). Note: trimmed, 0–80 chars.
- `Category` is a fixed enum: FOOD, GROCERIES, TRANSPORT, SHOPPING, BILLS, ENTERTAINMENT, HEALTH, OTHER. It is stored as its key,
  and an unknown key resolves to OTHER.

### 3.3 Budgets
- `Period` = DAILY | WEEKLY (Monday–Sunday) | MONTHLY (calendar month).
- Each period type has at most one budget. Its history is kept as versions:
  `BudgetVersion(period, amount: Long?, effectiveFrom: LocalDate)`. A `null` amount means "budget removed".
- Setting, changing or removing a budget writes a version with `effectiveFrom = today`. A second change on the same day replaces
  that day's version.
- **Amount for a period** = the latest version with `effectiveFrom ≤ min(periodEnd, today)`. A change made mid-week or mid-month
  therefore applies to the current period at once, and never rewrites periods that have already ended.

### 3.4 Rollover ledger (core rule)
The ledger is computed per period type, independently, from the expenses and the budget versions. It is never stored, so editing
or deleting a past expense always produces consistent numbers.

The ledger walks every period from the first budgeted period up to, but not including, the current one:

```
carry = 0
for each completed period p with a budget:
    carry += budget(p) - spent(p)      // under → adds up; over → goes negative
periods with no budget are skipped (carry unchanged)
```

For the current period:

```
available = budget + carry
left      = available - spentSoFar
```

- `carry > 0` is shown as **"Rolled over +₱X"**: the total of all earlier unspent money. It appears next to the period's own
  budget, not merged into it.
- `carry < 0` is shown as **"Overspent −₱X"**. Earlier overspending is deducted from the current period.
- Overspending in the current period shows a negative `left` ("₱X over"). When the period closes, that excess lowers the
  carry, which first uses up any savings and then reduces the next period.

This is one signed running balance. The user's wording maps onto it as follows: unspent money from every under-budget day adds
up and is shown separately; excess spending is subtracted from the budget of the day the user is in.

Worked example, daily budget ₱500:

| Day | Budget | Carry in | Available | Spent | Carry out |
|-----|--------|----------|-----------|-------|-----------|
| Mon | 500 | 0 | 500 | 300 | +200 |
| Tue | 500 | +200 | 700 | 450 | +250 |
| Wed | 500 | +250 | 750 | 900 | −150 |
| Thu | 500 | −150 | 350 | … | |

### 3.5 Comparison with earlier spending
Each comparison is a pair of totals for the same span of days:
- Today vs yesterday.
- This week so far (Mon → today) vs the same weekdays last week.
- This month so far (1 → today's day-of-month) vs the same days last month. The day is clamped to the shorter month's length.
- Per category: this month so far vs the same span last month.

Delta = `(current − previous) / previous`. When previous = 0 the comparison shows "new" (no percentage).

The dashboard also shows the last 7 days as bars, each marked over or under that day's budget when a daily budget existed.

## 4. Screens (both platforms)

Three tabs: **Home**, **Expenses**, **Budgets**. Add and edit open a modal sheet.

1. **Home (dashboard)**
   - Hero "Today": big "Left to spend". Below it, three separate lines: Daily budget, Rolled over / Overspent (green/red), Spent
     today. Then a progress bar. With no daily budget: a "Set a daily budget" button.
   - Week and Month cards (only for budgets that are set): spent / available, the carry line, what is left, and days left.
   - Compare card: today/week/month with up/down arrows and %.
   - Last-7-days bars.
   - Recent expenses (5) with "See all". A primary "Add expense" button.
2. **Expenses**: month switcher; the month's total vs last month; category breakdown (this month vs last month); a list grouped
   by day with day totals. Tap to edit, swipe to delete.
3. **Budgets**: Daily / Weekly / Monthly rows showing the amount or "Not set" and the current carry. Tap opens an editor
   (amount, Save, Remove). A currency picker sits at the bottom.
4. **Expense sheet**: amount (decimal keypad, focused on open), category chips, date (≤ today, defaults to today), note.
   Save, plus Delete when editing.

The app follows the date when it is left open past midnight, using the same `DayChangeSource` as the habit tracker.

## 5. Data (SQLDelight)

```sql
expense(id TEXT PK, amount INTEGER NOT NULL CHECK(amount > 0), category TEXT NOT NULL, note TEXT NOT NULL,
        date TEXT NOT NULL, created_at INTEGER NOT NULL)          -- index on date
budget_version(period TEXT NOT NULL, amount INTEGER, effective_from TEXT NOT NULL,
               PRIMARY KEY(period, effective_from))
preference(key TEXT PK, value TEXT NOT NULL)                      -- currency
```

Queries return Flows. The ledger reads daily totals (`SELECT date, SUM(amount) … GROUP BY date`) from the first version date up
to today. That is at most a few thousand rows a decade.

## 6. Errors

Expected failures are returned as `Outcome.Failure(DomainError)` and never thrown: InvalidAmount, NoteTooLong, FutureDate,
NotFound. The forms show them inline. Database errors are unexpected and crash in debug builds.

## 7. Testing

- Unit-test the domain first (TDD): MoneyParser/Formatter, period math (week/month bounds, clamping), the version-resolution
  rule, the ledger (worked example, gaps, removal and re-adding, mid-period change, month boundaries, leap years), comparisons.
- SQLDelight repositories on a JDBC SQLite driver (JVM host tests).
- ViewModels with fakes and Turbine, including the day changing while the app stays open.
- An architecture test: `domain` imports no `data` and no platform code.
- UI smoke tests: on iOS (XCUITest, iPhone 17 Pro simulator on the iOS 27.0 runtime) and Android (Compose UI test). Each one
  sets a daily budget, adds an expense, and checks that the "left" amount updates.
