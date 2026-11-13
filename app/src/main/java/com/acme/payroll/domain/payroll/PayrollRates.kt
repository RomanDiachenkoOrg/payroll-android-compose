package com.acme.payroll.domain.payroll

import java.math.BigDecimal

/** A progressive income tax band applied to monthly gross pay. [upTo] is null for the top band. */
data class TaxBand(
    val from: BigDecimal,
    val upTo: BigDecimal?,
    val ratePercent: BigDecimal,
)

data class PayrollRates(
    val taxBands: List<TaxBand>,
    val socialPercent: BigDecimal,
    val pensionPercent: BigDecimal,
    val overtimeMultiplier: BigDecimal = BigDecimal("1.5"),
    val weeklyOvertimeThreshold: BigDecimal = BigDecimal(40),
) {
    companion object {
        val DEFAULT = PayrollRates(
            taxBands = listOf(
                TaxBand(BigDecimal.ZERO, BigDecimal("1000"), BigDecimal.ZERO),
                TaxBand(BigDecimal("1000"), BigDecimal("4000"), BigDecimal("15")),
                TaxBand(BigDecimal("4000"), null, BigDecimal("25")),
            ),
            socialPercent = BigDecimal("5"),
            pensionPercent = BigDecimal("3"),
        )
    }
}
