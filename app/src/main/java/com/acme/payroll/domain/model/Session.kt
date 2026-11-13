package com.acme.payroll.domain.model

/** The signed-in user, persisted in DataStore between launches. */
data class Session(
    val userId: Long,
    val email: String,
    val displayName: String,
    val role: Role,
    val employeeId: Long?,
    val departmentId: Long?,
) {
    val isAdmin: Boolean get() = role == Role.ADMIN
    val isManager: Boolean get() = role == Role.MANAGER
    val canApproveTimesheets: Boolean get() = role == Role.ADMIN || role == Role.MANAGER
    val canSeeReports: Boolean get() = role == Role.ADMIN || role == Role.MANAGER
    val canManageEmployees: Boolean get() = role == Role.ADMIN
    val canManagePayRuns: Boolean get() = role == Role.ADMIN
}
