package com.acme.payroll.domain

import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.validation.EmployeeField
import com.acme.payroll.domain.validation.EmployeeForm
import com.acme.payroll.domain.validation.EmployeeValidator
import com.acme.payroll.domain.validation.TimesheetValidator
import com.acme.payroll.util.Csv
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

class ValidatorsTest {

    private val today = LocalDate.of(2026, 3, 16)

    private val valid = EmployeeForm(
        firstName = "Jane",
        lastName = "Doe",
        email = "jane.doe@acme.test",
        departmentId = 1,
        payType = PayType.SALARIED,
        amount = "55000",
        hireDate = today,
    )

    @Test
    fun `valid form has no errors`() {
        assertTrue(EmployeeValidator.validate(valid, today).isEmpty())
    }

    @Test
    fun `required fields are reported`() {
        val errors = EmployeeValidator.validate(EmployeeForm(), today)
        assertTrue(
            errors.keys.containsAll(
                listOf(
                    EmployeeField.FIRST_NAME, EmployeeField.LAST_NAME, EmployeeField.EMAIL,
                    EmployeeField.DEPARTMENT, EmployeeField.AMOUNT, EmployeeField.HIRE_DATE,
                )
            )
        )
    }

    @Test
    fun `email format and uniqueness`() {
        assertEquals("Enter a valid email address", EmployeeValidator.validate(valid.copy(email = "jane@"), today)[EmployeeField.EMAIL])
        assertEquals(
            "Another employee already uses this email",
            EmployeeValidator.validate(valid, today, emailTaken = true)[EmployeeField.EMAIL],
        )
    }

    @Test
    fun `salary must be positive`() {
        assertTrue(EmployeeValidator.validate(valid.copy(amount = "0"), today).containsKey(EmployeeField.AMOUNT))
        assertTrue(EmployeeValidator.validate(valid.copy(amount = "-10"), today).containsKey(EmployeeField.AMOUNT))
        assertTrue(EmployeeValidator.validate(valid.copy(amount = "abc"), today).containsKey(EmployeeField.AMOUNT))
    }

    @Test
    fun `hire date next month is rejected`() {
        val errors = EmployeeValidator.validate(valid.copy(hireDate = today.plusDays(30)), today)
        assertEquals("Hire date cannot be in the future", errors[EmployeeField.HIRE_DATE])
    }

    @Test
    fun `timesheet hours must be between 0 and 24`() {
        val errors = TimesheetValidator.validateHours(listOf("8", "25", "-1", "x", "", "7.5", "0"))
        assertEquals(setOf(1, 2, 3), errors.keys)
        assertEquals(BigDecimal("15.5"), TimesheetValidator.total(listOf("8", "7.5", "")))
    }

    @Test
    fun `csv values are escaped`() {
        assertEquals("plain", Csv.escape("plain"))
        assertEquals("\"a,b\"", Csv.escape("a,b"))
        assertEquals("\"say \"\"hi\"\"\"", Csv.escape("say \"hi\""))
        assertEquals("A,B\r\n1,2\r\n", Csv.build(listOf("A", "B"), listOf(listOf("1", 2))))
    }
}
