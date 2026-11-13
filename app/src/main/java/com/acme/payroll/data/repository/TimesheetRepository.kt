package com.acme.payroll.data.repository

import androidx.room.withTransaction
import com.acme.payroll.data.local.PayrollDatabase
import com.acme.payroll.data.local.dao.TimesheetDao
import com.acme.payroll.data.local.entity.TimesheetDetails
import com.acme.payroll.data.local.entity.TimesheetEntity
import com.acme.payroll.data.local.entity.TimesheetEntryEntity
import com.acme.payroll.domain.model.TimesheetStatus
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

class TimesheetException(message: String) : Exception(message)

@Singleton
class TimesheetRepository @Inject constructor(
    private val db: PayrollDatabase,
    private val timesheetDao: TimesheetDao,
    private val audit: AuditRepository,
) {
    fun observeAll(): Flow<List<TimesheetDetails>> = timesheetDao.observeAll()

    fun observePendingCount(departmentId: Long?): Flow<Int> =
        if (departmentId == null) timesheetDao.observeCountByStatus(TimesheetStatus.SUBMITTED)
        else timesheetDao.observeCountByStatusInDepartment(TimesheetStatus.SUBMITTED, departmentId)

    suspend fun get(id: Long): TimesheetDetails? = timesheetDao.getDetails(id)

    /** Creates or updates a draft and returns its id. [hours] holds seven values starting on Monday. */
    suspend fun saveDraft(
        id: Long?,
        employeeId: Long,
        weekStart: LocalDate,
        hours: List<BigDecimal>,
        notes: String,
    ): Long = db.withTransaction {
        require(hours.size == 7) { "A week has seven days" }
        val clash = timesheetDao.findForWeek(employeeId, weekStart)
        if (clash != null && clash.id != id) {
            throw TimesheetException("A timesheet for this week already exists")
        }
        val timesheetId = if (id == null) {
            timesheetDao.insert(TimesheetEntity(employeeId = employeeId, weekStart = weekStart, notes = notes))
        } else {
            val existing = timesheetDao.getDetails(id)?.timesheet ?: throw TimesheetException("Timesheet not found")
            if (existing.status == TimesheetStatus.SUBMITTED || existing.status == TimesheetStatus.APPROVED) {
                throw TimesheetException("Only draft or rejected timesheets can be edited")
            }
            timesheetDao.update(
                existing.copy(
                    weekStart = weekStart,
                    notes = notes,
                    status = TimesheetStatus.DRAFT,
                    updatedAt = System.currentTimeMillis(),
                )
            )
            id
        }
        timesheetDao.replaceEntries(
            timesheetId,
            hours.mapIndexed { i, h -> TimesheetEntryEntity(timesheetId = timesheetId, workDate = weekStart.plusDays(i.toLong()), hours = h) },
        )
        timesheetId
    }

    suspend fun submit(id: Long) = transition(id, TimesheetStatus.SUBMITTED, "SUBMIT") { current ->
        if (current.status != TimesheetStatus.DRAFT && current.status != TimesheetStatus.REJECTED) {
            throw TimesheetException("Timesheet was already submitted")
        }
        current.copy(rejectReason = null)
    }

    suspend fun approve(id: Long, approver: String) = transition(id, TimesheetStatus.APPROVED, "APPROVE") { current ->
        if (current.status != TimesheetStatus.SUBMITTED) throw TimesheetException("Only submitted timesheets can be approved")
        current.copy(decidedBy = approver, rejectReason = null)
    }

    suspend fun reject(id: Long, approver: String, reason: String) = transition(id, TimesheetStatus.REJECTED, "REJECT") { current ->
        if (current.status != TimesheetStatus.SUBMITTED) throw TimesheetException("Only submitted timesheets can be rejected")
        if (reason.isBlank()) throw TimesheetException("A reason is required")
        current.copy(decidedBy = approver, rejectReason = reason.trim())
    }

    private suspend fun transition(
        id: Long,
        to: TimesheetStatus,
        action: String,
        check: (TimesheetEntity) -> TimesheetEntity,
    ) {
        val current = timesheetDao.getDetails(id) ?: throw TimesheetException("Timesheet not found")
        val updated = check(current.timesheet).copy(status = to, updatedAt = System.currentTimeMillis())
        timesheetDao.update(updated)
        audit.log(action, "Timesheet", "${current.employee.fullName}, week of ${current.timesheet.weekStart}")
    }
}
