package com.acme.payroll.data.repository

import androidx.room.withTransaction
import com.acme.payroll.data.local.PayrollDatabase
import com.acme.payroll.data.local.dao.EmployeeDao
import com.acme.payroll.data.local.dao.PayRunDao
import com.acme.payroll.data.local.dao.TimesheetDao
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.data.local.entity.PayRunEntity
import com.acme.payroll.data.local.entity.PayRunSummary
import com.acme.payroll.data.local.entity.PayslipDetails
import com.acme.payroll.data.local.entity.PayslipEntity
import com.acme.payroll.domain.model.PayRunStatus
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.domain.payroll.PayInput
import com.acme.payroll.domain.payroll.PayrollCalculator
import com.acme.payroll.domain.payroll.PayrollRates
import com.acme.payroll.util.Dates
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import javax.inject.Singleton

class PayRunException(message: String) : Exception(message)

@Singleton
class PayRunRepository @Inject constructor(
    private val db: PayrollDatabase,
    private val payRunDao: PayRunDao,
    private val employeeDao: EmployeeDao,
    private val timesheetDao: TimesheetDao,
    private val settings: SettingsRepository,
    private val audit: AuditRepository,
) {
    fun observeSummaries(): Flow<List<PayRunSummary>> = payRunDao.observeSummaries()
    fun observeRun(id: Long): Flow<PayRunEntity?> = payRunDao.observeById(id)
    fun observePayslips(runId: Long): Flow<List<PayslipDetails>> = payRunDao.observePayslipsForRun(runId)
    fun observePayslip(id: Long): Flow<PayslipDetails?> = payRunDao.observePayslip(id)
    fun observePayslipsForEmployee(employeeId: Long): Flow<List<PayslipDetails>> = payRunDao.observePayslipsForEmployee(employeeId)
    fun observeLastPaid(): Flow<PayRunEntity?> = payRunDao.observeLastPaid()
    fun observeNextOpen(): Flow<PayRunEntity?> = payRunDao.observeNextOpen()
    fun observeGrossTotal(runId: Long): Flow<Long> = payRunDao.observeGrossTotal(runId)

    suspend fun create(month: YearMonth): Long {
        val start = month.atDay(1)
        if (payRunDao.findByPeriodStart(start) != null) {
            throw PayRunException("A pay run for ${month.format(Dates.monthDisplay)} already exists")
        }
        val id = payRunDao.insert(
            PayRunEntity(periodStart = start, periodEnd = month.atEndOfMonth(), payDate = Dates.payDateFor(month))
        )
        audit.log("CREATE", "PayRun", "Pay run ${month.format(Dates.monthDisplay)}")
        return id
    }

    /** (Re)builds the payslips of a run. Allowed while the run is Draft or Calculated. */
    suspend fun calculate(runId: Long) {
        val rates = settings.getRates()
        db.withTransaction {
            val run = payRunDao.getById(runId) ?: throw PayRunException("Pay run not found")
            if (run.status != PayRunStatus.DRAFT && run.status != PayRunStatus.CALCULATED) {
                throw PayRunException("Pay run is ${run.status.label.lowercase()} and cannot be recalculated")
            }
            val payslips = employeeDao.getActive()
                .filter { !it.hireDate.isAfter(run.periodEnd) }
                .mapNotNull { buildPayslip(run, it, rates) }
            payRunDao.replacePayslips(runId, payslips)
            payRunDao.update(run.copy(status = PayRunStatus.CALCULATED))
        }
        audit.log("CALCULATE", "PayRun", "Pay run #$runId calculated")
    }

    internal suspend fun buildPayslip(run: PayRunEntity, employee: EmployeeEntity, rates: PayrollRates): PayslipEntity? {
        val calculator = PayrollCalculator(rates)
        val input = when (employee.payType) {
            PayType.SALARIED -> PayInput(
                payType = PayType.SALARIED,
                annualSalary = Money.fromCents(employee.annualSalaryCents ?: return null),
                pensionOptIn = employee.pensionOptIn,
                otherDeductions = Money.fromCents(employee.otherDeductionCents),
            )
            PayType.HOURLY -> {
                val weeks = timesheetDao.approvedInPeriod(employee.id, run.periodStart, run.periodEnd)
                if (weeks.isEmpty()) return null
                PayInput(
                    payType = PayType.HOURLY,
                    hourlyRate = Money.fromCents(employee.hourlyRateCents ?: return null),
                    weeklyHours = weeks.map { it.totalHours },
                    pensionOptIn = employee.pensionOptIn,
                    otherDeductions = Money.fromCents(employee.otherDeductionCents),
                )
            }
        }
        val result = calculator.calculate(input)
        return PayslipEntity(
            payRunId = run.id,
            employeeId = employee.id,
            regularHours = result.regularHours,
            overtimeHours = result.overtimeHours,
            grossCents = Money.toCents(result.gross),
            taxCents = Money.toCents(result.incomeTax),
            socialCents = Money.toCents(result.social),
            pensionCents = Money.toCents(result.pension),
            otherCents = Money.toCents(result.other),
            netCents = Money.toCents(result.net),
        )
    }

    suspend fun approve(runId: Long, approver: String) {
        val run = payRunDao.getById(runId) ?: throw PayRunException("Pay run not found")
        if (run.status != PayRunStatus.CALCULATED) throw PayRunException("Calculate the pay run before approving it")
        payRunDao.update(run.copy(status = PayRunStatus.APPROVED, approvedBy = approver))
        audit.log("APPROVE", "PayRun", "Pay run ${run.periodStart} approved")
    }

    suspend fun markPaid(runId: Long, paidOn: LocalDate = LocalDate.now()) {
        val run = payRunDao.getById(runId) ?: throw PayRunException("Pay run not found")
        if (run.status == PayRunStatus.PAID) throw PayRunException("Pay run is already paid")
        payRunDao.update(run.copy(status = PayRunStatus.PAID, paidAt = Dates.toEpochMillisUtc(paidOn)))
        audit.log("PAY", "PayRun", "Pay run ${run.periodStart} marked as paid")
    }
}
