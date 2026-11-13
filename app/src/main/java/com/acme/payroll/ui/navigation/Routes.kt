package com.acme.payroll.ui.navigation

object Routes {
    const val HOME_GRAPH = "home_graph"
    const val DASHBOARD = "dashboard"

    const val EMPLOYEES_GRAPH = "employees_graph"
    const val EMPLOYEES = "employees"
    const val MY_PROFILE = "profile"
    const val ARG_EMPLOYEE_ID = "employeeId"
    const val EMPLOYEE_DETAIL = "employees/{$ARG_EMPLOYEE_ID}"
    const val EMPLOYEE_EDIT = "employee_edit?$ARG_EMPLOYEE_ID={$ARG_EMPLOYEE_ID}"

    const val TIMESHEETS_GRAPH = "timesheets_graph"
    const val TIMESHEETS = "timesheets"
    const val ARG_TIMESHEET_ID = "timesheetId"
    const val TIMESHEET_EDIT = "timesheet_edit?$ARG_TIMESHEET_ID={$ARG_TIMESHEET_ID}"

    const val PAYRUNS_GRAPH = "payruns_graph"
    const val PAYRUNS = "payruns"
    const val ARG_RUN_ID = "runId"
    const val PAYRUN_DETAIL = "payruns/{$ARG_RUN_ID}"
    const val ARG_PAYSLIP_ID = "payslipId"
    const val PAYSLIP = "payslips/{$ARG_PAYSLIP_ID}"

    const val MORE_GRAPH = "more_graph"
    const val MORE = "more"
    const val DEPARTMENTS = "departments"
    const val REPORTS = "reports"
    const val SETTINGS = "settings"
    const val TAX_SETTINGS = "settings/tax"
    const val USERS = "settings/users"
    const val AUDIT_LOG = "settings/audit"

    fun employeeDetail(id: Long) = "employees/$id"
    fun employeeEdit(id: Long? = null) = if (id == null) "employee_edit" else "employee_edit?$ARG_EMPLOYEE_ID=$id"
    fun timesheetEdit(id: Long? = null) = if (id == null) "timesheet_edit" else "timesheet_edit?$ARG_TIMESHEET_ID=$id"
    fun payRunDetail(id: Long) = "payruns/$id"
    fun payslip(id: Long) = "payslips/$id"
}
