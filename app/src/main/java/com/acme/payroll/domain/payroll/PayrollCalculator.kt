package com.acme.payroll.domain.payroll

import com.acme.payroll.domain.model.PayType
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode

data class PayInput(
    val payType: PayType,
    val annualSalary: BigDecimal? = null,
    val hourlyRate: BigDecimal? = null,
    /** Total hours per approved week in the period (hourly employees only). */
    val weeklyHours: List<BigDecimal> = emptyList(),
    val pensionOptIn: Boolean = false,
    val otherDeductions: BigDecimal = BigDecimal.ZERO,
)

data class PayBreakdown(
    val regularHours: BigDecimal,
    val overtimeHours: BigDecimal,
    val gross: BigDecimal,
    val incomeTax: BigDecimal,
    val social: BigDecimal,
    val pension: BigDecimal,
    val other: BigDecimal,
    val net: BigDecimal,
) {
    val totalDeductions: BigDecimal get() = incomeTax + social + pension + other
}

/** Monthly payroll calculation. All monetary results are rounded half-up to cents. */
class PayrollCalculator(private val rates: PayrollRates = PayrollRates.DEFAULT) {

    /** Splits a week's hours into regular and overtime hours. */
    fun splitWeek(hours: BigDecimal): Pair<BigDecimal, BigDecimal> {
        val threshold = rates.weeklyOvertimeThreshold
        return if (hours > threshold) threshold to (hours - threshold) else hours to BigDecimal.ZERO
    }

    fun monthlySalary(annualSalary: BigDecimal): BigDecimal =
        annualSalary.divide(MONTHS, 2, RoundingMode.HALF_UP)

    fun incomeTax(gross: BigDecimal): BigDecimal {
        var tax = BigDecimal.ZERO
        for (band in rates.taxBands.sortedBy { it.from }) {
            if (gross <= band.from) break
            val top = band.upTo?.let { gross.min(it) } ?: gross
            val taxable = top - band.from
            tax += taxable.multiply(band.ratePercent).divide(HUNDRED, MathContext.DECIMAL64)
        }
        return Money.round(tax)
    }

    fun percentOf(amount: BigDecimal, percent: BigDecimal): BigDecimal =
        Money.round(amount.multiply(percent).divide(HUNDRED, MathContext.DECIMAL64))

    fun calculate(input: PayInput): PayBreakdown {
        var regular = BigDecimal.ZERO
        var overtime = BigDecimal.ZERO
        val gross = when (input.payType) {
            PayType.SALARIED -> monthlySalary(requireNotNull(input.annualSalary) { "Annual salary missing" })
            PayType.HOURLY -> {
                val rate = requireNotNull(input.hourlyRate) { "Hourly rate missing" }
                input.weeklyHours.forEach { week ->
                    val (r, o) = splitWeek(week)
                    regular += r
                    overtime += o
                }
                Money.round(rate * regular + rate * rates.overtimeMultiplier * overtime)
            }
        }
        val tax = incomeTax(gross)
        val social = percentOf(gross, rates.socialPercent)
        val pension = if (input.pensionOptIn) percentOf(gross, rates.pensionPercent) else BigDecimal.ZERO.setScale(2)
        val other = Money.round(input.otherDeductions)
        val net = gross - tax - social - pension - other
        return PayBreakdown(
            regularHours = regular,
            overtimeHours = overtime,
            gross = gross,
            incomeTax = tax,
            social = social,
            pension = pension,
            other = other,
            net = Money.round(net),
        )
    }

    private companion object {
        val MONTHS = BigDecimal(12)
        val HUNDRED = BigDecimal(100)
    }
}
