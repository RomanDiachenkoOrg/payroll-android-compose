package com.acme.payroll.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.acme.payroll.domain.model.Session
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.navigation.Routes

@Composable
fun SettingsScreen(
    session: Session,
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    onLogout: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    AppScaffold(title = "Settings", onBack = onBack) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState())) {
            SectionHeader("Preferences")
            ListItem(
                headlineContent = { Text("Remember my email") },
                supportingContent = { Text("Pre-fill the sign-in form on this device") },
                trailingContent = {
                    Switch(
                        checked = settings.rememberEmail,
                        onCheckedChange = { viewModel.setRememberEmail(it) },
                        modifier = Modifier.testTag("setting_remember_email"),
                    )
                },
            )
            ListItem(
                headlineContent = { Text("Dark theme") },
                trailingContent = {
                    Switch(
                        checked = settings.darkTheme,
                        onCheckedChange = { viewModel.setDarkTheme(it) },
                        modifier = Modifier.testTag("setting_dark_theme"),
                    )
                },
            )
            if (session.isAdmin) {
                HorizontalDivider()
                SectionHeader("Administration")
                MenuRow("Tax brackets & rates", Icons.Filled.Percent, "menu_tax", "Income tax bands, social and pension rates") {
                    onNavigate(Routes.TAX_SETTINGS)
                }
                MenuRow("Users", Icons.Filled.SupervisorAccount, "menu_users") { onNavigate(Routes.USERS) }
                MenuRow("Audit log", Icons.Filled.History, "menu_audit") { onNavigate(Routes.AUDIT_LOG) }
            }
            HorizontalDivider()
            MenuRow("Sign out", Icons.AutoMirrored.Filled.Logout, "settings_logout", session.email, onLogout)
            Text(
                "Acme Payroll 1.3.0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}

@Composable
internal fun SectionHeader(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 4.dp),
    )
}
