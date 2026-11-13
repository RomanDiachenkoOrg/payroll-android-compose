package com.acme.payroll.ui.payruns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.model.PayRunStatus
import com.acme.payroll.domain.model.Session
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.ConfirmDialog
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LabeledValue
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.PayRunStatusChip
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.util.Dates
import java.time.YearMonth

@Composable
fun PayRunDetailScreen(
    session: Session,
    onBack: () -> Unit,
    onOpenPayslip: (Long) -> Unit,
    viewModel: PayRunDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var confirmPaid by remember { mutableStateOf(false) }
    var confirmApprove by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    val run = state.run
    AppScaffold(
        title = run?.let { YearMonth.from(it.periodStart).format(Dates.monthDisplay) } ?: "Pay run",
        onBack = onBack,
        snackbarHostState = snackbar,
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            run == null -> EmptyState("Pay run not found", Modifier.padding(padding))
            else -> LazyColumn(
                Modifier.padding(padding).testTag("payrun_detail"),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    SectionCard(null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                "${Dates.format(run.periodStart)} - ${Dates.format(run.periodEnd)}",
                                style = MaterialTheme.typography.titleMedium,
                                modifier = Modifier.weight(1f),
                            )
                            PayRunStatusChip(run.status, Modifier.testTag("payrun_status"))
                        }
                        LabeledValue("Pay date", Dates.format(run.payDate))
                        LabeledValue("Payslips", state.lines.size.toString())
                        LabeledValue("Gross", Money.formatCents(state.grossCents))
                        LabeledValue("Income tax", Money.formatCents(state.taxCents))
                        LabeledValue("Other deductions", Money.formatCents(state.deductionsCents))
                        LabeledValue("Net pay", Money.formatCents(state.netCents), emphasize = true)
                        run.approvedBy?.let { LabeledValue("Approved by", it) }
                    }
                }
                if (run.status == PayRunStatus.PAID) {
                    item {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.testTag("payrun_locked")) {
                            Icon(Icons.Filled.Lock, contentDescription = null)
                            Text(
                                "  This pay run has been paid and is locked.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else if (session.canManagePayRuns) {
                    item {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(
                                    onClick = viewModel::calculate,
                                    enabled = state.canCalculate && !state.busy,
                                    modifier = Modifier.weight(1f).testTag("payrun_calculate"),
                                ) { Text(if (run.status == PayRunStatus.DRAFT) "Calculate" else "Recalculate") }
                                OutlinedButton(
                                    onClick = { confirmApprove = true },
                                    enabled = state.canApprove && !state.busy,
                                    modifier = Modifier.weight(1f).testTag("payrun_approve"),
                                ) { Text("Approve") }
                            }
                            Button(
                                onClick = { confirmPaid = true },
                                enabled = state.canMarkPaid && !state.busy,
                                modifier = Modifier.fillMaxWidth().testTag("payrun_mark_paid"),
                            ) { Text("Mark as paid") }
                        }
                    }
                }
                item { Text("Lines", style = MaterialTheme.typography.titleMedium) }
                if (state.lines.isEmpty()) {
                    item { Text("Not calculated yet", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                }
                items(state.lines, key = { it.payslip.id }) { line ->
                    ListItem(
                        headlineContent = { Text(line.employee.fullName) },
                        supportingContent = {
                            Text("Gross ${Money.formatCents(line.payslip.grossCents)} · Tax ${Money.formatCents(line.payslip.taxCents)}")
                        },
                        trailingContent = { Text(Money.formatCents(line.payslip.netCents), style = MaterialTheme.typography.titleSmall) },
                        modifier = Modifier
                            .clickable { onOpenPayslip(line.payslip.id) }
                            .testTag("payrun_line_${line.employee.id}"),
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (confirmApprove) {
        ConfirmDialog(
            title = "Approve pay run?",
            text = "Approved pay runs can no longer be recalculated.",
            confirmLabel = "Approve",
            onConfirm = { confirmApprove = false; viewModel.approve() },
            onDismiss = { confirmApprove = false },
        )
    }
    if (confirmPaid) {
        ConfirmDialog(
            title = "Mark as paid?",
            text = "The pay run will be locked and payslips released to employees. This cannot be undone.",
            confirmLabel = "Mark paid",
            onConfirm = { confirmPaid = false; viewModel.markPaid() },
            onDismiss = { confirmPaid = false },
        )
    }
}
