package com.acme.payroll.ui.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.LabeledValue
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.util.Dates
import com.acme.payroll.util.Share
import java.time.YearMonth

@Composable
fun ReportsScreen(onBack: () -> Unit, viewModel: ReportsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AppScaffold(
        title = "Payroll cost report",
        onBack = onBack,
        actions = {
            IconButton(
                onClick = {
                    Share.shareCsv(
                        context,
                        "payroll_cost_${state.from}_${state.to}.csv",
                        viewModel.csv(),
                        "Payroll cost by department ${state.from} - ${state.to}",
                    )
                },
                enabled = state.rows.isNotEmpty(),
                modifier = Modifier.testTag("report_export_csv"),
            ) { Icon(Icons.Filled.Share, contentDescription = "Export CSV") }
        },
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            MonthStepper("From", state.from, viewModel::setFrom, "report_from")
            MonthStepper("To", state.to, viewModel::setTo, "report_to")
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            if (state.loading) {
                LoadingBox()
            } else if (state.rows.isEmpty()) {
                Text("No approved or paid pay runs in this period.", modifier = Modifier.testTag("report_empty"))
            } else {
                state.rows.forEach { row ->
                    SectionCard(row.departmentName, Modifier.testTag("report_row_${row.departmentName.lowercase()}")) {
                        LabeledValue("Headcount", row.headcount.toString())
                        LabeledValue("Gross", Money.formatCents(row.grossCents))
                        LabeledValue("Income tax", Money.formatCents(row.taxCents))
                        LabeledValue("Social", Money.formatCents(row.socialCents))
                        LabeledValue("Pension", Money.formatCents(row.pensionCents))
                        LabeledValue("Net", Money.formatCents(row.netCents))
                    }
                }
                SectionCard("Total") {
                    LabeledValue("Gross", Money.formatCents(state.totalGross), Modifier.testTag("report_total_gross"))
                    LabeledValue("Net", Money.formatCents(state.totalNet))
                    HorizontalDivider()
                    LabeledValue("Employer cost (gross + social)", Money.formatCents(state.totalEmployerCost), emphasize = true)
                }
            }
        }
    }
}

@Composable
private fun MonthStepper(label: String, month: YearMonth, onChange: (YearMonth) -> Unit, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
        IconButton(onClick = { onChange(month.minusMonths(1)) }, modifier = Modifier.testTag("${tag}_prev")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "$label previous month")
        }
        Text(month.format(Dates.monthDisplay), modifier = Modifier.testTag(tag))
        IconButton(onClick = { onChange(month.plusMonths(1)) }, modifier = Modifier.testTag("${tag}_next")) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "$label next month")
        }
    }
}
