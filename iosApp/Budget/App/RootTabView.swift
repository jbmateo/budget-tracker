import SwiftUI

/// Three tabs; adding or editing an expense is a modal sheet shared by Home and Expenses.
struct RootTabView: View {
    enum Tab: Hashable { case home, expenses, budgets }

    /// `expenseId == nil` adds a new expense. `id` makes each opening distinct so the sheet gets a fresh ViewModel.
    private struct FormSheet: Identifiable {
        let expenseId: String?
        let id: Int
    }

    @State private var selection: Tab = .home
    @State private var formSheet: FormSheet?
    @State private var openCount = 0

    var body: some View {
        TabView(selection: $selection) {
            NavigationStack {
                HomeView(
                    onAdd: { openForm(nil) },
                    onOpen: { openForm($0) },
                    onSeeAll: { selection = .expenses },
                    onSetBudget: { selection = .budgets }
                )
            }
            .tabItem { Label("Home", systemImage: "house") }
            .tag(Tab.home)

            NavigationStack {
                ExpensesView(onAdd: { openForm(nil) }, onOpen: { openForm($0) })
            }
            .tabItem { Label("Expenses", systemImage: "list.bullet.rectangle") }
            .tag(Tab.expenses)

            NavigationStack {
                BudgetsView()
            }
            .tabItem { Label("Budgets", systemImage: "chart.pie") }
            .tag(Tab.budgets)
        }
        .tint(Theme.accent)
        .sheet(item: $formSheet) { sheet in
            NavigationStack {
                ExpenseFormView(expenseId: sheet.expenseId)
            }
        }
    }

    private func openForm(_ id: String?) {
        openCount += 1
        formSheet = FormSheet(expenseId: id, id: openCount)
    }
}
