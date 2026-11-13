package com.acme.payroll.domain.model

enum class Role(val label: String) {
    ADMIN("Admin"),
    MANAGER("Manager"),
    EMPLOYEE("Employee");
}

enum class PayType(val label: String) {
    SALARIED("Salaried (monthly)"),
    HOURLY("Hourly");
}

enum class TimesheetStatus(val label: String) {
    DRAFT("Draft"),
    SUBMITTED("Submitted"),
    APPROVED("Approved"),
    REJECTED("Rejected");
}

enum class PayRunStatus(val label: String) {
    DRAFT("Draft"),
    CALCULATED("Calculated"),
    APPROVED("Approved"),
    PAID("Paid");

    val isLocked: Boolean get() = this == PAID
}

enum class DeductionKind { FIXED, PERCENT }
