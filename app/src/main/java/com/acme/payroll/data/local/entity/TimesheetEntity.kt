package com.acme.payroll.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.acme.payroll.domain.model.TimesheetStatus
import java.math.BigDecimal
import java.time.LocalDate

@Entity(
    tableName = "timesheets",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["employee_id", "week_start"], unique = true)],
)
data class TimesheetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "employee_id") val employeeId: Long,
    @ColumnInfo(name = "week_start") val weekStart: LocalDate,
    val status: TimesheetStatus = TimesheetStatus.DRAFT,
    val notes: String = "",
    @ColumnInfo(name = "reject_reason") val rejectReason: String? = null,
    @ColumnInfo(name = "decided_by") val decidedBy: String? = null,
    @ColumnInfo(name = "updated_at") val updatedAt: Long = System.currentTimeMillis(),
)

@Entity(
    tableName = "timesheet_entries",
    foreignKeys = [
        ForeignKey(
            entity = TimesheetEntity::class,
            parentColumns = ["id"],
            childColumns = ["timesheet_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("timesheet_id")],
)
data class TimesheetEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "timesheet_id") val timesheetId: Long,
    @ColumnInfo(name = "work_date") val workDate: LocalDate,
    val hours: BigDecimal,
)

data class TimesheetDetails(
    @Embedded val timesheet: TimesheetEntity,
    @Relation(parentColumn = "id", entityColumn = "timesheet_id")
    val entries: List<TimesheetEntryEntity>,
    @Relation(parentColumn = "employee_id", entityColumn = "id")
    val employee: EmployeeEntity,
) {
    val totalHours: BigDecimal get() = entries.fold(BigDecimal.ZERO) { acc, e -> acc + e.hours }
}
