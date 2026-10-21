import SwiftUI
import Shared

struct BudgetsView: View {
    var body: some View {
        Hosted(make: { KoinHelper.shared.budgets() }, state: { $0.state }) { host in
            BudgetsContent(host: host)
        }
    }
}

extension Period {
    var title: String {
        switch self {
        case .daily: "Daily"
        case .weekly: "Weekly"
        case .monthly: "Monthly"
        }
    }

    var carryUnit: String {
        switch self {
        case .daily: "earlier days"
        case .weekly: "earlier weeks"
        case .monthly: "earlier months"
        }
    }
}

private struct BudgetsContent: View {
    let host: ViewModelHost<BudgetsViewModel, BudgetsUiState>
    @State private var editing: Period?

    var body: some View {
        let s = host.state
        List {
            Section {
                ForEach(kotlinList(s.rows, as: BudgetRow.self), id: \.period) { row in
                    Button { editing = row.period } label: { rowView(row, currency: s.currency) }
                        .buttonStyle(.plain)
                        .accessibilityIdentifier("budgetRow_\(row.period.name)")
                }
            } footer: {
                Text("Money you don't spend rolls over to the next period and keeps adding up. Spend more than your budget and the extra comes off the next period.")
            }
            Section("Currency") {
                Picker("Currency", selection: Binding(get: { host.state.currency }, set: { host.vm.selectCurrency(currency: $0) })) {
                    ForEach(kotlinList(s.currencies, as: Currency.self), id: \.self) { c in
                        Text("\(c.code) (\(c.symbol))").tag(c)
                    }
                }
            }
        }
        .navigationTitle("Budgets")
        .sheet(item: $editing) { period in
            NavigationStack {
                BudgetEditor(
                    period: period,
                    current: kotlinList(s.rows, as: BudgetRow.self).first { $0.period == period }?.amount?.int64Value,
                    currency: s.currency,
                    host: host
                )
            }
            .presentationDetents([.medium])
        }
        .task { host.start() }
    }

    private func rowView(_ row: BudgetRow, currency: Currency) -> some View {
        HStack {
            VStack(alignment: .leading, spacing: 2) {
                Text(row.period.title).font(.body.weight(.medium))
                if let carry = row.carry?.int64Value, carry != 0 {
                    Text("\(Fmt.signed(carry, currency)) from \(row.period.carryUnit)")
                        .font(.caption)
                        .foregroundStyle(carry > 0 ? Theme.positive : Theme.negative)
                }
            }
            Spacer()
            if let amount = row.amount?.int64Value {
                Text(Fmt.money(amount, currency)).monospacedDigit()
            } else {
                Text("Not set").foregroundStyle(.secondary)
            }
            Image(systemName: "chevron.right").font(.caption).foregroundStyle(.tertiary)
        }
        .contentShape(Rectangle())
    }
}

extension Period: @retroactive Identifiable {
    public var id: String { name }
}

private struct BudgetEditor: View {
    let period: Period
    let current: Int64?
    let currency: Currency
    let host: ViewModelHost<BudgetsViewModel, BudgetsUiState>

    @Environment(\.dismiss) private var dismiss
    @State private var text = ""
    @FocusState private var focused: Bool

    var body: some View {
        Form {
            Section {
                HStack {
                    Text(currency.symbol).foregroundStyle(.secondary)
                    TextField("0.00", text: $text)
                        .keyboardType(.decimalPad)
                        .focused($focused)
                        .accessibilityIdentifier("budgetAmountField")
                }
                .font(.title2.monospacedDigit())
                if host.state.invalidPeriod == period {
                    Text("Enter an amount above zero, with at most 2 decimals.")
                        .font(.footnote).foregroundStyle(Theme.negative)
                }
            } footer: {
                Text("Changes apply from the current \(period == .daily ? "day" : period == .weekly ? "week" : "month") on. Past periods keep their budget.")
            }
            if current != nil {
                Section {
                    Button("Remove budget", role: .destructive) {
                        host.vm.remove(period: period)
                        dismiss()
                    }
                }
            }
        }
        .navigationTitle("\(period.title) budget")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { if host.vm.save(period: period, input: text) { dismiss() } }
                    .accessibilityIdentifier("saveBudgetButton")
            }
        }
        .onAppear {
            host.vm.clearError()
            text = current.map { MoneyFormatter.shared.plain(minor: $0) } ?? ""
            focused = true
        }
    }
}
