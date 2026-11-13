package com.acme.payroll.data.local

import androidx.room.TypeConverter
import java.math.BigDecimal
import java.time.LocalDate

/** Dates are stored as ISO-8601 strings so that lexical ordering matches chronological ordering. */
class Converters {
    @TypeConverter
    fun localDateToString(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun bigDecimalToString(value: BigDecimal?): String? = value?.toPlainString()

    @TypeConverter
    fun stringToBigDecimal(value: String?): BigDecimal? = value?.let(::BigDecimal)
}
