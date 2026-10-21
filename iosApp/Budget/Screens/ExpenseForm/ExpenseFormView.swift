import SwiftUI
import Shared

struct ExpenseFormView: View {
    let expenseId: String?

    var body: some View {
        Hosted(make: { KoinHelper.shared.expenseForm(id: expenseId) }, state: { $0.state }) { host in
            ExpenseFormContent(host: host)
        }
    }
}

private struct ExpenseFormContent: View {
    let host: ViewModelHost<ExpenseFormViewModel, ExpenseFormUiState>
    @Environment(\.dismiss) private var dismiss
    @FocusState private var amountFocused: Bool
    @State private var confirmDelete = false

    var body: some View {
        let s = host.state
        Form {
            Section {
                TextField("0.00", text: Binding(get: { host.state.amountText }, set: { host.vm.setAmount(text: $0) }))
                    .keyboardType(.decimalPad)
                    .font(.system(size: 40, weight: .bold, design: .rounded).monospacedDigit())
                    .focused($amountFocused)
                    .accessibilityIdentifier("amountField")
                    .accessibilityLabel("Amount")
                if s.error == .invalidAmount {
                    Text("Enter an amount above zero, with at most 2 decimals.")
                        .font(.footnote).foregroundStyle(Theme.negative)
                }
            } header: {
                Text("Amount")
            }

            Section("Category") {
                LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 8), count: 4), spacing: 12) {
                    ForEach(kotlinList(s.categories, as: ExpenseCategory.self), id: \.self) { c in
                        Button { host.vm.setCategory(category: c) } label: {
                            VStack(spacing: 6) {
                                CategoryIcon(category: c, size: 44)
                                    .overlay {
                                        RoundedRectangle(cornerRadius: 13.2, style: .continuous)
                                            .stroke(s.category == c ? c.tint : .clear, lineWidth: 2.5)
                                    }
                                Text(c.label).font(.caption2).lineLimit(1)
                                    .foregroundStyle(s.category == c ? .primary : .secondary)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel(c.label)
                        .accessibilityAddTraits(s.category == c ? .isSelected : [])
                    }
                }
                .padding(.vertical, 4)
            }

            Section {
                DatePicker(
                    "Date",
                    selection: Binding(
                        get: { Fmt.date(host.state.date) },
                        set: { d in
                            let c = Calendar.current.dateComponents([.year, .month, .day], from: d)
                            host.vm.setDate(year: Int32(c.year!), month: Int32(c.month!), day: Int32(c.day!))
                        }
                    ),
                    in: ...Fmt.date(s.maxDate),
                    displayedComponents: .date
                )
                if s.error == .futureDate {
                    Text("Expenses can't be in the future.").font(.footnote).foregroundStyle(Theme.negative)
                }
                TextField("Note (optional)", text: Binding(get: { host.state.note }, set: { host.vm.setNote(note: $0) }))
                    .accessibilityIdentifier("noteField")
                if s.error == .noteTooLong {
                    Text("Keep notes to 80 characters.").font(.footnote).foregroundStyle(Theme.negative)
                }
            }

            if s.isEdit {
                Section {
                    Button("Delete expense", role: .destructive) { confirmDelete = true }
                }
            }
        }
        .navigationTitle(s.isEdit ? "Edit expense" : "New expense")
        .navigationBarTitleDisplayMode(.inline)
        .toolbar {
            ToolbarItem(placement: .cancellationAction) { Button("Cancel") { dismiss() } }
            ToolbarItem(placement: .confirmationAction) {
                Button("Save") { host.vm.save() }
                    .disabled(s.isSaving || s.isLoading)
                    .accessibilityIdentifier("saveButton")
            }
        }
        .confirmationDialog("Delete this expense?", isPresented: $confirmDelete, titleVisibility: .visible) {
            Button("Delete", role: .destructive) { host.vm.delete() }
        }
        .alert("This expense no longer exists.", isPresented: .constant(s.error == .notFound)) {
            Button("OK") { dismiss() }
        }
        .onChange(of: s.done) { _, done in if done { dismiss() } }
        .task {
            host.start()
            if !s.isEdit { amountFocused = true }
        }
    }
}
