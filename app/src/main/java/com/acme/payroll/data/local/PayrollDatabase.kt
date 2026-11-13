package com.acme.payroll.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.acme.payroll.data.local.dao.AuditDao
import com.acme.payroll.data.local.dao.DepartmentDao
import com.acme.payroll.data.local.dao.EmployeeDao
import com.acme.payroll.data.local.dao.PayRunDao
import com.acme.payroll.data.local.dao.SettingsDao
import com.acme.payroll.data.local.dao.TimesheetDao
import com.acme.payroll.data.local.dao.UserDao
import com.acme.payroll.data.local.entity.AuditLogEntity
import com.acme.payroll.data.local.entity.DeductionTypeEntity
import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.data.local.entity.PayRunEntity
import com.acme.payroll.data.local.entity.PayrollSettingEntity
import com.acme.payroll.data.local.entity.PayslipEntity
import com.acme.payroll.data.local.entity.SalaryHistoryEntity
import com.acme.payroll.data.local.entity.TaxBandEntity
import com.acme.payroll.data.local.entity.TimesheetEntity
import com.acme.payroll.data.local.entity.TimesheetEntryEntity
import com.acme.payroll.data.local.entity.UserEntity

@Database(
    entities = [
        DepartmentEntity::class,
        EmployeeEntity::class,
        SalaryHistoryEntity::class,
        TimesheetEntity::class,
        TimesheetEntryEntity::class,
        PayRunEntity::class,
        PayslipEntity::class,
        UserEntity::class,
        AuditLogEntity::class,
        DeductionTypeEntity::class,
        TaxBandEntity::class,
        PayrollSettingEntity::class,
    ],
    version = PayrollDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class PayrollDatabase : RoomDatabase() {
    abstract fun departmentDao(): DepartmentDao
    abstract fun employeeDao(): EmployeeDao
    abstract fun timesheetDao(): TimesheetDao
    abstract fun payRunDao(): PayRunDao
    abstract fun userDao(): UserDao
    abstract fun auditDao(): AuditDao
    abstract fun settingsDao(): SettingsDao

    companion object {
        const val NAME = "payroll.db"
        const val VERSION = 3

        /** 1 -> 2: employees could opt into the company pension plan. */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE employees ADD COLUMN pension_opt_in INTEGER NOT NULL DEFAULT 0")
            }
        }

        /** 2 -> 3: timesheet notes and last login tracking. */
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE timesheets ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE users ADD COLUMN last_login_at INTEGER")
            }
        }

        val ALL_MIGRATIONS = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
    }
}
