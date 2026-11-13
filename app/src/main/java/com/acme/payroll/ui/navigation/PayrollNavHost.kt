package com.acme.payroll.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navigation
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import com.acme.payroll.ui.dashboard.DashboardScreen
import com.acme.payroll.ui.departments.DepartmentsScreen
import com.acme.payroll.ui.employees.EmployeeDetailScreen
import com.acme.payroll.ui.employees.EmployeeEditScreen
import com.acme.payroll.ui.employees.EmployeeListScreen
import com.acme.payroll.ui.payruns.PayRunDetailScreen
import com.acme.payroll.ui.payruns.PayRunListScreen
import com.acme.payroll.ui.payruns.PayslipScreen
import com.acme.payroll.ui.reports.ReportsScreen
import com.acme.payroll.ui.settings.AuditLogScreen
import com.acme.payroll.ui.settings.MoreScreen
import com.acme.payroll.ui.settings.SettingsScreen
import com.acme.payroll.ui.settings.TaxSettingsScreen
import com.acme.payroll.ui.settings.UsersScreen
import com.acme.payroll.ui.timesheets.TimesheetEditScreen
import com.acme.payroll.ui.timesheets.TimesheetListScreen

@Composable
fun PayrollNavHost(
    navController: NavHostController,
    session: Session,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(navController = navController, startDestination = Routes.HOME_GRAPH, modifier = modifier) {
        navigation(startDestination = Routes.DASHBOARD, route = Routes.HOME_GRAPH) {
            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    session = session,
                    onOpenTimesheets = {
                        navController.navigate(Routes.TIMESHEETS_GRAPH) { launchSingleTop = true }
                    },
                    onOpenPayRun = { navController.navigate(Routes.payRunDetail(it)) },
                )
            }
        }
        employeesGraph(navController, session)
        timesheetsGraph(navController, session)
        payRunsGraph(navController, session)
        moreGraph(navController, session, onLogout)
    }
}

private fun NavGraphBuilder.employeesGraph(navController: NavHostController, session: Session) {
    val start = if (session.role == Role.EMPLOYEE) Routes.MY_PROFILE else Routes.EMPLOYEES
    navigation(startDestination = start, route = Routes.EMPLOYEES_GRAPH) {
        composable(Routes.EMPLOYEES) {
            EmployeeListScreen(
                canEdit = session.canManageEmployees,
                onOpen = { navController.navigate(Routes.employeeDetail(it)) },
                onAdd = { navController.navigate(Routes.employeeEdit()) },
            )
        }
        composable(Routes.MY_PROFILE) {
            EmployeeDetailScreen(
                session = session,
                onBack = null,
                onEdit = {},
                onOpenPayslip = { navController.navigate(Routes.payslip(it)) },
            )
        }
        composable(
            Routes.EMPLOYEE_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_EMPLOYEE_ID) { type = NavType.LongType }),
        ) {
            EmployeeDetailScreen(
                session = session,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate(Routes.employeeEdit(it)) },
                onOpenPayslip = { navController.navigate(Routes.payslip(it)) },
            )
        }
        composable(
            Routes.EMPLOYEE_EDIT,
            arguments = listOf(navArgument(Routes.ARG_EMPLOYEE_ID) { type = NavType.LongType; defaultValue = 0L }),
        ) {
            EmployeeEditScreen(
                onBack = { navController.popBackStack() },
                onSaved = { id, created ->
                    navController.popBackStack()
                    if (created) navController.navigate(Routes.employeeDetail(id))
                },
            )
        }
    }
}

private fun NavGraphBuilder.timesheetsGraph(navController: NavHostController, session: Session) {
    navigation(startDestination = Routes.TIMESHEETS, route = Routes.TIMESHEETS_GRAPH) {
        composable(Routes.TIMESHEETS) {
            TimesheetListScreen(
                session = session,
                onOpen = { navController.navigate(Routes.timesheetEdit(it)) },
                onCreate = { navController.navigate(Routes.timesheetEdit()) },
            )
        }
        composable(
            Routes.TIMESHEET_EDIT,
            arguments = listOf(navArgument(Routes.ARG_TIMESHEET_ID) { type = NavType.LongType; defaultValue = 0L }),
        ) {
            TimesheetEditScreen(session = session, onBack = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.payRunsGraph(navController: NavHostController, session: Session) {
    navigation(startDestination = Routes.PAYRUNS, route = Routes.PAYRUNS_GRAPH) {
        composable(Routes.PAYRUNS) {
            PayRunListScreen(
                canManage = session.canManagePayRuns,
                onOpen = { navController.navigate(Routes.payRunDetail(it)) },
            )
        }
        composable(
            Routes.PAYRUN_DETAIL,
            arguments = listOf(navArgument(Routes.ARG_RUN_ID) { type = NavType.LongType }),
        ) {
            PayRunDetailScreen(
                session = session,
                onBack = { navController.popBackStack() },
                onOpenPayslip = { navController.navigate(Routes.payslip(it)) },
            )
        }
        composable(
            Routes.PAYSLIP,
            arguments = listOf(navArgument(Routes.ARG_PAYSLIP_ID) { type = NavType.LongType }),
        ) {
            PayslipScreen(onBack = { navController.popBackStack() })
        }
    }
}

private fun NavGraphBuilder.moreGraph(navController: NavHostController, session: Session, onLogout: () -> Unit) {
    navigation(startDestination = Routes.MORE, route = Routes.MORE_GRAPH) {
        composable(Routes.MORE) {
            MoreScreen(session = session, onNavigate = { navController.navigate(it) }, onLogout = onLogout)
        }
        composable(Routes.DEPARTMENTS) {
            DepartmentsScreen(canEdit = session.isAdmin, onBack = { navController.popBackStack() })
        }
        composable(Routes.REPORTS) {
            ReportsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                session = session,
                onBack = { navController.popBackStack() },
                onNavigate = { navController.navigate(it) },
                onLogout = onLogout,
            )
        }
        composable(Routes.TAX_SETTINGS) { TaxSettingsScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.USERS) { UsersScreen(onBack = { navController.popBackStack() }) }
        composable(Routes.AUDIT_LOG) { AuditLogScreen(onBack = { navController.popBackStack() }) }
    }
}
