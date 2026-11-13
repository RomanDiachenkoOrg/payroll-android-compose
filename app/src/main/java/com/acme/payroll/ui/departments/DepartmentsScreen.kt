package com.acme.payroll.ui.departments

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.data.local.entity.DepartmentSummary
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.ConfirmDialog
import com.acme.payroll.ui.components.LoadingBox

@Composable
fun DepartmentsScreen(
    canEdit: Boolean,
    onBack: () -> Unit,
    viewModel: DepartmentsViewModel = hiltViewModel(),
) {
    val departments by viewModel.departments.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editing by remember { mutableStateOf<DepartmentSummary?>(null) }
    var creating by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<DepartmentSummary?>(null) }

    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    AppScaffold(
        title = "Departments",
        onBack = onBack,
        snackbarHostState = snackbar,
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(onClick = { creating = true }, modifier = Modifier.testTag("department_add")) {
                    Icon(Icons.Filled.Add, contentDescription = "Add department")
                }
            }
        },
    ) { padding ->
        val list = departments
        if (list == null) {
            LoadingBox(Modifier.padding(padding))
        } else {
            LazyColumn(Modifier.padding(padding).testTag("department_list")) {
                items(list, key = { it.id }) { d ->
                    ListItem(
                        headlineContent = { Text("${d.name} (${d.code})") },
                        supportingContent = {
                            Text("Cost center ${d.costCenter} · ${d.activeCount} active / ${d.totalCount} total")
                        },
                        trailingContent = {
                            if (canEdit) {
                                Row {
                                    IconButton(onClick = { editing = d }, modifier = Modifier.testTag("department_edit_${d.id}")) {
                                        Icon(Icons.Filled.Edit, contentDescription = "Edit ${d.name}")
                                    }
                                    IconButton(onClick = { deleting = d }, modifier = Modifier.testTag("department_delete_${d.id}")) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete ${d.name}")
                                    }
                                }
                            }
                        },
                        modifier = Modifier.testTag("department_row_${d.id}"),
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (creating || editing != null) {
        DepartmentDialog(
            initial = editing,
            onDismiss = { creating = false; editing = null },
            onSave = { name, code, cc, onError ->
                viewModel.save(editing?.id ?: 0L, name, code, cc) { error ->
                    if (error == null) {
                        creating = false
                        editing = null
                    } else onError(error)
                }
            },
        )
    }
    deleting?.let { d ->
        ConfirmDialog(
            title = "Delete ${d.name}?",
            text = "This cannot be undone.",
            confirmLabel = "Delete",
            destructive = true,
            onConfirm = {
                deleting = null
                viewModel.delete(d.id)
            },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun DepartmentDialog(
    initial: DepartmentSummary?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, (String) -> Unit) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var code by remember { mutableStateOf(initial?.code.orEmpty()) }
    var costCenter by remember { mutableStateOf(initial?.costCenter.orEmpty()) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "New department" else "Edit department") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it; error = null }, label = { Text("Name") }, singleLine = true, modifier = Modifier.testTag("department_name"))
                OutlinedTextField(code, { code = it; error = null }, label = { Text("Code") }, singleLine = true, modifier = Modifier.testTag("department_code"))
                OutlinedTextField(costCenter, { costCenter = it }, label = { Text("Cost center") }, singleLine = true, modifier = Modifier.testTag("department_cost_center"))
                error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("department_error")) }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, code, costCenter) { error = it } }, modifier = Modifier.testTag("department_save")) {
                Text("Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
