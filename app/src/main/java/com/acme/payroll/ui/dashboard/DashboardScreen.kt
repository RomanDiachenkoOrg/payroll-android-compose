package com.acme.payroll.ui.dashboard

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.util.Dates

@Composable
fun DashboardScreen(
    session: Session,
    onOpenTimesheets: () -> Unit,
    onOpenPayRun: (Long) -> Unit,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScaffold(title = "Dashboard") { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@AppScaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Hello, ${state.greetingName}", style = MaterialTheme.typography.titleLarge)

            if (session.role == Role.EMPLOYEE) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MetricCard("Last net pay", state.myLastNetCents?.let(Money::formatCents) ?: "-", "metric_last_net", Modifier.weight(1f))
                    MetricCard("Next pay date", Dates.format(state.nextPayDate), "metric_next_pay_date", Modifier.weight(1f))
                }
                MetricCard(
                    "My timesheets",
                    "Open",
                    "metric_my_timesheets",
                    Modifier.fillMaxWidth(),
                    onClick = onOpenTimesheets,
                )
                return@Column
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(state.headcountLabel, state.headcount.toString(), "metric_headcount", Modifier.weight(1f))
                MetricCard(
                    "Pending timesheets",
                    state.pendingTimesheets.toString(),
                    "metric_pending_timesheets",
                    Modifier.weight(1f),
                    onClick = onOpenTimesheets,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(
                    if (state.lastPayrollLabel.isEmpty()) "Last payroll" else "Last payroll (${state.lastPayrollLabel})",
                    state.lastPayrollGrossCents?.let(Money::formatCents) ?: "-",
                    "metric_last_payroll",
                    Modifier.weight(1f),
                )
                MetricCard(
                    "Next pay date",
                    Dates.format(state.nextPayDate),
                    "metric_next_pay_date",
                    Modifier.weight(1f),
                    onClick = state.nextRunId?.takeIf { session.canManagePayRuns }?.let { id -> { onOpenPayRun(id) } },
                )
            }

            if (state.history.isNotEmpty()) {
                SectionCard("Gross payroll by month") {
                    PayrollBarChart(state.history)
                }
            }

            if (state.recentActivity.isNotEmpty()) {
                SectionCard("Recent activity") {
                    state.recentActivity.forEachIndexed { i, entry ->
                        if (i > 0) HorizontalDivider()
                        Column(Modifier.padding(vertical = 4.dp)) {
                            Text("${entry.action} · ${entry.entity}", style = MaterialTheme.typography.labelLarge)
                            Text(entry.details, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${entry.userEmail} · ${Dates.formatTimestamp(entry.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    tag: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Card(
        modifier = modifier
            .testTag(tag)
            .let { if (onClick != null) it.clickable(onClick = onClick) else it },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 2)
            Text(value, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("${tag}_value"))
        }
    }
}

@Composable
private fun PayrollBarChart(bars: List<PayrollBar>) {
    val max = (bars.maxOfOrNull { it.grossCents } ?: 0L).coerceAtLeast(1L)
    val barColor = MaterialTheme.colorScheme.primary
    val description = bars.joinToString { "${it.label} ${Money.formatCents(it.grossCents)}" }
    Column {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(140.dp)
                .semantics { contentDescription = "Gross payroll chart: $description" },
        ) {
            val slot = size.width / bars.size
            val barWidth = slot * 0.55f
            bars.forEachIndexed { i, bar ->
                val h = size.height * (bar.grossCents.toFloat() / max.toFloat())
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(i * slot + (slot - barWidth) / 2, size.height - h),
                    size = Size(barWidth, h),
                    cornerRadius = CornerRadius(6f, 6f),
                )
            }
        }
        Row(Modifier.fillMaxWidth()) {
            bars.forEach {
                Column(Modifier.weight(1f)) {
                    Text(it.label, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$" + (it.grossCents / 100_000).toString() + "k",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
