package app.budgettracker.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Colors beyond Material's scheme: the emerald hero and money-state colors (same hexes as iOS Theme.swift). */
@Immutable
data class BudgetColors(
    val canvas: Color,
    val card: Color,
    val hero: Color,
    val heroText: Color,
    val heroSubtext: Color,
    val heroPanel: Color,
    val heroPositive: Color,
    val heroNegative: Color,
    val positive: Color,
    val negative: Color,
    val track: Color,
)

private val Light = BudgetColors(
    canvas = Color(0xFFF3F5F2), card = Color.White, hero = Color(0xFF0E5C47), heroText = Color(0xFFF1FBF6),
    heroSubtext = Color(0xFFBFE3D4), heroPanel = Color(0xFF0A4A39), heroPositive = Color(0xFF8AF0C4),
    heroNegative = Color(0xFFFFB3A3), positive = Color(0xFF16794F), negative = Color(0xFFC2412D), track = Color(0xFFE3E8E5),
)

private val Dark = Light.copy(
    canvas = Color(0xFF101412), card = Color(0xFF1A201D), hero = Color(0xFF0B4A3A), heroPanel = Color(0xFF083B2E),
    positive = Color(0xFF5FD3A6), negative = Color(0xFFFF8A73), track = Color(0xFF2A322E),
)

val LocalBudgetColors = staticCompositionLocalOf { Light }

@Composable
fun BudgetTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val scheme = if (dark) {
        darkColorScheme(primary = Color(0xFF5FD3A6), onPrimary = Color(0xFF003828), background = Dark.canvas, surface = Dark.canvas)
    } else {
        lightColorScheme(primary = Color(0xFF0E5C47), onPrimary = Color.White, background = Light.canvas, surface = Light.canvas)
    }
    CompositionLocalProvider(LocalBudgetColors provides if (dark) Dark else Light) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
