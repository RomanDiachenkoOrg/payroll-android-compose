package com.acme.payroll.domain.payroll

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * Amounts are stored as whole cents (Long) in the database and handled as
 * BigDecimal in calculations.
 */
object Money {
    private val HUNDRED = BigDecimal(100)

    fun fromCents(cents: Long): BigDecimal = BigDecimal(cents).divide(HUNDRED, 2, RoundingMode.UNNECESSARY)

    fun toCents(amount: BigDecimal): Long = amount.setScale(2, RoundingMode.HALF_UP).multiply(HUNDRED).longValueExact()

    fun round(amount: BigDecimal): BigDecimal = amount.setScale(2, RoundingMode.HALF_UP)

    fun parse(text: String): BigDecimal? =
        text.trim().replace(",", "").replace("$", "").takeIf { it.isNotEmpty() }?.toBigDecimalOrNull()

    private val usd: NumberFormat
        get() = NumberFormat.getCurrencyInstance(Locale.US)

    fun format(amount: BigDecimal): String = usd.format(amount)

    fun formatCents(cents: Long): String = format(fromCents(cents))

    /** Plain "1234.50" formatting used in CSV exports. */
    fun plain(cents: Long): String = fromCents(cents).toPlainString()
}
