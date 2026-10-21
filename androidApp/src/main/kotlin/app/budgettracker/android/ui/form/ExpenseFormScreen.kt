package app.budgettracker.android.ui.form

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.budgettracker.android.ui.common.CategoryIcon
import app.budgettracker.android.ui.common.mediumDate
import app.budgettracker.android.ui.common.tint
import app.budgettracker.android.ui.theme.LocalBudgetColors
import app.budgettracker.domain.model.DomainError
import app.budgettracker.presentation.expenseform.ExpenseFormViewModel
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime
import org.koin.androidx.compose.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseFormScreen(
    expenseId: String?,
    onClose: () -> Unit,
    vm: ExpenseFormViewModel = koinViewModel { parametersOf(expenseId) },
) {
    val s by vm.state.collectAsStateWithLifecycle()
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val negative = LocalBudgetColors.current.negative

    LaunchedEffect(s.done) { if (s.done) onClose() }
    LaunchedEffect(Unit) { if (expenseId == null) focus.requestFocus() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (s.isEdit) "Edit expense" else "New expense") },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Cancel") } },
                actions = {
                    TextButton(onClick = vm::save, enabled = !s.isSaving && !s.isLoading, modifier = Modifier.testTag("saveButton")) {
                        Text("Save")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = s.amountText,
                onValueChange = vm::setAmount,
                label = { Text("Amount") },
                placeholder = { Text("0.00") },
                textStyle = TextStyle(fontSize = 32.sp, fontWeight = FontWeight.Bold),
                singleLine = true,
                isError = s.error == DomainError.InvalidAmount,
                supportingText = if (s.error == DomainError.InvalidAmount) {
                    { Text("Enter an amount above zero, with at most 2 decimals.") }
                } else null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).testTag("amountField"),
            )

            Text("Category", style = MaterialTheme.typography.titleSmall)
            s.categories.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { c ->
                        val selected = c == s.category
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp)).clickable { vm.setCategory(c) }.padding(vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Column(
                                Modifier.border(2.dp, if (selected) c.tint else Color.Transparent, RoundedCornerShape(13.dp)),
                            ) { CategoryIcon(c, size = 44.dp) }
                            Text(
                                c.label,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            OutlinedButton(onClick = { pickingDate = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.CalendarToday, contentDescription = null)
                Text("  ${mediumDate(s.date)}")
            }
            if (s.error == DomainError.FutureDate) Text("Expenses can't be in the future.", color = negative)

            OutlinedTextField(
                value = s.note,
                onValueChange = vm::setNote,
                label = { Text("Note (optional)") },
                singleLine = true,
                isError = s.error == DomainError.NoteTooLong,
                supportingText = if (s.error == DomainError.NoteTooLong) { { Text("Keep notes to 80 characters.") } } else null,
                modifier = Modifier.fillMaxWidth(),
            )

            if (s.isEdit) {
                TextButton(onClick = { confirmDelete = true }) { Text("Delete expense", color = negative) }
            }
            Button(onClick = vm::save, enabled = !s.isSaving && !s.isLoading, modifier = Modifier.fillMaxWidth()) { Text("Save") }
        }
    }

    if (pickingDate) {
        val utc = TimeZone.UTC
        val state = rememberDatePickerState(
            initialSelectedDateMillis = s.date.atStartOfDayIn(utc).toEpochMilliseconds(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long) =
                    Instant.fromEpochMilliseconds(utcTimeMillis).toLocalDateTime(utc).date <= s.maxDate
            },
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let {
                        val d: LocalDate = Instant.fromEpochMilliseconds(it).toLocalDateTime(utc).date
                        vm.setDate(d.year, d.month.ordinal + 1, d.day)
                    }
                    pickingDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("Cancel") } },
        ) { DatePicker(state) }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this expense?") },
            confirmButton = { TextButton(onClick = { confirmDelete = false; vm.delete() }) { Text("Delete", color = negative) } },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
    if (s.error == DomainError.NotFound) {
        AlertDialog(
            onDismissRequest = onClose,
            title = { Text("This expense no longer exists.") },
            confirmButton = { TextButton(onClick = onClose) { Text("OK") } },
        )
    }
}
