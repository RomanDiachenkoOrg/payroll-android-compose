package com.acme.payroll.ui.timesheets

import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.model.Session
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.DateField
import com.acme.payroll.ui.components.DropdownField
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.ui.components.TimesheetStatusChip
import com.acme.payroll.util.Dates
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun TimesheetEditScreen(
    session: Session,
    onBack: () -> Unit,
    viewModel: TimesheetEditViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var showReject by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.closeEvents.collect {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            onBack()
        }
    }

    AppScaffold(title = if (state.id == null) "New timesheet" else "Timesheet", onBack = onBack) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@AppScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(state.employeeName, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TimesheetStatusChip(state.status, Modifier.testTag("timesheet_status"))
            }
            if (state.id == null && state.employeeOptions.isNotEmpty()) {
                DropdownField(
                    label = "Employee",
                    options = state.employeeOptions,
                    selected = state.employeeId ?: -1L,
                    onSelected = viewModel::setEmployee,
                    modifier = Modifier.fillMaxWidth().testTag("timesheet_employee"),
                )
            }
            DateField(
                label = "Week starting (Monday)",
                date = state.weekStart,
                onDateSelected = viewModel::setWeek,
                enabled = state.canEdit,
                supportingText = Dates.formatWeek(state.weekStart),
                testTag = "timesheet_week",
            )
            state.rejectReason?.let {
                SectionCard("Rejected by ${state.decidedBy ?: "manager"}") {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("timesheet_reject_reason"))
                }
            }

            SectionCard("Hours") {
                state.hours.forEachIndexed { day, value ->
                    val date = state.weekStart.plusDays(day.toLong())
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.US) + " " + date.format(Dates.shortDisplay),
                            modifier = Modifier.weight(1f),
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { viewModel.setHours(day, it) },
                            enabled = state.canEdit,
                            singleLine = true,
                            isError = state.hourErrors.containsKey(day),
                            supportingText = state.hourErrors[day]?.let { { Text(it) } },
                            label = { Text("Hours") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.width(140.dp).testTag("hours_day_$day"),
                        )
                    }
                }
                Text(
                    "Total: ${state.total.stripTrailingZeros().toPlainString()} h",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("timesheet_total"),
                )
            }

            OutlinedTextField(
                value = state.notes,
                onValueChange = viewModel::setNotes,
                enabled = state.canEdit,
                label = { Text("Notes") },
                modifier = Modifier.fillMaxWidth().testTag("timesheet_notes"),
                minLines = 2,
            )

            state.error?.let {
                Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("timesheet_error"))
            }

            if (state.canEdit) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = viewModel::saveDraft,
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f).testTag("timesheet_save_draft"),
                    ) { Text("Save draft") }
                    Button(
                        onClick = viewModel::submit,
                        enabled = !state.busy && state.canSubmit,
                        modifier = Modifier.weight(1f).testTag("timesheet_submit"),
                    ) { Text("Submit") }
                }
            }
            if (state.canDecide && session.canApproveTimesheets) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(
                        onClick = { showReject = true },
                        enabled = !state.busy,
                        modifier = Modifier.weight(1f).testTag("timesheet_reject"),
                    ) { Text("Reject", color = MaterialTheme.colorScheme.error) }
                    Button(
                        onClick = viewModel::approve,
                        enabled = !state.busy,
                        colors = ButtonDefaults.buttonColors(),
                        modifier = Modifier.weight(1f).testTag("timesheet_approve"),
                    ) { Text("Approve") }
                }
            }
        }
    }

    if (showReject) {
        var reason by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showReject = false },
            title = { Text("Reject timesheet") },
            text = {
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it },
                    label = { Text("Reason") },
                    modifier = Modifier.fillMaxWidth().testTag("reject_reason_input"),
                    minLines = 2,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showReject = false
                        viewModel.reject(reason)
                    },
                    enabled = reason.isNotBlank(),
                    modifier = Modifier.testTag("reject_confirm"),
                ) { Text("Reject") }
            },
            dismissButton = { TextButton(onClick = { showReject = false }) { Text("Cancel") } },
        )
    }
}
