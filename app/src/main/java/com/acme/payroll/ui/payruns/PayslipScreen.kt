package com.acme.payroll.ui.payruns

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.data.local.entity.PayslipDetails
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LabeledValue
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.PayRunStatusChip
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.util.Dates
import com.acme.payroll.util.Share
import java.time.YearMonth

@Composable
fun PayslipScreen(
    onBack: () -> Unit,
    viewModel: PayslipViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var printView by rememberSaveable { mutableStateOf(false) }
    val details = state.details

    AppScaffold(
        title = "Payslip",
        onBack = onBack,
        actions = {
            if (details != null) {
                IconButton(onClick = { printView = !printView }, modifier = Modifier.testTag("payslip_print_view")) {
                    Icon(Icons.Filled.Print, contentDescription = if (printView) "Standard view" else "Print-friendly view")
                }
                IconButton(
                    onClick = {
                        Share.shareCsv(
                            context,
                            PayslipViewModel.fileName(details),
                            PayslipViewModel.toCsv(details),
                            "Payslip ${details.employee.fullName}",
                        )
                    },
                    modifier = Modifier.testTag("payslip_share_csv"),
                ) {
                    Icon(Icons.Filled.Share, contentDescription = "Share as CSV")
                }
            }
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            details == null -> EmptyState("Payslip not found", Modifier.padding(padding))
            printView -> PrintFriendlyPayslip(details, Modifier.padding(padding))
            else -> PayslipBreakdown(details, Modifier.padding(padding))
        }
    }
}

@Composable
private fun PayslipBreakdown(d: PayslipDetails, modifier: Modifier) {
    val p = d.payslip
    Column(
        modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SectionCard(null) {
            Text(d.employee.fullName, style = MaterialTheme.typography.titleLarge)
            Text(d.employee.jobTitle, style = MaterialTheme.typography.bodyMedium)
            LabeledValue("Period", YearMonth.from(d.payRun.periodStart).format(Dates.monthDisplay))
            LabeledValue("Pay date", Dates.format(d.payRun.payDate))
            LabeledValue("Status", "")
            PayRunStatusChip(d.payRun.status)
        }
        if (p.regularHours.signum() > 0 || p.overtimeHours.signum() > 0) {
            SectionCard("Hours") {
                LabeledValue("Regular", p.regularHours.stripTrailingZeros().toPlainString() + " h")
                LabeledValue("Overtime (1.5x)", p.overtimeHours.stripTrailingZeros().toPlainString() + " h")
            }
        }
        SectionCard("Earnings") {
            LabeledValue("Gross pay", Money.formatCents(p.grossCents), Modifier.testTag("payslip_gross"))
        }
        SectionCard("Deductions") {
            LabeledValue("Income tax", Money.formatCents(p.taxCents), Modifier.testTag("payslip_tax"))
            LabeledValue("Social contribution", Money.formatCents(p.socialCents), Modifier.testTag("payslip_social"))
            LabeledValue("Pension", Money.formatCents(p.pensionCents), Modifier.testTag("payslip_pension"))
            LabeledValue("Other", Money.formatCents(p.otherCents), Modifier.testTag("payslip_other"))
            HorizontalDivider()
            LabeledValue("Net pay", Money.formatCents(p.netCents), Modifier.testTag("payslip_net"), emphasize = true)
        }
    }
}

@Composable
private fun PrintFriendlyPayslip(d: PayslipDetails, modifier: Modifier) {
    val p = d.payslip
    val lines = buildString {
        appendLine("ACME CORP - PAYSLIP")
        appendLine("=".repeat(36))
        appendLine("Employee : ${d.employee.fullName}")
        appendLine("Email    : ${d.employee.email}")
        appendLine("Period   : ${d.payRun.periodStart} to ${d.payRun.periodEnd}")
        appendLine("Pay date : ${d.payRun.payDate}")
        appendLine("-".repeat(36))
        fun row(label: String, cents: Long) = appendLine(label.padEnd(20) + Money.formatCents(cents).padStart(16))
        row("Gross", p.grossCents)
        row("Income tax", -p.taxCents)
        row("Social", -p.socialCents)
        row("Pension", -p.pensionCents)
        row("Other", -p.otherCents)
        appendLine("-".repeat(36))
        row("NET PAY", p.netCents)
    }
    Column(
        modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
            .testTag("payslip_print"),
    ) {
        Text(lines, fontFamily = FontFamily.Monospace, color = Color.Black, style = MaterialTheme.typography.bodySmall)
    }
}
