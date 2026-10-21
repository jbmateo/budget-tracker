import SwiftUI
import Shared

struct ExpensesView: View {
    let onAdd: () -> Void
    let onOpen: (String) -> Void

    var body: some View {
        Hosted(make: { KoinHelper.shared.expenses() }, state: { $0.state }) { host in
            ExpensesContent(host: host, onAdd: onAdd, onOpen: onOpen)
        }
    }
}

private struct ExpensesContent: View {
    let host: ViewModelHost<ExpensesViewModel, ExpensesUiState>
    let onAdd: () -> Void
    let onOpen: (String) -> Void

    var body: some View {
        let s = host.state
        List {
            Section {
                HStack {
                    Button { host.vm.previousMonth() } label: { Image(systemName: "chevron.left") }
                        .accessibilityLabel("Previous month")
                    Spacer()
                    Text(Fmt.month(year: s.year, month: s.monthNumber)).font(.headline)
                    Spacer()
                    Button { host.vm.nextMonth() } label: { Image(systemName: "chevron.right") }
                        .disabled(!s.canGoNext)
                        .accessibilityLabel("Next month")
                }
                .buttonStyle(.borderless)
                if let data = s.data {
                    summary(data)
                }
            }
            if let data = s.data {
                let categories = kotlinList(data.categories, as: CategoryComparison.self)
                if !categories.isEmpty {
                    Section("By category") {
                        ForEach(categories, id: \.category) { row in
                            categoryRow(row, max: categories.map { max($0.current, $0.previous) }.max() ?? 1, currency: data.currency)
                        }
                    }
                }
                let days = kotlinList(data.days, as: DayGroup.self)
                if days.isEmpty {
                    Section {
                        Text("No expenses this month.").foregroundStyle(.secondary)
                    }
                }
                ForEach(days, id: \.date) { day in
                    Section {
                        ForEach(kotlinList(day.items, as: Expense.self), id: \.id) { e in
                            Button { onOpen(e.id) } label: { ExpenseRow(expense: e, currency: data.currency, showDate: false) }
                                .buttonStyle(.plain)
                                .swipeActions {
                                    Button("Delete", role: .destructive) { host.vm.delete(id: e.id) }
                                }
                        }
                    } header: {
                        HStack {
                            Text(Fmt.shortDay(day.date))
                            Spacer()
                            Text(Fmt.money(day.total, data.currency)).monospacedDigit()
                        }
                    }
                }
            }
        }
        .navigationTitle("Expenses")
        .toolbar {
            ToolbarItem(placement: .topBarTrailing) {
                Button(action: onAdd) { Label("Add expense", systemImage: "plus") }
            }
        }
        .task { host.start() }
    }

    private func summary(_ data: MonthExpenses) -> some View {
        HStack(alignment: .firstTextBaseline) {
            VStack(alignment: .leading, spacing: 2) {
                Text(Fmt.money(data.total, data.currency)).font(.title2.bold().monospacedDigit())
                Text("Last month, same days: \(Fmt.money(data.previousTotal, data.currency))")
                    .font(.caption).foregroundStyle(.secondary)
            }
            Spacer()
            DeltaChip(comparison: Comparison(current: data.total, previous: data.previousTotal))
        }
        .accessibilityElement(children: .combine)
    }

    private func categoryRow(_ row: CategoryComparison, max: Int64, currency: Currency) -> some View {
        HStack(spacing: 12) {
            CategoryIcon(category: row.category, size: 30)
            VStack(alignment: .leading, spacing: 4) {
                HStack {
                    Text(row.category.label).font(.subheadline)
                    Spacer()
                    Text(Fmt.money(row.current, currency)).font(.subheadline.monospacedDigit().weight(.medium))
                }
                GeometryReader { geo in
                    let w = geo.size.width
                    ZStack(alignment: .leading) {
                        Capsule().fill(Theme.track).frame(width: w * CGFloat(row.previous) / CGFloat(Swift.max(max, 1)))
                        Capsule().fill(row.category.tint).frame(width: w * CGFloat(row.current) / CGFloat(Swift.max(max, 1)))
                    }
                }
                .frame(height: 6)
                Text("Last month: \(Fmt.money(row.previous, currency))").font(.caption2).foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
    }
}
