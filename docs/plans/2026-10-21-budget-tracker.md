# Budget Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** An offline-first budget tracker. Shared KMP logic and ViewModels, with native SwiftUI and Compose UIs: a dashboard, budgets and expenses.

**Architecture:** One `shared` module (`domain` / `data` / `presentation` / `di`) like `../habit-tracker`. The rollover ledger is a pure
function over daily totals and budget versions, recomputed from Flows, never stored. The platforms only render `UiState`.

**Tech Stack:** Kotlin 2.4.20, SKIE 0.10.15, SQLDelight 2.4.1, Koin 4.2.2, kotlinx-datetime 0.8.0, coroutines 1.11.0, Lifecycle 2.11.0,
Turbine 1.2.1, AGP 9.4.1, Compose BOM 2026.09.00, SwiftUI (iOS 17), XcodeGen.

**Spec:** `docs/design/2026-10-21-budget-tracker-design.md`. The § references below point into it.

## Global Constraints

- Base package `app.budgettracker`. Android applicationId `app.budgettracker.android`. iOS bundle id `app.budgettracker.ios`, display name "Budget".
- Copy the build configuration, versions and platform glue from `../habit-tracker` (gradle files, `DriverFactory`, `DayChangeSource`,
  `SystemAppClock`/`FixedAppClock`, `ViewModelHandle`, `KoinHelper` shape, `Hosted`/`ViewModelHost`, `project.yml`). Rename
  `habittracker` → `budgettracker`, DB `BudgetDatabase` / `budget.db`.
- Money is `Long` minor units everywhere. Never use `Double` for money.
- Weeks start on Monday. Dates are `LocalDate`, stored as ISO TEXT. "Now" comes only from `AppClock`.
- `domain/` imports only `kotlin.*`, `kotlinx.coroutines.*`, `kotlinx.datetime.*`, `app.budgettracker.domain.*`.
- Tests: `./gradlew :shared:testAndroidHostTest` and `./gradlew :shared:iosSimulatorArm64Test`.
- iOS: iPhone 17 Pro simulator on the iOS 27.0 runtime. Android SDK at `~/Library/Android/sdk`.
- Commit messages end with `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>`.

## Review Focus

1. **Editing or deleting a past expense** changes the carry for every later period immediately (the ledger is recomputed). Test in Task 4 and Task 7.
2. **Budget changed mid-month** applies to the current month and leaves earlier months unchanged. Test in Task 3.
3. **App open past midnight:** yesterday's leftover moves into "Rolled over" without a relaunch. Test in Task 7 (HomeViewModel with a FakeAppClock flow).
4. **Month clamping:** Mar 31 vs Feb compares Feb 1–28/29. Test in Task 5.
5. **Amount entry like "0", "1.234", "abc", ""** is refused inline with no crash. Test in Task 2 (parser) and Task 7 (form VM).

---

## File structure

```
shared/src/commonMain/kotlin/app/budgettracker/
  domain/model/     Money.kt (MoneyParser, MoneyFormatter, Currency), Expense.kt, Category.kt, Period.kt (Period, PeriodRange),
                    Budget.kt (BudgetVersion), Outcome.kt (Outcome, DomainError)
  domain/logic/     Periods.kt, BudgetResolver.kt, Ledger.kt (PeriodStatus), Comparisons.kt
  domain/repository ExpenseRepository.kt, BudgetRepository.kt, PreferencesRepository.kt
  domain/time/      AppClock.kt
  domain/usecase/   SaveExpense.kt, DeleteExpense.kt, SetBudget.kt, ObserveDashboard.kt, ObserveMonthExpenses.kt, ObserveBudgets.kt
  data/…            DriverFactory, Sql*Repository, time/
  presentation/     home/, expenses/, budgets/, expenseform/ (ViewModel + Contract each)
  di/               Modules.kt, InitKoin.kt
shared/src/commonMain/sqldelight/app/budgettracker/data/db/  Expense.sq, BudgetVersion.sq, Preference.sq
androidApp/src/main/kotlin/app/budgettracker/android/  BudgetApp.kt, MainActivity.kt, ui/{theme,home,expenses,budgets,form}/
iosApp/  project.yml, Budget/{App,Bridge,Theme,Screens}/, BudgetUITests/
```

---

### Task 1: Scaffold

**Files:** root gradle files, `gradle/` (wrapper + toml copied), `.gitignore`, `shared/build.gradle.kts`, `androidApp` (manifest, MainActivity showing "Budget"), `ArchitectureTest.kt`, `SmokeTest.kt`.

- [ ] Copy and rename the build from habit-tracker. Write `SmokeTest` (`assertEquals(2, 1 + 1)`) and `ArchitectureTest` (scan `domain/` sources for forbidden imports).
- [ ] Run `./gradlew :shared:testAndroidHostTest :androidApp:assembleDebug`. Expected: BUILD SUCCESSFUL.
- [ ] Commit `chore: scaffold KMP project`.

### Task 2: Money

**Produces:** `object MoneyParser { fun parse(input: String): Long? }` and `object MoneyFormatter { fun format(minor: Long, currency: Currency): String; fun plain(minor: Long): String }` (`plain` = "1234.50", used to prefill the edit form). `enum class Currency(val code: String, val symbol: String) { PHP("₱"), USD("$"), EUR("€"), GBP("£"), SGD("S$"), AUD("A$"), CAD("C$") }` with `fromCode(String?) = PHP fallback`.

- [ ] Tests (`MoneyTest`): `parse("1234")==123400`, `parse("1,234.5")==123450`, `parse(" 12.05 ")==1205`, null for `""`, `"0"`, `"0.00"`, `"1.234"`, `"abc"`, `"-5"`, `"1000000000"`; `parse("999999999.99")==99999999999`. `format(123450, PHP)=="₱1,234.50"`, `format(-8000, PHP)=="-₱80.00"`, `format(5, USD)=="$0.05"`.
- [ ] Implement, run tests, commit `feat: money parsing and formatting`.

### Task 3: Periods and budget resolution

**Produces:** `enum class Period { DAILY, WEEKLY, MONTHLY }`. `data class PeriodRange(val start: LocalDate, val end: LocalDate)` (inclusive). In `Periods.kt`: `fun Period.rangeOf(date: LocalDate): PeriodRange`, `fun PeriodRange.next(period: Period): PeriodRange`, `fun PeriodRange.daysLeft(today: LocalDate): Int` (today counts). `data class BudgetVersion(val period: Period, val amount: Long?, val effectiveFrom: LocalDate)`. `fun budgetFor(versions: List<BudgetVersion>, range: PeriodRange, today: LocalDate): Long?` (§3.3: the latest version with `effectiveFrom ≤ min(range.end, today)`).

- [ ] Tests: week of Wed 2026-10-21 = Mon 10-19..Sun 10-25; week across a year end; month of 2028-02-10 = 02-01..02-29. `budgetFor`: a version set mid-month applies to the current month; a version effective after the end of a past month does not change that month; a `null` latest version → null; no version yet → null.
- [ ] Implement, run, commit `feat: period ranges and budget resolution`.

### Task 4: Ledger

**Produces:** `data class PeriodStatus(val period: Period, val range: PeriodRange, val budget: Long, val carry: Long, val spent: Long) { val available = budget + carry; val left = available - spent }`. `fun computeStatus(period: Period, versions: List<BudgetVersion>, dailyTotals: Map<LocalDate, Long>, today: LocalDate): PeriodStatus?` (null when the current period has no budget). The algorithm is in §3.4: walk from `period.rangeOf(firstVersion.effectiveFrom)` up to the current range, skipping periods without a budget.

- [ ] Tests (`LedgerTest`): the worked example in §3.4 (Thu: budget 50000, carry −15000, available 35000); zero spend on a past day adds its whole budget; a removed-then-re-added budget keeps the carry from before the gap; weekly carry across a month boundary; monthly: Sep budget 1,000,000 with 900,000 spent → Oct carry +100,000; changing an expense total for a past day changes the current carry.
- [ ] Implement, run, commit `feat: rollover ledger`.

### Task 5: Comparisons

**Produces:** `data class Comparison(val current: Long, val previous: Long) { val deltaPercent: Int? }` (null when previous == 0, rounded half-up). `fun compareDay`, `compareWeekToDate`, `compareMonthToDate(today, totals: Map<LocalDate, Long>): Comparison`. `fun categoryMonthToDate(today, expenses: List<Expense>): List<CategoryComparison(category, current, previous)>` sorted by current desc, omitting rows where both are 0. `fun lastDays(n: Int, today, totals, dailyVersions): List<DayBar(date, spent, budget: Long?)>`.

- [ ] Tests: 2026-03-31 month-to-date vs Feb 1–28; Wednesday week-to-date vs last Mon–Wed; previous 0 → `deltaPercent == null`; 120 vs 100 → 20; categories sorted.
- [ ] Implement, run, commit `feat: spending comparisons`.

### Task 6: Database and repositories

**Produces:** `interface ExpenseRepository { fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<Map<LocalDate, Long>>; fun observeBetween(from, to): Flow<List<Expense>>; fun observeRecent(limit: Int): Flow<List<Expense>>; suspend fun get(id: String): Expense?; suspend fun upsert(e: Expense); suspend fun delete(id: String) }`. `interface BudgetRepository { fun observeVersions(): Flow<List<BudgetVersion>>; suspend fun put(v: BudgetVersion) }` (`INSERT OR REPLACE` on (period, effective_from)). `interface PreferencesRepository { fun observeCurrency(): Flow<Currency>; suspend fun setCurrency(c: Currency) }`. Schema in §5.

- [ ] Host tests on `JdbcSqliteDriver.IN_MEMORY`: daily totals group by date; a same-day `put` replaces the earlier version; an unknown category key reads as OTHER; currency defaults to PHP.
- [ ] Implement `.sq` files + `Sql*Repository` + DriverFactory (android/ios), run, commit `feat: SQLDelight persistence`.

### Task 7: Use cases, ViewModels, DI

**Produces (contracts):**
- `HomeViewModel.state: StateFlow<HomeUiState>`: `HomeUiState(loading, currency, today: PeriodStatus?, week: PeriodStatus?, month: PeriodStatus?, compareDay/Week/Month: Comparison, bars: List<DayBar>, recent: List<Expense>)`.
- `ExpensesViewModel`: `state(month: YearMonth-like (year, month), total, previousTotal, categories: List<CategoryComparison>, days: List<DayGroup(date, total, items)>)`. Events: `prevMonth()`, `nextMonth()` (blocked after the current month), `delete(id)`.
- `BudgetsViewModel`: `rows: List<BudgetRow(period, amount: Long?, carry: Long?)>`, `currency`. Events: `save(period, input: String): Boolean` (false + `error` on invalid), `remove(period)`, `setCurrency(code)`.
- `ExpenseFormViewModel(id: String?)`: fields `amountText, category, date, note, error: DomainError?, saved: Boolean, isEdit`. Events: `setAmount/Category/Date/Note`, `save()`, `delete()`.
- Use cases: `SaveExpense` (validates via MoneyParser, note ≤ 80, date ≤ today → `Outcome`), `DeleteExpense`, `SetBudget(period, amount: Long?)` (writes `effectiveFrom = today`).
- `ObserveDashboard` combines `todayFlow`, versions, totals (from earliest version − 1 month, or today − 62 days, through today), recent and currency.
- DI: `sharedModule`, `platformModule`, `initKoin(fixedToday)`. iOS `KoinHelper` handles for each VM.

- [ ] VM tests with fakes and Turbine: Home updates after an expense is added; **the day advances via FakeAppClock → yesterday's leftover shows as carry**; editing a past expense changes the carry; the form rejects "0" and "1.234" with InvalidAmount and refuses a future date; Budgets `save("abc")` returns false.
- [ ] Koin graph test (all VMs resolve). Run both test tasks, commit `feat: use cases, view models and DI`.

### Task 8: iOS app

**Files:** `iosApp/project.yml`, `Budget/App/{BudgetApp,RootTabView}.swift`, `Bridge/{Hosted,ViewModelHost}.swift` (copied), `Theme/Theme.swift`, `Screens/{Home,Expenses,Budgets,ExpenseForm,Common}/…`.

- [ ] Build the screens as in §4: a hero card with "Left to spend" plus separate Budget / Rolled over / Spent rows, period cards, the compare card, 7-day bars, recent list, Add button; Expenses with a month switcher, categories and day groups with swipe-to-delete; Budgets rows with an editor sheet and currency picker; an expense form sheet with a decimal pad. Use accessibility identifiers `addExpenseButton`, `amountField`, `saveButton`, `leftToday`, `budgetRow_DAILY`, `budgetAmountField`.
- [ ] `BudgetUITests/SmokeUITests`: reset the DB, set a daily budget of 500, add an expense of 120, then `leftToday` contains "380.00".
- [ ] `xcodegen && xcodebuild test` on iPhone 17 Pro (iOS 27.0). Commit `feat(ios): SwiftUI app`.

### Task 9: Android app

**Files:** `BudgetApp.kt` (Koin start), `MainActivity.kt`, `ui/theme`, `ui/home`, `ui/expenses`, `ui/budgets`, `ui/form`, `ui/Format.kt`; androidTest `SmokeTest`.

- [ ] Same screens with Material 3, bottom navigation, the form as a ModalBottomSheet / full-screen dialog, test tags matching the iOS identifiers.
- [ ] A Compose UI smoke test with the same flow as iOS. Run it on an emulator, commit `feat(android): Compose app`.

### Task 10: README and screenshots

- [ ] README: what it does, the rollover rule with the worked example, how to build and test each platform. Screenshots from both platforms with sample data. Commit `docs: README`.
