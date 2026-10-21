package app.budgettracker.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingDown
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.LocalGroceryStore
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.budgettracker.android.ui.theme.LocalBudgetColors
import app.budgettracker.domain.logic.Comparison
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.domain.model.ExpenseCategory
import app.budgettracker.domain.model.MoneyFormatter
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

fun money(minor: Long, currency: Currency): String = MoneyFormatter.format(minor, currency)
fun signedMoney(minor: Long, currency: Currency): String = (if (minor > 0) "+" else "") + money(minor, currency)

fun longDay(d: LocalDate): String = d.toJavaLocalDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d"))
fun shortDay(d: LocalDate): String = d.toJavaLocalDate().format(DateTimeFormatter.ofPattern("EEE, MMM d"))
fun mediumDate(d: LocalDate): String = d.toJavaLocalDate().format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
fun weekdayLetter(d: LocalDate): String = d.toJavaLocalDate().format(DateTimeFormatter.ofPattern("EEEEE"))
fun monthTitle(year: Int, month: Int): String =
    java.time.YearMonth.of(year, month).format(DateTimeFormatter.ofPattern("MMMM yyyy"))

val ExpenseCategory.icon: ImageVector
    get() = when (this) {
        ExpenseCategory.FOOD -> Icons.Rounded.Restaurant
        ExpenseCategory.GROCERIES -> Icons.Rounded.LocalGroceryStore
        ExpenseCategory.TRANSPORT -> Icons.Rounded.DirectionsCar
        ExpenseCategory.SHOPPING -> Icons.Rounded.ShoppingBag
        ExpenseCategory.BILLS -> Icons.Rounded.Bolt
        ExpenseCategory.ENTERTAINMENT -> Icons.Rounded.SportsEsports
        ExpenseCategory.HEALTH -> Icons.Rounded.LocalHospital
        ExpenseCategory.OTHER -> Icons.Rounded.Apps
    }

val ExpenseCategory.tint: Color
    get() = when (this) {
        ExpenseCategory.FOOD -> Color(0xFFF08C00)
        ExpenseCategory.GROCERIES -> Color(0xFF2F9E44)
        ExpenseCategory.TRANSPORT -> Color(0xFF1C7ED6)
        ExpenseCategory.SHOPPING -> Color(0xFFD6336C)
        ExpenseCategory.BILLS -> Color(0xFFE0A800)
        ExpenseCategory.ENTERTAINMENT -> Color(0xFF7048E8)
        ExpenseCategory.HEALTH -> Color(0xFFE03131)
        ExpenseCategory.OTHER -> Color(0xFF868E96)
    }

@Composable
fun CategoryIcon(category: ExpenseCategory, size: Dp = 36.dp) {
    Box(
        Modifier.size(size).clip(RoundedCornerShape(size * 0.3f)).background(category.tint.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(category.icon, contentDescription = null, tint = category.tint, modifier = Modifier.size(size * 0.5f))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(LocalBudgetColors.current.card).padding(16.dp),
        content = content,
    )
}

@Composable
fun ExpenseRow(expense: Expense, currency: Currency, showDate: Boolean = true, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        CategoryIcon(expense.category)
        Column(Modifier.weight(1f).padding(horizontal = 12.dp)) {
            Text(
                expense.note.ifEmpty { expense.category.label },
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                if (showDate) "${expense.category.label} · ${shortDay(expense.date)}" else expense.category.label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(money(expense.amount, currency), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
    }
}

/** Thin progress bar; switches to [over] when [value] exceeds [total]. */
@Composable
fun BudgetBar(
    value: Long,
    total: Long,
    track: Color = LocalBudgetColors.current.track,
    fill: Color = MaterialTheme.colorScheme.primary,
    over: Color = LocalBudgetColors.current.negative,
) {
    val ratio = if (total > 0) (value.toFloat() / total).coerceIn(0f, 1f) else if (value > 0) 1f else 0f
    Box(Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).background(track)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(ratio).clip(CircleShape).background(if (value > total) over else fill))
    }
}

/** Red up-arrow when spending rose, green down-arrow when it fell, "new" when there is nothing to compare. */
@Composable
fun DeltaChip(c: Comparison) {
    val pct = c.deltaPercent
    if (pct == null) {
        Text(if (c.current > 0) "new" else "—", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val colors = LocalBudgetColors.current
    val color = when {
        pct > 0 -> colors.negative
        pct < 0 -> colors.positive
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        Modifier.clip(CircleShape).background(color.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (pct != 0) {
            Icon(
                if (pct > 0) Icons.AutoMirrored.Rounded.TrendingUp else Icons.AutoMirrored.Rounded.TrendingDown,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(4.dp))
        }
        Text("${abs(pct)}%", style = MaterialTheme.typography.labelMedium, color = color, fontWeight = FontWeight.SemiBold)
    }
}
