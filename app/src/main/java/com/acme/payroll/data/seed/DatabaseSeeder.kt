package com.acme.payroll.data.seed

import android.util.Log
import androidx.room.withTransaction
import com.acme.payroll.data.local.PayrollDatabase
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
import com.acme.payroll.domain.PasswordHasher
import com.acme.payroll.domain.model.DeductionKind
import com.acme.payroll.domain.model.PayRunStatus
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.TimesheetStatus
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.domain.payroll.PayInput
import com.acme.payroll.domain.payroll.PayrollCalculator
import com.acme.payroll.domain.payroll.PayrollRates
import com.acme.payroll.util.Dates
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.math.BigDecimal
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Populates an empty database with demo data. Dates are relative to the current day so that the
 * app always shows two recent paid pay runs, an open draft run and timesheets awaiting approval.
 */
@Singleton
class DatabaseSeeder @Inject constructor(
    private val db: PayrollDatabase,
) {
    private val mutex = Mutex()

    suspend fun seedIfEmpty(today: LocalDate = LocalDate.now()) = mutex.withLock {
        if (db.departmentDao().count() > 0) return@withLock
        Log.i(TAG, "Empty database, loading sample data")
        db.withTransaction { seed(today) }
    }

    private suspend fun seed(today: LocalDate) {
        val rates = PayrollRates.DEFAULT
        val settingsDao = db.settingsDao()
        settingsDao.insertTaxBands(rates.taxBands.map { TaxBandEntity(fromAmount = it.from, upToAmount = it.upTo, ratePercent = it.ratePercent) })
        settingsDao.upsertSetting(PayrollSettingEntity(PayrollSettingEntity.SOCIAL_PERCENT, rates.socialPercent.toPlainString()))
        settingsDao.upsertSetting(PayrollSettingEntity(PayrollSettingEntity.PENSION_PERCENT, rates.pensionPercent.toPlainString()))
        settingsDao.insertDeductionTypes(
            listOf(
                DeductionTypeEntity(code = "HEALTH", name = "Health insurance", kind = DeductionKind.FIXED, defaultAmount = BigDecimal("120.00")),
                DeductionTypeEntity(code = "DENTAL", name = "Dental plan", kind = DeductionKind.FIXED, defaultAmount = BigDecimal("30.00")),
                DeductionTypeEntity(code = "PARKING", name = "Parking", kind = DeductionKind.FIXED, defaultAmount = BigDecimal("45.00")),
                DeductionTypeEntity(code = "UNION", name = "Union dues", kind = DeductionKind.PERCENT, defaultAmount = BigDecimal("1.0")),
            )
        )

        val deptIds = SeedData.departments.associate { dept ->
            dept.code to db.departmentDao().insert(DepartmentEntity(name = dept.name, code = dept.code, costCenter = dept.costCenter))
        }

        val employeeDao = db.employeeDao()
        val employees = SeedData.people.map { p ->
            val cents = Money.toCents(BigDecimal(p.amount))
            val entity = EmployeeEntity(
                firstName = p.first,
                lastName = p.last,
                email = p.email,
                phone = "+1 555 01${(10 + SeedData.people.indexOf(p)).toString().padStart(2, '0')}",
                jobTitle = p.title,
                departmentId = deptIds.getValue(p.deptCode),
                payType = p.payType,
                annualSalaryCents = cents.takeIf { p.payType == PayType.SALARIED },
                hourlyRateCents = cents.takeIf { p.payType == PayType.HOURLY },
                hireDate = p.hired,
                active = p.active,
                pensionOptIn = p.pension,
                otherDeductionCents = Money.toCents(BigDecimal(p.otherDeduction)),
            )
            entity.copy(id = employeeDao.insert(entity))
        }

        // Salary history: starting pay at hire, then an annual review at the start of last year.
        val reviewDate = LocalDate.of(today.year - 1, 1, 1)
        employees.forEach { e ->
            val current = e.annualSalaryCents ?: e.hourlyRateCents ?: 0L
            if (e.hireDate.isBefore(reviewDate)) {
                val starting = current * 88 / 100
                employeeDao.insertSalaryHistory(history(e, e.hireDate, starting, "Starting pay"))
                employeeDao.insertSalaryHistory(history(e, reviewDate, current, "Annual review"))
            } else {
                employeeDao.insertSalaryHistory(history(e, e.hireDate, current, "Starting pay"))
            }
        }

        seedTimesheets(employees, today)
        seedPayRuns(employees, today, rates)
        seedUsers(employees, deptIds)
        seedAudit(today)
    }

    private fun history(e: EmployeeEntity, date: LocalDate, cents: Long, reason: String) = SalaryHistoryEntity(
        employeeId = e.id,
        effectiveDate = date,
        annualSalaryCents = cents.takeIf { e.payType == PayType.SALARIED },
        hourlyRateCents = cents.takeIf { e.payType == PayType.HOURLY },
        reason = reason,
    )

    private suspend fun seedTimesheets(employees: List<EmployeeEntity>, today: LocalDate) {
        val dao = db.timesheetDao()
        val currentWeek = Dates.weekStart(today)
        val firstWeek = Dates.weekStart(YearMonth.from(today).minusMonths(2).atDay(1)).let {
            if (it.isBefore(YearMonth.from(today).minusMonths(2).atDay(1))) it.plusWeeks(1) else it
        }
        employees.filter { it.payType == PayType.HOURLY && it.active }.forEachIndexed { empIndex, e ->
            var week = firstWeek
            var weekIndex = 0
            while (!week.isAfter(currentWeek)) {
                val status = when {
                    week == currentWeek -> TimesheetStatus.DRAFT
                    week == currentWeek.minusWeeks(1) && e.lastName == "Nguyen" -> TimesheetStatus.REJECTED
                    week == currentWeek.minusWeeks(1) -> TimesheetStatus.SUBMITTED
                    else -> TimesheetStatus.APPROVED
                }
                val overtimeDay = (empIndex + weekIndex) % 3 == 0
                val hours = (0 until 7).map { day ->
                    when {
                        week == currentWeek && week.plusDays(day.toLong()).isAfter(today) -> BigDecimal.ZERO
                        day < 5 -> if (e.lastName == "Price" && day == 4) BigDecimal("6") else BigDecimal("8")
                        day == 5 && overtimeDay -> BigDecimal(if (empIndex % 2 == 0) "5" else "4.5")
                        else -> BigDecimal.ZERO
                    }
                }
                val id = dao.insert(
                    TimesheetEntity(
                        employeeId = e.id,
                        weekStart = week,
                        status = status,
                        rejectReason = if (status == TimesheetStatus.REJECTED) "Friday hours missing the client visit, please split them" else null,
                        decidedBy = when (status) {
                            TimesheetStatus.APPROVED, TimesheetStatus.REJECTED -> "manager@acme.test"
                            else -> null
                        },
                        updatedAt = toMillis(week.plusDays(7)),
                    )
                )
                dao.insertEntries(hours.mapIndexed { day, h -> TimesheetEntryEntity(timesheetId = id, workDate = week.plusDays(day.toLong()), hours = h) })
                week = week.plusWeeks(1)
                weekIndex++
            }
        }
    }

    private suspend fun seedPayRuns(employees: List<EmployeeEntity>, today: LocalDate, rates: PayrollRates) {
        val dao = db.payRunDao()
        val calculator = PayrollCalculator(rates)
        val current = YearMonth.from(today)
        listOf(current.minusMonths(2), current.minusMonths(1)).forEach { month ->
            val payDate = Dates.payDateFor(month)
            val runId = dao.insert(
                PayRunEntity(
                    periodStart = month.atDay(1),
                    periodEnd = month.atEndOfMonth(),
                    payDate = payDate,
                    status = PayRunStatus.PAID,
                    createdAt = toMillis(month.atDay(20)),
                    approvedBy = "admin@acme.test",
                    paidAt = toMillis(payDate),
                )
            )
            val payslips = employees.filter { it.active }.mapNotNull { e ->
                val input = when (e.payType) {
                    PayType.SALARIED -> PayInput(
                        PayType.SALARIED,
                        annualSalary = Money.fromCents(e.annualSalaryCents!!),
                        pensionOptIn = e.pensionOptIn,
                        otherDeductions = Money.fromCents(e.otherDeductionCents),
                    )
                    PayType.HOURLY -> {
                        val weeks = db.timesheetDao().approvedInPeriod(e.id, month.atDay(1), month.atEndOfMonth())
                        if (weeks.isEmpty()) return@mapNotNull null
                        PayInput(
                            PayType.HOURLY,
                            hourlyRate = Money.fromCents(e.hourlyRateCents!!),
                            weeklyHours = weeks.map { it.totalHours },
                            pensionOptIn = e.pensionOptIn,
                            otherDeductions = Money.fromCents(e.otherDeductionCents),
                        )
                    }
                }
                val r = calculator.calculate(input)
                PayslipEntity(
                    payRunId = runId,
                    employeeId = e.id,
                    regularHours = r.regularHours,
                    overtimeHours = r.overtimeHours,
                    grossCents = Money.toCents(r.gross),
                    taxCents = Money.toCents(r.incomeTax),
                    socialCents = Money.toCents(r.social),
                    pensionCents = Money.toCents(r.pension),
                    otherCents = Money.toCents(r.other),
                    netCents = Money.toCents(r.net),
                )
            }
            dao.insertPayslips(payslips)
        }
        dao.insert(
            PayRunEntity(
                periodStart = current.atDay(1),
                periodEnd = current.atEndOfMonth(),
                payDate = Dates.payDateFor(current),
                createdAt = toMillis(current.atDay(1)),
            )
        )
    }

    private suspend fun seedUsers(employees: List<EmployeeEntity>, deptIds: Map<String, Long>) {
        val manager = employees.first { it.email == SeedData.MANAGER_EMPLOYEE_EMAIL }
        val employee = employees.first { it.email == SeedData.EMPLOYEE_EMPLOYEE_EMAIL }
        fun user(email: String, name: String, role: Role, employeeId: Long?, deptId: Long?): UserEntity {
            val salt = email.substringBefore('@').reversed() + "-7f3a"
            return UserEntity(
                email = email,
                displayName = name,
                passwordHash = PasswordHasher.hash(SeedData.DEMO_PASSWORD, salt),
                salt = salt,
                role = role,
                employeeId = employeeId,
                departmentId = deptId,
            )
        }
        db.userDao().insertAll(
            listOf(
                user("admin@acme.test", "Alex Morgan", Role.ADMIN, null, null),
                user("manager@acme.test", manager.fullName, Role.MANAGER, manager.id, deptIds.getValue("ENG")),
                user("employee@acme.test", employee.fullName, Role.EMPLOYEE, employee.id, deptIds.getValue("ENG")),
            )
        )
    }

    private suspend fun seedAudit(today: LocalDate) {
        val current = YearMonth.from(today)
        val entries = mutableListOf<AuditLogEntity>()
        fun add(date: LocalDate, hour: Int, who: String, action: String, entity: String, details: String) {
            entries += AuditLogEntity(
                timestamp = toMillis(date) + hour * 3_600_000L,
                userEmail = who,
                action = action,
                entity = entity,
                details = details,
            )
        }
        add(current.minusMonths(3).atDay(2), 9, "system", "IMPORT", "Employee", "Imported 25 employee records")
        listOf(current.minusMonths(2), current.minusMonths(1)).forEach { m ->
            val label = m.format(Dates.monthDisplay)
            add(m.atDay(20), 10, "admin@acme.test", "CREATE", "PayRun", "Pay run $label")
            add(m.atDay(21), 11, "admin@acme.test", "CALCULATE", "PayRun", "Pay run $label calculated")
            add(m.atDay(22), 15, "admin@acme.test", "APPROVE", "PayRun", "Pay run ${m.atDay(1)} approved")
            add(Dates.payDateFor(m), 8, "admin@acme.test", "PAY", "PayRun", "Pay run ${m.atDay(1)} marked as paid")
        }
        add(current.minusMonths(1).atDay(5), 14, "admin@acme.test", "DEACTIVATE", "Employee", "Employee Ruby Evans")
        add(current.minusMonths(1).atDay(12), 9, "admin@acme.test", "RAISE", "Employee", "Daniel Kim: $96,000.00 from ${current.minusMonths(1).atDay(1)}")
        add(current.atDay(1), 8, "admin@acme.test", "CREATE", "PayRun", "Pay run ${current.format(Dates.monthDisplay)}")
        add(today.minusDays(3), 17, "manager@acme.test", "APPROVE", "Timesheet", "Jonah Price, week of ${Dates.weekStart(today).minusWeeks(2)}")
        add(today.minusDays(2), 16, "manager@acme.test", "LOGIN", "User", "Signed in")
        add(today.minusDays(1), 12, "employee@acme.test", "SUBMIT", "Timesheet", "Emily Turner, week of ${Dates.weekStart(today).minusWeeks(1)}")
        db.auditDao().insertAll(entries)
    }

    private fun toMillis(date: LocalDate): Long = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

    private companion object {
        const val TAG = "DatabaseSeeder"
    }
}
