import SwiftUI
import Shared

/// Money and date formatting shared by all screens. Money text comes from the shared `MoneyFormatter`.
enum Fmt {
    static func money(_ minor: Int64, _ currency: Currency) -> String {
        MoneyFormatter.shared.format(minor: minor, currency: currency)
    }

    /// `+₱120.00` / `-₱80.00`.
    static func signed(_ minor: Int64, _ currency: Currency) -> String {
        (minor > 0 ? "+" : "") + money(minor, currency)
    }

    static func date(_ d: Kotlinx_datetimeLocalDate) -> Date {
        let components = DateComponents(year: Int(d.year), month: Int(d.month.ordinal) + 1, day: Int(d.day))
        return Calendar.current.date(from: components) ?? .now
    }

    static func longDay(_ d: Kotlinx_datetimeLocalDate) -> String {
        date(d).formatted(.dateTime.weekday(.wide).month(.wide).day())
    }

    static func shortDay(_ d: Kotlinx_datetimeLocalDate) -> String {
        date(d).formatted(.dateTime.weekday(.abbreviated).month(.abbreviated).day())
    }

    static func weekdayLetter(_ d: Kotlinx_datetimeLocalDate) -> String {
        date(d).formatted(.dateTime.weekday(.narrow))
    }

    static func month(year: Int32, month: Int32) -> String {
        let value = Calendar.current.date(from: DateComponents(year: Int(year), month: Int(month), day: 1)) ?? .now
        return value.formatted(.dateTime.month(.wide).year())
    }
}

/// SF Symbol and tint per expense category.
extension ExpenseCategory {
    var symbol: String {
        switch self {
        case .food: "fork.knife"
        case .groceries: "cart"
        case .transport: "car"
        case .shopping: "bag"
        case .bills: "bolt"
        case .entertainment: "gamecontroller"
        case .health: "cross.case"
        case .other: "square.grid.2x2"
        }
    }

    var tint: Color {
        switch self {
        case .food: .orange
        case .groceries: .green
        case .transport: .blue
        case .shopping: .pink
        case .bills: .yellow
        case .entertainment: .purple
        case .health: .red
        case .other: .gray
        }
    }
}

struct CategoryIcon: View {
    let category: ExpenseCategory
    var size: CGFloat = 36

    var body: some View {
        Image(systemName: category.symbol)
            .font(.system(size: size * 0.42, weight: .semibold))
            .foregroundStyle(category.tint)
            .frame(width: size, height: size)
            .background(category.tint.opacity(0.15), in: RoundedRectangle(cornerRadius: size * 0.3, style: .continuous))
            .accessibilityHidden(true)
    }
}

/// One expense in a list.
struct ExpenseRow: View {
    let expense: Expense
    let currency: Currency
    var showDate = true

    var body: some View {
        HStack(spacing: 12) {
            CategoryIcon(category: expense.category)
            VStack(alignment: .leading, spacing: 2) {
                Text(expense.note.isEmpty ? expense.category.label : expense.note)
                    .font(.body)
                    .lineLimit(1)
                Text(showDate ? "\(expense.category.label) · \(Fmt.shortDay(expense.date))" : expense.category.label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            Spacer(minLength: 8)
            Text(Fmt.money(expense.amount, currency))
                .font(.body.monospacedDigit().weight(.medium))
        }
        .contentShape(Rectangle())
        .accessibilityElement(children: .combine)
    }
}

/// Thin rounded progress bar; turns red when [value] exceeds [total].
struct BudgetBar: View {
    let value: Int64
    let total: Int64
    var track: Color = Theme.track
    var fill: Color = Theme.accent
    var over: Color = Theme.negative

    var body: some View {
        GeometryReader { geo in
            let ratio = total > 0 ? min(Double(value) / Double(total), 1) : (value > 0 ? 1 : 0)
            ZStack(alignment: .leading) {
                Capsule().fill(track)
                Capsule().fill(value > total ? over : fill).frame(width: geo.size.width * ratio)
            }
        }
        .frame(height: 8)
        .accessibilityHidden(true)
    }
}

/// Spent-more / spent-less pill: red arrow up when spending rose, green arrow down when it fell.
struct DeltaChip: View {
    let comparison: Comparison

    var body: some View {
        if let pct = comparison.deltaPercent?.intValue {
            let up = pct > 0
            let color = pct == 0 ? Color.secondary : (up ? Theme.negative : Theme.positive)
            Label("\(abs(pct))%", systemImage: pct == 0 ? "equal" : (up ? "arrow.up" : "arrow.down"))
                .font(.caption.weight(.semibold).monospacedDigit())
                .foregroundStyle(color)
                .padding(.horizontal, 8).padding(.vertical, 3)
                .background(color.opacity(0.12), in: Capsule())
                .accessibilityLabel(pct == 0 ? "Same as before" : "\(abs(pct)) percent \(up ? "more" : "less")")
        } else {
            Text(comparison.current > 0 ? "new" : "—")
                .font(.caption.weight(.semibold))
                .foregroundStyle(.secondary)
        }
    }
}
