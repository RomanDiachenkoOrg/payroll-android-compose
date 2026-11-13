package com.acme.payroll.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.model.Session
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.ConfirmDialog
import com.acme.payroll.ui.components.DateField
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LabeledValue
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.ui.components.StatusChip
import com.acme.payroll.ui.theme.StatusColors
import com.acme.payroll.util.Dates
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun EmployeeDetailScreen(
    session: Session,
    onBack: (() -> Unit)?,
    onEdit: (Long) -> Unit,
    onOpenPayslip: (Long) -> Unit,
    viewModel: EmployeeDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { androidx.compose.material3.SnackbarHostState() }
    var confirmDeactivate by remember { mutableStateOf(false) }
    var showRaise by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val item = state.employee
    AppScaffold(
        title = if (onBack == null) "My profile" else item?.employee?.fullName ?: "Employee",
        onBack = onBack,
        snackbarHostState = snackbar,
        actions = {
            if (session.canManageEmployees && item != null) {
                IconButton(onClick = { onEdit(item.employee.id) }, modifier = Modifier.testTag("employee_edit")) {
                    Icon(Icons.Filled.Edit, contentDescription = "Edit employee")
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            item == null -> EmptyState("Employee not found", Modifier.padding(padding))
            else -> Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                val e = item.employee
                SectionCard(null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(e.fullName, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                        if (e.active) StatusChip("Active", StatusColors.Approved) else StatusChip("Inactive", StatusColors.Draft)
                    }
                    Text("${e.jobTitle} · ${item.department.name}", style = MaterialTheme.typography.bodyMedium)
                    HorizontalDivider()
                    LabeledValue("Email", e.email)
                    LabeledValue("Phone", e.phone.ifBlank { "-" })
                    LabeledValue("Hire date", Dates.format(e.hireDate))
                    LabeledValue("Pay type", e.payType.label)
                    LabeledValue("Current pay", payText(e), emphasize = true)
                    LabeledValue("Pension plan", if (e.pensionOptIn) "Enrolled" else "Not enrolled")
                    LabeledValue("Other deductions", Money.formatCents(e.otherDeductionCents) + " / month")
                }

                SectionCard("Salary history") {
                    if (state.salaryHistory.isEmpty()) Text("No pay changes recorded")
                    state.salaryHistory.forEach { h ->
                        val amount = h.annualSalaryCents?.let { Money.formatCents(it) + " / yr" }
                            ?: h.hourlyRateCents?.let { Money.formatCents(it) + " / h" } ?: "-"
                        LabeledValue("${Dates.format(h.effectiveDate)} · ${h.reason}", amount)
                    }
                    if (session.canManageEmployees && e.active) {
                        OutlinedButton(onClick = { showRaise = true }, modifier = Modifier.testTag("add_raise")) {
                            Text("Add raise")
                        }
                    }
                }

                SectionCard("Payslips") {
                    if (state.payslips.isEmpty()) Text("No payslips yet")
                    state.payslips.forEach { p ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onOpenPayslip(p.payslip.id) }
                                .padding(vertical = 6.dp)
                                .testTag("payslip_row_${p.payslip.id}"),
                        ) {
                            Text(YearMonth.from(p.payRun.periodStart).format(Dates.monthDisplay), modifier = Modifier.weight(1f))
                            Text("Net " + Money.formatCents(p.payslip.netCents))
                        }
                    }
                }

                if (session.canManageEmployees) {
                    if (e.active) {
                        OutlinedButton(
                            onClick = { confirmDeactivate = true },
                            modifier = Modifier.fillMaxWidth().testTag("employee_deactivate"),
                        ) { Text("Deactivate employee", color = MaterialTheme.colorScheme.error) }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.setActive(true) },
                            modifier = Modifier.fillMaxWidth().testTag("employee_reactivate"),
                        ) { Text("Reactivate employee") }
                    }
                }
            }
        }
    }

    if (confirmDeactivate && item != null) {
        ConfirmDialog(
            title = "Deactivate ${item.employee.fullName}?",
            text = "Inactive employees are excluded from future pay runs. Existing payslips are kept.",
            confirmLabel = "Deactivate",
            destructive = true,
            onConfirm = {
                confirmDeactivate = false
                viewModel.setActive(false)
            },
            onDismiss = { confirmDeactivate = false },
        )
    }
    if (showRaise && item != null) {
        RaiseDialog(
            employee = item.employee,
            onSubmit = { amount, date, reason -> viewModel.addRaise(amount, date, reason) },
            onDismiss = { showRaise = false },
        )
    }
}

private fun payText(e: EmployeeEntity): String = when (e.payType) {
    PayType.SALARIED -> Money.formatCents(e.annualSalaryCents ?: 0) + " / yr"
    PayType.HOURLY -> Money.formatCents(e.hourlyRateCents ?: 0) + " / h"
}

@Composable
private fun RaiseDialog(
    employee: EmployeeEntity,
    onSubmit: (String, LocalDate?, String) -> String?,
    onDismiss: () -> Unit,
) {
    var amount by remember { mutableStateOf("") }
    var date by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var reason by remember { mutableStateOf("Annual review") }
    var error by remember { mutableStateOf<String?>(null) }
    val label = if (employee.payType == PayType.SALARIED) "New annual salary" else "New hourly rate"
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add raise") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Current: ${payText(employee)}", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it; error = null },
                    label = { Text(label) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.testTag("raise_amount"),
                )
                DateField("Effective date", date, { date = it }, testTag = "raise_date")
                OutlinedTextField(
                    value = reason,
                    onValueChange = { reason = it; error = null },
                    label = { Text("Reason") },
                    singleLine = true,
                    modifier = Modifier.testTag("raise_reason"),
                )
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    error = onSubmit(amount, date, reason)
                    if (error == null) onDismiss()
                },
                modifier = Modifier.testTag("raise_save"),
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
