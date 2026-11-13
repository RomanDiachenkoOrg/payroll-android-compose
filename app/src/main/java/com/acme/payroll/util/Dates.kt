package com.acme.payroll.util

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

object Dates {
    val display: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
    val shortDisplay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
    val monthDisplay: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    private val timestamp: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")

    fun weekStart(date: LocalDate): LocalDate = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))

    /** Last weekday of the month; salaries are paid on that day. */
    fun payDateFor(month: YearMonth): LocalDate {
        var d = month.atEndOfMonth()
        while (d.dayOfWeek == DayOfWeek.SATURDAY || d.dayOfWeek == DayOfWeek.SUNDAY) d = d.minusDays(1)
        return d
    }

    fun format(date: LocalDate?): String = date?.format(display) ?: "-"

    fun formatWeek(start: LocalDate): String =
        "${start.format(shortDisplay)} - ${start.plusDays(6).format(display)}"

    fun formatTimestamp(epochMillis: Long): String =
        LocalDateTime.ofInstant(Instant.ofEpochMilli(epochMillis), ZoneId.systemDefault()).format(timestamp)

    fun toEpochMillisUtc(date: LocalDate): Long = date.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()

    fun fromEpochMillisUtc(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
}
