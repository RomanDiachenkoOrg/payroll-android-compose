package com.acme.payroll.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.acme.payroll.domain.model.DeductionKind
import com.acme.payroll.domain.model.Role
import java.math.BigDecimal

@Entity(tableName = "users", indices = [Index(value = ["email"], unique = true)])
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val email: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "password_hash") val passwordHash: String,
    val salt: String,
    val role: Role,
    @ColumnInfo(name = "employee_id") val employeeId: Long? = null,
    @ColumnInfo(name = "department_id") val departmentId: Long? = null,
    @ColumnInfo(name = "last_login_at") val lastLoginAt: Long? = null,
)

@Entity(tableName = "audit_log", indices = [Index("timestamp")])
data class AuditLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    @ColumnInfo(name = "user_email") val userEmail: String,
    val action: String,
    val entity: String,
    val details: String,
)

@Entity(tableName = "deduction_types", indices = [Index(value = ["code"], unique = true)])
data class DeductionTypeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val kind: DeductionKind,
    @ColumnInfo(name = "default_amount") val defaultAmount: BigDecimal,
)

@Entity(tableName = "tax_bands")
data class TaxBandEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "from_amount") val fromAmount: BigDecimal,
    @ColumnInfo(name = "up_to_amount") val upToAmount: BigDecimal?,
    @ColumnInfo(name = "rate_percent") val ratePercent: BigDecimal,
)

@Entity(tableName = "payroll_settings")
data class PayrollSettingEntity(
    @PrimaryKey val key: String,
    val value: String,
) {
    companion object {
        const val SOCIAL_PERCENT = "social_percent"
        const val PENSION_PERCENT = "pension_percent"
    }
}

data class DepartmentCostRow(
    @ColumnInfo(name = "department_name") val departmentName: String,
    val headcount: Int,
    @ColumnInfo(name = "gross_cents") val grossCents: Long,
    @ColumnInfo(name = "tax_cents") val taxCents: Long,
    @ColumnInfo(name = "social_cents") val socialCents: Long,
    @ColumnInfo(name = "pension_cents") val pensionCents: Long,
    @ColumnInfo(name = "net_cents") val netCents: Long,
)
