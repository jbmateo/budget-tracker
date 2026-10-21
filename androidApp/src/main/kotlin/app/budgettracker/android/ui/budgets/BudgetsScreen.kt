package app.budgettracker.android.ui.budgets

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.budgettracker.android.ui.common.Card
import app.budgettracker.android.ui.common.money
import app.budgettracker.android.ui.common.signedMoney
import app.budgettracker.android.ui.theme.LocalBudgetColors
import app.budgettracker.domain.model.Currency
import app.budgettracker.domain.model.MoneyFormatter
import app.budgettracker.domain.model.Period
import app.budgettracker.presentation.budgets.BudgetRow
import app.budgettracker.presentation.budgets.BudgetsViewModel
import org.koin.androidx.compose.koinViewModel

val Period.title get() = when (this) { Period.DAILY -> "Daily"; Period.WEEKLY -> "Weekly"; Period.MONTHLY -> "Monthly" }
private val Period.unit get() = when (this) { Period.DAILY -> "day"; Period.WEEKLY -> "week"; Period.MONTHLY -> "month" }

@Composable
fun BudgetsScreen(contentPadding: PaddingValues, vm: BudgetsViewModel = koinViewModel()) {
    val s by vm.state.collectAsStateWithLifecycle()
    var editing by rememberSaveable { mutableStateOf<Period?>(null) }
    Column(
        Modifier.fillMaxSize().background(LocalBudgetColors.current.canvas).padding(contentPadding)
            .verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text("Budgets", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Card {
            s.rows.forEachIndexed { i, row ->
                if (i > 0) HorizontalDivider()
                BudgetLine(row, s.currency, Modifier.clickable { editing = row.period }.testTag("budgetRow_${row.period.name}"))
            }
        }
        Text(
            "Money you don't spend rolls over to the next period and keeps adding up. Spend more than your budget and the extra comes off the next period.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        Card {
            Text("Currency", style = MaterialTheme.typography.titleMedium)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                s.currencies.forEach { c ->
                    FilterChip(selected = c == s.currency, onClick = { vm.selectCurrency(c) }, label = { Text("${c.code} ${c.symbol}") })
                }
            }
        }
    }
    editing?.let { period ->
        BudgetEditor(
            period = period,
            current = s.rows.first { it.period == period }.amount,
            currency = s.currency,
            invalid = s.invalidPeriod == period,
            onSave = { text -> if (vm.save(period, text)) editing = null },
            onRemove = { vm.remove(period); editing = null },
            onDismiss = { vm.clearError(); editing = null },
        )
    }
}

@Composable
private fun BudgetLine(row: BudgetRow, currency: Currency, modifier: Modifier) {
    val colors = LocalBudgetColors.current
    Row(modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(row.period.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            val carry = row.carry
            if (carry != null && carry != 0L) {
                Text(
                    "${signedMoney(carry, currency)} from earlier ${row.period.unit}s",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (carry > 0) colors.positive else colors.negative,
                )
            }
        }
        Text(
            row.amount?.let { money(it, currency) } ?: "Not set",
            color = if (row.amount == null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
        )
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
    }
}

@Composable
private fun BudgetEditor(
    period: Period,
    current: Long?,
    currency: Currency,
    invalid: Boolean,
    onSave: (String) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(current?.let(MoneyFormatter::plain) ?: "") }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("${period.title} budget") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    prefix = { Text(currency.symbol) },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    isError = invalid,
                    supportingText = if (invalid) { { Text("Enter an amount above zero, with at most 2 decimals.") } } else null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.focusRequester(focus).testTag("budgetAmountField"),
                )
                Text(
                    "Changes apply from the current ${period.unit} on. Past periods keep their budget.",
                    style = MaterialTheme.typography.bodySmall,
                )
                if (current != null) {
                    TextButton(onClick = onRemove) { Text("Remove budget", color = LocalBudgetColors.current.negative) }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(text) }, modifier = Modifier.testTag("saveBudgetButton")) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
