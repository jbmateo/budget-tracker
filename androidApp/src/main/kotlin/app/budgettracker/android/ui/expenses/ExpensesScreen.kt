package app.budgettracker.android.ui.expenses

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.budgettracker.android.ui.common.Card
import app.budgettracker.android.ui.common.CategoryIcon
import app.budgettracker.android.ui.common.DeltaChip
import app.budgettracker.android.ui.common.ExpenseRow
import app.budgettracker.android.ui.common.money
import app.budgettracker.android.ui.common.monthTitle
import app.budgettracker.android.ui.common.shortDay
import app.budgettracker.android.ui.common.tint
import app.budgettracker.android.ui.theme.LocalBudgetColors
import app.budgettracker.domain.logic.CategoryComparison
import app.budgettracker.domain.logic.Comparison
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.Expense
import app.budgettracker.presentation.expenses.ExpensesViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun ExpensesScreen(
    contentPadding: PaddingValues,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    vm: ExpensesViewModel = koinViewModel(),
) {
    val s by vm.state.collectAsStateWithLifecycle()
    Box(Modifier.fillMaxSize().background(LocalBudgetColors.current.canvas).padding(contentPadding)) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = vm::previousMonth) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        monthTitle(s.year, s.monthNumber),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                    IconButton(onClick = vm::nextMonth, enabled = s.canGoNext) {
                        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            }
            val data = s.data
            if (data == null) {
                item { Box(Modifier.fillMaxWidth().padding(32.dp), Alignment.Center) { CircularProgressIndicator() } }
                return@LazyColumn
            }
            item {
                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(money(data.total, data.currency), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                "Last month, same days: ${money(data.previousTotal, data.currency)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        DeltaChip(Comparison(data.total, data.previousTotal))
                    }
                }
            }
            if (data.categories.isNotEmpty()) {
                item {
                    val max = data.categories.maxOf { maxOf(it.current, it.previous) }.coerceAtLeast(1)
                    Card {
                        Text("By category", style = MaterialTheme.typography.titleMedium)
                        data.categories.forEach { CategoryLine(it, max, data.currency) }
                    }
                }
            }
            if (data.days.isEmpty()) {
                item {
                    Text(
                        "No expenses this month.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(8.dp),
                    )
                }
            }
            data.days.forEach { day ->
                item(key = day.date.toString()) {
                    Row(Modifier.padding(horizontal = 4.dp, vertical = 4.dp)) {
                        Text(shortDay(day.date), style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                        Text(money(day.total, data.currency), style = MaterialTheme.typography.labelLarge)
                    }
                }
                items(day.items, key = { it.id }) { e -> SwipeRow(e, data.currency, onOpen = { onOpen(e.id) }, onDelete = { vm.delete(e.id) }) }
            }
        }
        FloatingActionButton(
            onClick = onAdd,
            containerColor = LocalBudgetColors.current.hero,
            contentColor = LocalBudgetColors.current.heroText,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
        ) { Icon(Icons.Rounded.Add, contentDescription = "Add expense") }
    }
}

@Composable
private fun SwipeRow(e: Expense, currency: Currency, onOpen: () -> Unit, onDelete: () -> Unit) {
    val state = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) onDelete()
            value == SwipeToDismissBoxValue.EndToStart
        },
    )
    SwipeToDismissBox(
        state = state,
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(
                Modifier.fillMaxSize().clip(MaterialTheme.shapes.large).background(LocalBudgetColors.current.negative).padding(horizontal = 20.dp),
                contentAlignment = Alignment.CenterEnd,
            ) { Icon(Icons.Rounded.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onError) }
        },
    ) {
        Box(
            Modifier.clip(MaterialTheme.shapes.large).background(LocalBudgetColors.current.card)
                .clickable(onClick = onOpen).padding(horizontal = 12.dp, vertical = 4.dp),
        ) { ExpenseRow(e, currency, showDate = false) }
    }
}

@Composable
private fun CategoryLine(row: CategoryComparison, max: Long, currency: Currency) {
    Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        CategoryIcon(row.category, size = 30.dp)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row {
                Text(row.category.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(money(row.current, currency), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
            Box(Modifier.fillMaxWidth().height(6.dp)) {
                Box(Modifier.fillMaxWidth(row.previous.toFloat() / max).fillMaxHeight().clip(CircleShape).background(LocalBudgetColors.current.track))
                Box(Modifier.fillMaxWidth(row.current.toFloat() / max).fillMaxHeight().clip(CircleShape).background(row.category.tint))
            }
            Text(
                "Last month: ${money(row.previous, currency)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
