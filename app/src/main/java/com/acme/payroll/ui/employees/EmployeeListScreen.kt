package com.acme.payroll.ui.employees

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.acme.payroll.data.local.entity.EmployeeWithDepartment
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.DropdownField
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.StatusChip
import com.acme.payroll.ui.theme.StatusColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmployeeListScreen(
    canEdit: Boolean,
    onOpen: (Long) -> Unit,
    onAdd: () -> Unit,
    viewModel: EmployeeListViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var sortMenu by remember { mutableStateOf(false) }

    AppScaffold(
        title = "Employees",
        actions = {
            Box {
                IconButton(onClick = { sortMenu = true }, modifier = Modifier.testTag("employees_sort")) {
                    Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Sort")
                }
                DropdownMenu(expanded = sortMenu, onDismissRequest = { sortMenu = false }) {
                    EmployeeSort.entries.forEach { sort ->
                        DropdownMenuItem(
                            text = { Text((if (sort == state.filters.sort) "✓ " else "") + sort.label) },
                            onClick = {
                                sortMenu = false
                                viewModel.onSortChange(sort)
                            },
                        )
                    }
                }
            }
        },
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = onAdd, modifier = Modifier.testTag("employees_add")) {
                    Icon(Icons.Filled.Add, contentDescription = "Add employee")
                }
            }
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.filters.query,
                    onValueChange = viewModel::onQueryChange,
                    label = { Text("Search name, email or title") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("employees_search"),
                )
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DropdownField(
                        label = "Department",
                        options = listOf<Pair<Long?, String>>(null to "All departments") + state.departments.map { it.id to it.name },
                        selected = state.filters.departmentId,
                        onSelected = viewModel::onDepartmentChange,
                        modifier = Modifier.weight(1f).testTag("employees_department_filter"),
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusFilter.entries.forEach { status ->
                        FilterChip(
                            selected = state.filters.status == status,
                            onClick = { viewModel.onStatusChange(status) },
                            label = { Text(status.label) },
                            modifier = Modifier.testTag("status_filter_${status.name.lowercase()}"),
                        )
                    }
                }
            }

            when {
                state.loading -> LoadingBox()
                state.items.isEmpty() -> EmptyState("No employees match the current filters")
                else -> {
                    LazyColumn(Modifier.weight(1f).testTag("employees_list")) {
                        items(state.items, key = { it.employee.id }) { item ->
                            EmployeeRow(item, onClick = { onOpen(item.employee.id) })
                            HorizontalDivider()
                        }
                    }
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        OutlinedButton(
                            onClick = viewModel::previousPage,
                            enabled = state.hasPrevious,
                            modifier = Modifier.testTag("page_previous"),
                        ) { Text("Previous") }
                        Text(
                            "Page ${state.filters.page + 1} of ${state.pageCount} · ${state.totalCount} total",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.testTag("page_indicator"),
                        )
                        OutlinedButton(
                            onClick = viewModel::nextPage,
                            enabled = state.hasNext,
                            modifier = Modifier.testTag("page_next"),
                        ) { Text("Next") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmployeeRow(item: EmployeeWithDepartment, onClick: () -> Unit) {
    val e = item.employee
    ListItem(
        headlineContent = { Text(e.fullName) },
        supportingContent = { Text("${e.jobTitle} · ${item.department.name}") },
        trailingContent = {
            if (!e.active) StatusChip("Inactive", StatusColors.Draft)
        },
        modifier = Modifier
            .clickable(onClick = onClick)
            .testTag("employee_row_${e.id}"),
    )
}
