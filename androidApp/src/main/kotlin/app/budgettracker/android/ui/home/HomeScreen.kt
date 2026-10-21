package app.budgettracker.android.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.budgettracker.android.ui.common.BudgetBar
import app.budgettracker.android.ui.common.Card
import app.budgettracker.android.ui.common.DeltaChip
import app.budgettracker.android.ui.common.ExpenseRow
import app.budgettracker.android.ui.common.longDay
import app.budgettracker.android.ui.common.money
import app.budgettracker.android.ui.common.signedMoney
import app.budgettracker.android.ui.common.weekdayLetter
import app.budgettracker.android.ui.theme.LocalBudgetColors
import app.budgettracker.domain.logic.Comparison
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.PeriodStatus
import app.budgettracker.domain.usecase.Dashboard
import app.budgettracker.presentation.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel
import kotlin.math.abs

@Composable
fun HomeScreen(
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onSeeAll: () -> Unit,
    onSetBudget: () -> Unit,
    vm: HomeViewModel = koinViewModel(),
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val d = state.dashboard
    Box(Modifier.fillMaxSize().background(LocalBudgetColors.current.canvas).padding(contentPadding)) {
        if (d == null) {
            CircularProgressIndicator(Modifier.align(Alignment.Center))
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                item {
                    Text(
                        longDay(d.today),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
                item { TodayHero(d, onSetBudget) }
                d.weekly?.let { item { PeriodCard("This week", it, d.currency) } }
                d.monthly?.let { item { PeriodCard("This month", it, d.currency) } }
                item { CompareCard(d) }
                item { LastDaysCard(d) }
                item { RecentCard(d, onOpen, onSeeAll) }
            }
        }
        ExtendedFloatingActionButton(
            onClick = onAdd,
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text("Add expense") },
            containerColor = LocalBudgetColors.current.hero,
            contentColor = LocalBudgetColors.current.heroText,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).testTag("addExpenseButton"),
        )
    }
}

@Composable
private fun TodayHero(d: Dashboard, onSetBudget: () -> Unit) {
    val c = LocalBudgetColors.current
    Column(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(c.hero).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        val s = d.daily
        if (s != null) {
            Column {
                Text(
                    if (s.left < 0) "Over today's budget by" else "Left to spend today",
                    color = c.heroSubtext,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    money(abs(s.left), d.currency),
                    color = if (s.left < 0) c.heroNegative else c.heroText,
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.testTag("leftToday"),
                )
            }
            BudgetBar(s.spent, maxOf(s.available, 0), track = c.heroPanel, fill = c.heroPositive, over = c.heroNegative)
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.heroPanel).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                HeroLine("Daily budget", money(s.budget, d.currency), c.heroText)
                if (s.carry != 0L) {
                    HeroLine(
                        if (s.carry > 0) "Rolled over" else "Overspent before",
                        signedMoney(s.carry, d.currency),
                        if (s.carry > 0) c.heroPositive else c.heroNegative,
                    )
                }
                HeroLine("Spent today", money(s.spent, d.currency), c.heroText)
            }
            if (s.left < 0) {
                Text(
                    "The extra ${money(-s.left, d.currency)} comes off tomorrow's budget.",
                    color = c.heroSubtext,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        } else {
            Text("Spent today", color = c.heroSubtext, style = MaterialTheme.typography.titleSmall)
            Text(money(d.compareDay.current, d.currency), color = c.heroText, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            Button(onClick = onSetBudget, colors = ButtonDefaults.buttonColors(containerColor = c.heroPanel, contentColor = c.heroText)) {
                Text("Set a daily budget")
            }
        }
    }
}

@Composable
private fun HeroLine(label: String, value: String, valueColor: Color) {
    Row(Modifier.fillMaxWidth()) {
        Text(label, color = LocalBudgetColors.current.heroSubtext, modifier = Modifier.weight(1f))
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun PeriodCard(title: String, s: PeriodStatus, currency: Currency) {
    val c = LocalBudgetColors.current
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Text(
                if (s.daysLeft == 1) "Last day" else "${s.daysLeft} days left",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                money(abs(s.left), currency),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (s.left < 0) c.negative else MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(6.dp))
            Text(if (s.left < 0) "over" else "left", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(Modifier.height(8.dp))
        BudgetBar(s.spent, maxOf(s.available, 0))
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            val muted = MaterialTheme.colorScheme.onSurfaceVariant
            Text("Spent ${money(s.spent, currency)}", style = MaterialTheme.typography.bodySmall, color = muted)
            Text("Budget ${money(s.budget, currency)}", style = MaterialTheme.typography.bodySmall, color = muted)
            if (s.carry != 0L) {
                Text(
                    signedMoney(s.carry, currency),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (s.carry > 0) c.positive else c.negative,
                )
            }
        }
    }
}

@Composable
private fun CompareCard(d: Dashboard) {
    Card {
        Text("Compared with before", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        CompareRow("Today", "vs yesterday", d.compareDay, d.currency)
        HorizontalDivider()
        CompareRow("This week", "vs last week, same days", d.compareWeek, d.currency)
        HorizontalDivider()
        CompareRow("This month", "vs last month, same days", d.compareMonth, d.currency)
    }
}

@Composable
private fun CompareRow(title: String, subtitle: String, c: Comparison, currency: Currency) {
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            Text(
                "$subtitle: ${money(c.previous, currency)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(money(c.current, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            DeltaChip(c)
        }
    }
}

/** Spending per day for the last 7 days; bars over that day's budget turn red, the budget shows as a line. */
@Composable
private fun LastDaysCard(d: Dashboard) {
    val c = LocalBudgetColors.current
    val primary = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.outline
    val max = d.bars.maxOf { maxOf(it.spent, it.budget ?: 0L) }.coerceAtLeast(1L)
    Card(Modifier.semantics { contentDescription = "Spending over the last 7 days" }) {
        Text("Last 7 days", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            d.bars.forEach { bar ->
                Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
                        val over = bar.budget?.let { bar.spent > it } == true
                        Box(
                            Modifier.fillMaxWidth(0.7f).fillMaxHeight(bar.spent.toFloat() / max)
                                .clip(RoundedCornerShape(4.dp)).background(if (over) c.negative else primary),
                        )
                        bar.budget?.let { b ->
                            Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.Bottom) {
                                Box(Modifier.width(24.dp).height(2.dp).background(outline))
                                Spacer(Modifier.fillMaxHeight(b.toFloat() / max))
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(weekdayLetter(bar.date), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun RecentCard(d: Dashboard, onOpen: (String) -> Unit, onSeeAll: () -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Recent", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (d.recent.isNotEmpty()) TextButton(onClick = onSeeAll) { Text("See all") }
        }
        if (d.recent.isEmpty()) {
            Text(
                "No expenses yet. Tap Add expense to log your first one.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        d.recent.forEach { e -> ExpenseRow(e, d.currency, modifier = Modifier.clickable { onOpen(e.id) }) }
    }
}
