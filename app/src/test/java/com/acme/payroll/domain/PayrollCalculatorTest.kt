package com.acme.payroll.domain

import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.payroll.PayInput
import com.acme.payroll.domain.payroll.PayrollCalculator
import com.acme.payroll.domain.payroll.PayrollRates
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal

class PayrollCalculatorTest {

    private val calculator = PayrollCalculator(PayrollRates.DEFAULT)

    private fun bd(v: String) = BigDecimal(v)

    @Test
    fun `salaried gross is annual salary divided by twelve`() {
        assertEquals(bd("5000.00"), calculator.monthlySalary(bd("60000")))
        assertEquals(bd("833.34"), calculator.monthlySalary(bd("10000.10")))
    }

    @Test
    fun `income tax is progressive`() {
        assertEquals(bd("0.00"), calculator.incomeTax(bd("900.00")))
        assertEquals(bd("0.00"), calculator.incomeTax(bd("1000.00")))
        assertEquals(bd("150.00"), calculator.incomeTax(bd("2000.00")))
        assertEquals(bd("450.00"), calculator.incomeTax(bd("4000.00")))
        assertEquals(bd("700.00"), calculator.incomeTax(bd("5000.00")))
    }

    @Test
    fun `full salaried breakdown with pension and other deductions`() {
        val result = calculator.calculate(
            PayInput(
                payType = PayType.SALARIED,
                annualSalary = bd("60000"),
                pensionOptIn = true,
                otherDeductions = bd("120"),
            )
        )
        assertEquals(bd("5000.00"), result.gross)
        assertEquals(bd("700.00"), result.incomeTax)
        assertEquals(bd("250.00"), result.social)
        assertEquals(bd("150.00"), result.pension)
        assertEquals(bd("120.00"), result.other)
        assertEquals(bd("3780.00"), result.net)
    }

    @Test
    fun `no pension when employee did not opt in`() {
        val result = calculator.calculate(PayInput(payType = PayType.SALARIED, annualSalary = bd("36000")))
        assertEquals(bd("0.00"), result.pension)
        assertEquals(bd("3000.00"), result.gross)
        assertEquals(bd("300.00"), result.incomeTax)
        assertEquals(bd("2550.00"), result.net)
    }

    @Test
    fun `hourly overtime is paid at one and a half above forty hours a week`() {
        val result = calculator.calculate(
            PayInput(
                payType = PayType.HOURLY,
                hourlyRate = bd("20"),
                weeklyHours = listOf(bd("45"), bd("40"), bd("38")),
            )
        )
        assertEquals(bd("118"), result.regularHours)
        assertEquals(bd("5"), result.overtimeHours)
        assertEquals(bd("2510.00"), result.gross)
        assertEquals(bd("226.50"), result.incomeTax)
        assertEquals(bd("125.50"), result.social)
        assertEquals(bd("2158.00"), result.net)
    }

    @Test
    fun `exactly forty hours has no overtime`() {
        val (regular, overtime) = calculator.splitWeek(bd("40"))
        assertEquals(bd("40"), regular)
        assertEquals(BigDecimal.ZERO, overtime)
    }
}
