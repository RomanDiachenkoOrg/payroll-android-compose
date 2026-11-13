package com.acme.payroll.util

/** Minimal RFC 4180 writer. */
object Csv {
    fun escape(value: String): String =
        if (value.any { it == ',' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else value

    fun row(vararg values: Any?): String = values.joinToString(",") { escape(it?.toString() ?: "") }

    fun build(header: List<String>, rows: List<List<Any?>>): String = buildString {
        append(row(*header.toTypedArray())).append("\r\n")
        rows.forEach { append(row(*it.toTypedArray())).append("\r\n") }
    }
}
