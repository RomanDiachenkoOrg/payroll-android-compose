package com.acme.payroll.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "departments", indices = [Index(value = ["code"], unique = true)])
data class DepartmentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val code: String,
    @ColumnInfo(name = "cost_center") val costCenter: String,
)

/** Department row with headcounts, used by the departments screen. */
data class DepartmentSummary(
    val id: Long,
    val name: String,
    val code: String,
    @ColumnInfo(name = "cost_center") val costCenter: String,
    @ColumnInfo(name = "active_count") val activeCount: Int,
    @ColumnInfo(name = "total_count") val totalCount: Int,
)
