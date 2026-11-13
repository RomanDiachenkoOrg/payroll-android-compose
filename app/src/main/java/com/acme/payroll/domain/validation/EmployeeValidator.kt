package com.acme.payroll.domain.validation

import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.payroll.Money
import java.math.BigDecimal
import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class EmployeeForm(
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val phone: String = "",
    val jobTitle: String = "",
    val departmentId: Long? = null,
    val payType: PayType = PayType.SALARIED,
    val amount: String = "",
    val hireDate: LocalDate? = null,
    val pensionOptIn: Boolean = false,
    val otherDeduction: String = "0",
)

enum class EmployeeField { FIRST_NAME, LAST_NAME, EMAIL, DEPARTMENT, AMOUNT, HIRE_DATE, OTHER_DEDUCTION }

object EmployeeValidator {
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    fun isValidEmail(email: String): Boolean = EMAIL_REGEX.matches(email.trim())

    /**
     * Returns a map of field -> message. Empty map means the form is valid.
     * [emailTaken] is evaluated by the caller against the database.
     */
    fun validate(form: EmployeeForm, today: LocalDate, emailTaken: Boolean = false): Map<EmployeeField, String> {
        val errors = linkedMapOf<EmployeeField, String>()
        if (form.firstName.isBlank()) errors[EmployeeField.FIRST_NAME] = "First name is required"
        if (form.lastName.isBlank()) errors[EmployeeField.LAST_NAME] = "Last name is required"

        when {
            form.email.isBlank() -> errors[EmployeeField.EMAIL] = "Email is required"
            !isValidEmail(form.email) -> errors[EmployeeField.EMAIL] = "Enter a valid email address"
            emailTaken -> errors[EmployeeField.EMAIL] = "Another employee already uses this email"
        }

        if (form.departmentId == null) errors[EmployeeField.DEPARTMENT] = "Select a department"

        val amount = Money.parse(form.amount)
        val label = if (form.payType == PayType.SALARIED) "Annual salary" else "Hourly rate"
        when {
            form.amount.isBlank() -> errors[EmployeeField.AMOUNT] = "$label is required"
            amount == null -> errors[EmployeeField.AMOUNT] = "$label must be a number"
            amount <= BigDecimal.ZERO -> errors[EmployeeField.AMOUNT] = "$label must be greater than 0"
        }

        val hire = form.hireDate
        if (hire == null) {
            errors[EmployeeField.HIRE_DATE] = "Hire date is required"
        } else if (ChronoUnit.DAYS.between(today, hire) > 1) {
            errors[EmployeeField.HIRE_DATE] = "Hire date cannot be in the future"
        }

        val other = Money.parse(form.otherDeduction.ifBlank { "0" })
        if (other == null || other < BigDecimal.ZERO) {
            errors[EmployeeField.OTHER_DEDUCTION] = "Deduction must be 0 or more"
        }
        return errors
    }
}
