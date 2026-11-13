package com.acme.payroll.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session

data class TopLevelDestination(
    val graphRoute: String,
    val label: String,
    val icon: ImageVector,
    val tag: String,
)

fun topLevelDestinationsFor(session: Session): List<TopLevelDestination> = buildList {
    add(TopLevelDestination(Routes.HOME_GRAPH, "Home", Icons.Filled.Dashboard, "nav_home"))
    if (session.role == Role.EMPLOYEE) {
        add(TopLevelDestination(Routes.EMPLOYEES_GRAPH, "Profile", Icons.Filled.AccountCircle, "nav_profile"))
    } else {
        add(TopLevelDestination(Routes.EMPLOYEES_GRAPH, "Employees", Icons.Filled.People, "nav_employees"))
    }
    add(TopLevelDestination(Routes.TIMESHEETS_GRAPH, "Timesheets", Icons.Filled.Schedule, "nav_timesheets"))
    if (session.canManagePayRuns) {
        add(TopLevelDestination(Routes.PAYRUNS_GRAPH, "Pay runs", Icons.Filled.Payments, "nav_payruns"))
    }
    add(TopLevelDestination(Routes.MORE_GRAPH, "More", Icons.Filled.MoreHoriz, "nav_more"))
}

@Composable
fun MainScaffold(session: Session, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val destinations = topLevelDestinationsFor(session)
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            NavigationBar(modifier = Modifier.testTag("bottom_nav")) {
                destinations.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.graphRoute } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.graphRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = null) },
                        label = { Text(dest.label) },
                        modifier = Modifier.testTag(dest.tag),
                    )
                }
            }
        },
    ) { padding ->
        PayrollNavHost(
            navController = navController,
            session = session,
            onLogout = onLogout,
            modifier = Modifier.fillMaxSize().padding(padding),
        )
    }
}
