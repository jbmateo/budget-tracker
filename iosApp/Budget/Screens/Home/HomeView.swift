import SwiftUI
import Charts
import Shared

struct HomeView: View {
    let onAdd: () -> Void
    let onOpen: (String) -> Void
    let onSeeAll: () -> Void
    let onSetBudget: () -> Void

    var body: some View {
        Hosted(make: { KoinHelper.shared.home() }, state: { $0.state }) { host in
            HomeContent(host: host, onAdd: onAdd, onOpen: onOpen, onSeeAll: onSeeAll, onSetBudget: onSetBudget)
        }
    }
}

private struct HomeContent: View {
    let host: ViewModelHost<HomeViewModel, HomeUiState>
    let onAdd: () -> Void
    let onOpen: (String) -> Void
    let onSeeAll: () -> Void
    let onSetBudget: () -> Void

    var body: some View {
        ZStack(alignment: .bottom) {
            ScrollView {
                if let d = host.state.dashboard {
                    VStack(alignment: .leading, spacing: 16) {
                        Text(Fmt.longDay(d.today))
                            .font(.subheadline.weight(.medium))
                            .foregroundStyle(.secondary)
                            .padding(.horizontal, 4)
                        TodayHero(dashboard: d, onSetBudget: onSetBudget)
                        if let week = d.weekly { PeriodCard(title: "This week", status: week, currency: d.currency) }
                        if let month = d.monthly { PeriodCard(title: "This month", status: month, currency: d.currency) }
                        CompareCard(dashboard: d)
                        LastDaysCard(dashboard: d)
                        RecentCard(dashboard: d, onOpen: onOpen, onSeeAll: onSeeAll)
                    }
                    .padding(16)
                    .padding(.bottom, 72)
                } else {
                    ProgressView().padding(.top, 80)
                }
            }
            Button(action: onAdd) {
                Label("Add expense", systemImage: "plus")
                    .font(.headline)
                    .padding(.horizontal, 24).padding(.vertical, 14)
                    .foregroundStyle(Theme.heroText)
                    .background(Theme.hero, in: Capsule())
                    .shadow(color: .black.opacity(0.18), radius: 10, y: 4)
            }
            .accessibilityIdentifier("addExpenseButton")
            .padding(.bottom, 12)
        }
        .background(Theme.canvas.ignoresSafeArea())
        .navigationTitle("Budget")
        .task { host.start() }
    }
}

/// "Left to spend today", with the day's budget, rollover and spending listed separately.
private struct TodayHero: View {
    let dashboard: Dashboard
    let onSetBudget: () -> Void

    var body: some View {
        let currency = dashboard.currency
        VStack(alignment: .leading, spacing: 14) {
            if let s = dashboard.daily {
                VStack(alignment: .leading, spacing: 4) {
                    Text(s.left < 0 ? "Over today's budget by" : "Left to spend today")
                        .font(.subheadline.weight(.medium))
                        .foregroundStyle(Theme.heroSubtext)
                    Text(Fmt.money(abs(s.left), currency))
                        .font(.system(size: 44, weight: .bold, design: .rounded).monospacedDigit())
                        .foregroundStyle(s.left < 0 ? Theme.heroNegative : Theme.heroText)
                        .minimumScaleFactor(0.5)
                        .lineLimit(1)
                        .accessibilityIdentifier("leftToday")
                }
                BudgetBar(value: s.spent, total: max(s.available, 0), track: Theme.heroPanel, fill: Theme.heroPositive, over: Theme.heroNegative)
                VStack(spacing: 8) {
                    line("Daily budget", Fmt.money(s.budget, currency), Theme.heroText)
                    if s.carry != 0 {
                        line(
                            s.carry > 0 ? "Rolled over" : "Overspent before",
                            Fmt.signed(s.carry, currency),
                            s.carry > 0 ? Theme.heroPositive : Theme.heroNegative
                        )
                        .accessibilityIdentifier("carryToday")
                    }
                    line("Spent today", Fmt.money(s.spent, currency), Theme.heroText)
                }
                .padding(12)
                .background(Theme.heroPanel, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                if s.left < 0 {
                    Text("The extra \(Fmt.money(-s.left, currency)) comes off tomorrow's budget.")
                        .font(.footnote)
                        .foregroundStyle(Theme.heroSubtext)
                }
            } else {
                Text("Spent today")
                    .font(.subheadline.weight(.medium))
                    .foregroundStyle(Theme.heroSubtext)
                Text(Fmt.money(dashboard.compareDay.current, currency))
                    .font(.system(size: 44, weight: .bold, design: .rounded).monospacedDigit())
                    .foregroundStyle(Theme.heroText)
                Button("Set a daily budget", action: onSetBudget)
                    .buttonStyle(.borderedProminent)
                    .tint(Theme.heroPanel)
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background(Theme.hero, in: RoundedRectangle(cornerRadius: Theme.cornerRadius, style: .continuous))
    }

    private func line(_ label: String, _ value: String, _ color: Color) -> some View {
        HStack {
            Text(label).foregroundStyle(Theme.heroSubtext)
            Spacer()
            Text(value).monospacedDigit().fontWeight(.semibold).foregroundStyle(color)
        }
        .font(.subheadline)
        .accessibilityElement(children: .combine)
    }
}

/// Weekly or monthly budget at a glance.
private struct PeriodCard: View {
    let title: String
    let status: PeriodStatus
    let currency: Currency

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .firstTextBaseline) {
                Text(title).font(.headline)
                Spacer()
                Text(status.daysLeft == 1 ? "Last day" : "\(status.daysLeft) days left")
                    .font(.caption).foregroundStyle(.secondary)
            }
            HStack(alignment: .firstTextBaseline) {
                Text(Fmt.money(abs(status.left), currency))
                    .font(.title2.bold().monospacedDigit())
                    .foregroundStyle(status.left < 0 ? Theme.negative : .primary)
                Text(status.left < 0 ? "over" : "left").foregroundStyle(.secondary)
                Spacer()
            }
            BudgetBar(value: status.spent, total: max(status.available, 0))
            HStack(spacing: 12) {
                Text("Spent \(Fmt.money(status.spent, currency))")
                Text("Budget \(Fmt.money(status.budget, currency))")
                if status.carry != 0 {
                    Text(Fmt.signed(status.carry, currency))
                        .foregroundStyle(status.carry > 0 ? Theme.positive : Theme.negative)
                }
            }
            .font(.caption.monospacedDigit())
            .foregroundStyle(.secondary)
        }
        .card()
    }
}

/// Current spending against the same span before.
private struct CompareCard: View {
    let dashboard: Dashboard

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("Compared with before").font(.headline)
            row("Today", "vs yesterday", dashboard.compareDay)
            Divider()
            row("This week", "vs last week, same days", dashboard.compareWeek)
            Divider()
            row("This month", "vs last month, same days", dashboard.compareMonth)
        }
        .card()
    }

    private func row(_ title: String, _ subtitle: String, _ c: Comparison) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(title).font(.subheadline.weight(.medium))
                Text("\(subtitle): \(Fmt.money(c.previous, dashboard.currency))")
                    .font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            VStack(alignment: .trailing, spacing: 4) {
                Text(Fmt.money(c.current, dashboard.currency)).font(.subheadline.monospacedDigit().weight(.semibold))
                DeltaChip(comparison: c)
            }
        }
        .accessibilityElement(children: .combine)
    }
}

/// Spending per day for the last 7 days; a bar turns red when it went over that day's budget.
private struct LastDaysCard: View {
    let dashboard: Dashboard

    private struct Bar: Identifiable {
        let id: Int
        let label: String
        let spent: Double
        let budget: Double?
    }

    var body: some View {
        let bars = kotlinList(dashboard.bars, as: DayBar.self).enumerated().map { i, b in
            Bar(id: i, label: Fmt.weekdayLetter(b.date) + "\u{200B}\(i)", spent: Double(b.spent) / 100,
                budget: b.budget.map { Double($0.int64Value) / 100 })
        }
        VStack(alignment: .leading, spacing: 12) {
            Text("Last 7 days").font(.headline)
            Chart(bars) { bar in
                BarMark(x: .value("Day", bar.label), y: .value("Spent", bar.spent))
                    .foregroundStyle(bar.budget.map { bar.spent > $0 } == true ? Theme.negative : Theme.accent)
                    .clipShape(RoundedRectangle(cornerRadius: 4))
                if let budget = bar.budget {
                    RuleMark(xStart: .value("Day", bar.label), xEnd: .value("Day", bar.label), y: .value("Budget", budget))
                        .lineStyle(StrokeStyle(lineWidth: 2, dash: [3, 2]))
                        .foregroundStyle(.secondary)
                }
            }
            .chartXAxis {
                AxisMarks { value in
                    AxisValueLabel { Text((value.as(String.self) ?? "").prefix(1)) }
                }
            }
            .chartYAxis(.hidden)
            .frame(height: 120)
        }
        .card()
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Spending over the last 7 days")
    }
}

private struct RecentCard: View {
    let dashboard: Dashboard
    let onOpen: (String) -> Void
    let onSeeAll: () -> Void

    var body: some View {
        let recent = kotlinList(dashboard.recent, as: Expense.self)
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                Text("Recent").font(.headline)
                Spacer()
                if !recent.isEmpty { Button("See all", action: onSeeAll).font(.subheadline) }
            }
            if recent.isEmpty {
                Text("No expenses yet. Tap Add expense to log your first one.")
                    .font(.subheadline).foregroundStyle(.secondary)
            }
            ForEach(recent, id: \.id) { e in
                Button { onOpen(e.id) } label: { ExpenseRow(expense: e, currency: dashboard.currency) }
                    .buttonStyle(.plain)
            }
        }
        .card()
    }
}
