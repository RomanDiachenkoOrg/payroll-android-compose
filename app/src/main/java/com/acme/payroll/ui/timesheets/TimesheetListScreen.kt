package com.acme.payroll.ui.timesheets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import com.acme.payroll.domain.model.TimesheetStatus
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.TimesheetStatusChip
import com.acme.payroll.util.Dates

@Composable
fun TimesheetListScreen(
    session: Session,
    onOpen: (Long) -> Unit,
    onCreate: () -> Unit,
    viewModel: TimesheetListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val canCreate = session.role != Role.MANAGER

    AppScaffold(
        title = if (session.role == Role.EMPLOYEE) "My timesheets" else "Timesheets",
        floatingActionButton = {
            if (canCreate) {
                ExtendedFloatingActionButton(
                    onClick = onCreate,
                    icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                    text = { Text("New timesheet") },
                    modifier = Modifier.testTag("timesheet_new"),
                )
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = state.statusFilter == null,
                    onClick = { viewModel.setFilter(null) },
                    label = { Text("All") },
                    modifier = Modifier.testTag("timesheet_filter_all"),
                )
                TimesheetStatus.entries.forEach { status ->
                    val count = state.counts[status] ?: 0
                    FilterChip(
                        selected = state.statusFilter == status,
                        onClick = { viewModel.setFilter(status) },
                        label = { Text("${status.label} ($count)") },
                        modifier = Modifier.testTag("timesheet_filter_${status.name.lowercase()}"),
                    )
                }
            }
            when {
                state.loading -> LoadingBox()
                state.items.isEmpty() -> EmptyState("No timesheets")
                else -> LazyColumn(Modifier.testTag("timesheet_list")) {
                    items(state.items, key = { it.timesheet.id }) { t ->
                        ListItem(
                            headlineContent = { Text(t.employee.fullName) },
                            supportingContent = {
                                Text("Week of ${Dates.formatWeek(t.timesheet.weekStart)} · ${t.totalHours.stripTrailingZeros().toPlainString()} h")
                            },
                            trailingContent = { TimesheetStatusChip(t.timesheet.status) },
                            modifier = Modifier
                                .clickable { onOpen(t.timesheet.id) }
                                .testTag("timesheet_row_${t.timesheet.id}"),
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
