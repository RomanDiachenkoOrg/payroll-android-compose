package com.acme.payroll.ui.settings

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.EmptyState
import com.acme.payroll.ui.components.LoadingBox
import com.acme.payroll.ui.components.SectionCard
import com.acme.payroll.util.Dates

@Composable
fun TaxSettingsScreen(onBack: () -> Unit, viewModel: TaxSettingsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) { viewModel.messages.collect { snackbar.showSnackbar(it) } }

    AppScaffold(title = "Tax brackets & rates", onBack = onBack, snackbarHostState = snackbar) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.padding(padding))
            return@AppScaffold
        }
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionCard("Monthly income tax bands") {
                Text(
                    "Each band taxes only the part of gross pay that falls inside it. Leave the last 'up to' empty.",
                    style = MaterialTheme.typography.bodySmall,
                )
                state.bands.forEachIndexed { i, band ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        NumberField("From", band.from, Modifier.weight(1f), "band_${i}_from") { viewModel.updateBand(i, band.copy(from = it)) }
                        NumberField("Up to", band.upTo, Modifier.weight(1f), "band_${i}_to") { viewModel.updateBand(i, band.copy(upTo = it)) }
                        NumberField("Rate %", band.rate, Modifier.weight(0.8f), "band_${i}_rate") { viewModel.updateBand(i, band.copy(rate = it)) }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = viewModel::addBand, modifier = Modifier.testTag("band_add")) { Text("Add band") }
                    OutlinedButton(onClick = viewModel::removeLastBand, modifier = Modifier.testTag("band_remove")) { Text("Remove last") }
                }
            }
            SectionCard("Contributions") {
                NumberField("Social contribution %", state.socialPercent, Modifier.fillMaxWidth(), "rate_social", viewModel::setSocial)
                NumberField("Pension (opt-in) %", state.pensionPercent, Modifier.fillMaxWidth(), "rate_pension", viewModel::setPension)
            }
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("tax_error")) }
            Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth().testTag("tax_save")) { Text("Save") }
        }
    }
}

@Composable
private fun NumberField(label: String, value: String, modifier: Modifier, tag: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = modifier.testTag(tag),
    )
}

@Composable
fun UsersScreen(onBack: () -> Unit, viewModel: UsersViewModel = hiltViewModel()) {
    val users by viewModel.users.collectAsStateWithLifecycle()
    AppScaffold(title = "Users", onBack = onBack) { padding ->
        LazyColumn(Modifier.padding(padding).testTag("users_list")) {
            items(users, key = { it.id }) { u ->
                ListItem(
                    headlineContent = { Text(u.displayName) },
                    supportingContent = {
                        Text("${u.email} · last sign-in ${u.lastLoginAt?.let(Dates::formatTimestamp) ?: "never"}")
                    },
                    trailingContent = { Text(u.role.label, style = MaterialTheme.typography.labelLarge) },
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
fun AuditLogScreen(onBack: () -> Unit, viewModel: AuditLogViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    AppScaffold(title = "Audit log", onBack = onBack) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("Filter by user, entity or text") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).testTag("audit_search"),
            )
            Row(
                Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(selected = state.action == null, onClick = { viewModel.setAction(null) }, label = { Text("All") })
                state.actions.forEach { a ->
                    FilterChip(
                        selected = state.action == a,
                        onClick = { viewModel.setAction(a) },
                        label = { Text(a) },
                        modifier = Modifier.testTag("audit_action_${a.lowercase()}"),
                    )
                }
            }
            if (state.entries.isEmpty()) {
                EmptyState("No audit entries")
            } else {
                LazyColumn(Modifier.testTag("audit_list")) {
                    items(state.entries, key = { it.id }) { e ->
                        ListItem(
                            overlineContent = { Text("${Dates.formatTimestamp(e.timestamp)} · ${e.userEmail}") },
                            headlineContent = { Text("${e.action} ${e.entity}") },
                            supportingContent = { Text(e.details) },
                        )
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
