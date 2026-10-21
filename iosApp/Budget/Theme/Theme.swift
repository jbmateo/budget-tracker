import SwiftUI
import UIKit

/// Light/dark hex pairs: a soft canvas, white cards and a deep emerald hero.
enum Theme {
    static let canvas = dynamic(0xF3F5F2, 0x101412)
    static let card = dynamic(0xFFFFFF, 0x1A201D)
    static let hero = dynamic(0x0E5C47, 0x0B4A3A)
    static let heroText = dynamic(0xF1FBF6, 0xF1FBF6)
    static let heroSubtext = dynamic(0xBFE3D4, 0xBFE3D4)
    static let heroPanel = dynamic(0x0A4A39, 0x083B2E)
    static let accent = dynamic(0x0E5C47, 0x5FD3A6)
    /// Money saved / under budget.
    static let positive = dynamic(0x16794F, 0x5FD3A6)
    /// Overspent / over budget.
    static let negative = dynamic(0xC2412D, 0xFF8A73)
    /// Rollover chips on the hero, readable on emerald.
    static let heroPositive = dynamic(0x8AF0C4, 0x8AF0C4)
    static let heroNegative = dynamic(0xFFB3A3, 0xFFB3A3)
    static let track = dynamic(0xE3E8E5, 0x2A322E)

    static let cornerRadius: CGFloat = 20

    private static func dynamic(_ light: UInt32, _ dark: UInt32) -> Color {
        Color(UIColor { $0.userInterfaceStyle == .dark ? UIColor(hex: dark) : UIColor(hex: light) })
    }
}

extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

/// White rounded card on the canvas.
struct CardBackground: ViewModifier {
    func body(content: Content) -> some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.card, in: RoundedRectangle(cornerRadius: Theme.cornerRadius, style: .continuous))
    }
}

extension View {
    func card() -> some View { modifier(CardBackground()) }
}
