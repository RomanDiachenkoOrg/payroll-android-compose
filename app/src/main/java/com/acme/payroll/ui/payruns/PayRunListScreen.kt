package com.acme.payroll.ui.payruns

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.PayRunStatusChip
import com.acme.payroll.util.Dates
import java.time.YearMonth

@Composable
fun PayRunListScreen(
    canManage: Boolean,
    onOpen: (Long) -> Unit,
    viewModel: PayRunListViewModel = hiltViewModel(),
) {
    val runs by viewModel.runs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var createMonth by remember { mutableStateOf<YearMonth?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is PayRunListEvent.Created -> onOpen(event.id)
                is PayRunListEvent.Error -> snackbar.showSnackbar(event.message)
            }
        }
    }

    AppScaffold(
        title = "Pay runs",
        snackbarHostState = snackbar,
        floatingActionButton = {
            if (canManage) {
                FloatingActionButton(
                    onClick = { createMonth = viewModel.suggestedMonth() },
                    modifier = Modifier.testTag("payrun_new"),
                ) { Icon(Icons.Filled.Add, contentDescription = "New pay run") }
            }
        },
    ) { padding ->
        val list = runs
        when {
            list == null -> LoadingBox(Modifier.padding(padding))
            list.isEmpty() -> EmptyState("No pay runs yet", Modifier.padding(padding))
            else -> LazyColumn(Modifier.padding(padding).testTag("payrun_list")) {
                items(list, key = { it.run.id }) { summary ->
                    val run = summary.run
                    ListItem(
                        headlineContent = { Text(YearMonth.from(run.periodStart).format(Dates.monthDisplay)) },
                        supportingContent = {
                            Text(
                                "Pay date ${Dates.format(run.payDate)} · ${summary.employeeCount} payslips · " +
                                    "Gross ${Money.formatCents(summary.grossCents)}",
                            )
                        },
                        trailingContent = { PayRunStatusChip(run.status) },
                        modifier = Modifier.clickable { onOpen(run.id) }.testTag("payrun_row_${run.id}"),
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    createMonth?.let { month ->
        AlertDialog(
            onDismissRequest = { createMonth = null },
            title = { Text("New pay run") },
            text = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    IconButton(onClick = { createMonth = month.minusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous month")
                    }
                    Text(
                        month.format(Dates.monthDisplay),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.testTag("payrun_new_month"),
                    )
                    IconButton(onClick = { createMonth = month.plusMonths(1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next month")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        createMonth = null
                        viewModel.create(month)
                    },
                    modifier = Modifier.testTag("payrun_new_confirm"),
                ) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { createMonth = null }) { Text("Cancel") } },
        )
    }
}
