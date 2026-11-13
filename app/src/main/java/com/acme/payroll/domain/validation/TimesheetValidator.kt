package com.acme.payroll.domain.validation

import java.math.BigDecimal

object TimesheetValidator {
    private val MAX_DAY = BigDecimal(24)

    /** Returns an error per day index (0 = Monday), or an empty map when all hours are valid. */
    fun validateHours(hours: List<String>): Map<Int, String> {
        val errors = mutableMapOf<Int, String>()
        hours.forEachIndexed { index, text ->
            val value = text.trim().ifEmpty { "0" }.toBigDecimalOrNull()
            when {
                value == null -> errors[index] = "Not a number"
                value < BigDecimal.ZERO -> errors[index] = "Must be 0 or more"
                value > MAX_DAY -> errors[index] = "Max 24 h"
            }
        }
        return errors
    }

    fun total(hours: List<String>): BigDecimal =
        hours.mapNotNull { it.trim().ifEmpty { "0" }.toBigDecimalOrNull() }.fold(BigDecimal.ZERO, BigDecimal::add)
}
