package com.acme.payroll.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import com.acme.payroll.domain.model.Session
import com.acme.payroll.ui.components.AppScaffold
import com.acme.payroll.ui.components.ConfirmDialog
import com.acme.payroll.ui.navigation.Routes

@Composable
fun MoreScreen(session: Session, onNavigate: (String) -> Unit, onLogout: () -> Unit) {
    var confirmLogout by remember { mutableStateOf(false) }
    AppScaffold(title = "More") { padding ->
        Column(Modifier.padding(padding)) {
            ListItem(
                headlineContent = { Text(session.displayName) },
                supportingContent = { Text("${session.email} · ${session.role.label}") },
                modifier = Modifier.testTag("current_user"),
            )
            HorizontalDivider()
            if (session.role != com.acme.payroll.domain.model.Role.EMPLOYEE) {
                MenuRow("Departments", Icons.Filled.Apartment, "menu_departments") { onNavigate(Routes.DEPARTMENTS) }
            }
            if (session.canSeeReports) {
                MenuRow("Reports", Icons.Filled.BarChart, "menu_reports") { onNavigate(Routes.REPORTS) }
            }
            MenuRow("Settings", Icons.Filled.Settings, "menu_settings") { onNavigate(Routes.SETTINGS) }
            MenuRow("Sign out", Icons.AutoMirrored.Filled.Logout, "menu_logout") { confirmLogout = true }
        }
    }
    if (confirmLogout) {
        ConfirmDialog(
            title = "Sign out?",
            text = "You will need to sign in again to use the app.",
            confirmLabel = "Sign out",
            onConfirm = { confirmLogout = false; onLogout() },
            onDismiss = { confirmLogout = false },
        )
    }
}

@Composable
internal fun MenuRow(title: String, icon: ImageVector, tag: String, supporting: String? = null, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = supporting?.let { { Text(it) } },
        leadingContent = { Icon(icon, contentDescription = null) },
        modifier = Modifier.clickable(onClick = onClick).testTag(tag),
    )
}
