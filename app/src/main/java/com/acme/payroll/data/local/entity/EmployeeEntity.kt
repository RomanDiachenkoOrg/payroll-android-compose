package com.acme.payroll.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import com.acme.payroll.domain.model.PayType
import java.time.LocalDate

@Entity(
    tableName = "employees",
    foreignKeys = [
        ForeignKey(
            entity = DepartmentEntity::class,
            parentColumns = ["id"],
            childColumns = ["department_id"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index(value = ["email"], unique = true), Index("department_id")],
)
data class EmployeeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "first_name") val firstName: String,
    @ColumnInfo(name = "last_name") val lastName: String,
    val email: String,
    val phone: String = "",
    @ColumnInfo(name = "job_title") val jobTitle: String,
    @ColumnInfo(name = "department_id") val departmentId: Long,
    @ColumnInfo(name = "pay_type") val payType: PayType,
    @ColumnInfo(name = "annual_salary_cents") val annualSalaryCents: Long? = null,
    @ColumnInfo(name = "hourly_rate_cents") val hourlyRateCents: Long? = null,
    @ColumnInfo(name = "hire_date") val hireDate: LocalDate,
    val active: Boolean = true,
    @ColumnInfo(name = "pension_opt_in") val pensionOptIn: Boolean = false,
    @ColumnInfo(name = "other_deduction_cents") val otherDeductionCents: Long = 0,
) {
    val fullName: String get() = "$firstName $lastName"
}

data class EmployeeWithDepartment(
    @Embedded val employee: EmployeeEntity,
    @Relation(parentColumn = "department_id", entityColumn = "id")
    val department: DepartmentEntity,
)

@Entity(
    tableName = "salary_history",
    foreignKeys = [
        ForeignKey(
            entity = EmployeeEntity::class,
            parentColumns = ["id"],
            childColumns = ["employee_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("employee_id")],
)
data class SalaryHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "employee_id") val employeeId: Long,
    @ColumnInfo(name = "effective_date") val effectiveDate: LocalDate,
    @ColumnInfo(name = "annual_salary_cents") val annualSalaryCents: Long? = null,
    @ColumnInfo(name = "hourly_rate_cents") val hourlyRateCents: Long? = null,
    val reason: String,
)
