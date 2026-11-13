package com.acme.payroll.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.acme.payroll.domain.model.PayRunStatus
import java.math.BigDecimal
import java.time.LocalDate

@Entity(tableName = "pay_runs", indices = [Index(value = ["period_start"], unique = true)])
data class PayRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "period_start") val periodStart: LocalDate,
    @ColumnInfo(name = "period_end") val periodEnd: LocalDate,
    @ColumnInfo(name = "pay_date") val payDate: LocalDate,
    val status: PayRunStatus = PayRunStatus.DRAFT,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "approved_by") val approvedBy: String? = null,
    @ColumnInfo(name = "paid_at") val paidAt: Long? = null,
)

/** Pay run with aggregated totals for list screens. */
data class PayRunSummary(
    @Embedded val run: PayRunEntity,
    @ColumnInfo(name = "employee_count") val employeeCount: Int,
    @ColumnInfo(name = "gross_cents") val grossCents: Long,
    @ColumnInfo(name = "net_cents") val netCents: Long,
)

@Entity(
    tableName = "payslips",
    foreignKeys = [
        ForeignKey(
            entity = PayRunEntity::class,
            parentColumns = ["id"],
            childColumns = ["pay_run_id"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["pay_run_id", "employee_id"], unique = true), Index("employee_id")],
)
data class PayslipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "pay_run_id") val payRunId: Long,
    @ColumnInfo(name = "employee_id") val employeeId: Long,
    @ColumnInfo(name = "regular_hours") val regularHours: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "overtime_hours") val overtimeHours: BigDecimal = BigDecimal.ZERO,
    @ColumnInfo(name = "gross_cents") val grossCents: Long,
    @ColumnInfo(name = "tax_cents") val taxCents: Long,
    @ColumnInfo(name = "social_cents") val socialCents: Long,
    @ColumnInfo(name = "pension_cents") val pensionCents: Long,
    @ColumnInfo(name = "other_cents") val otherCents: Long,
    @ColumnInfo(name = "net_cents") val netCents: Long,
)

data class PayslipDetails(
    @Embedded val payslip: PayslipEntity,
    @Relation(parentColumn = "employee_id", entityColumn = "id")
    val employee: EmployeeEntity,
    @Relation(parentColumn = "pay_run_id", entityColumn = "id")
    val payRun: PayRunEntity,
)
