package com.acme.payroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.acme.payroll.data.local.entity.TimesheetDetails
import com.acme.payroll.data.local.entity.TimesheetEntity
import com.acme.payroll.data.local.entity.TimesheetEntryEntity
import com.acme.payroll.domain.model.TimesheetStatus
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface TimesheetDao {
    @Transaction
    @Query("SELECT * FROM timesheets ORDER BY week_start DESC, id DESC")
    fun observeAll(): Flow<List<TimesheetDetails>>

    @Transaction
    @Query("SELECT * FROM timesheets WHERE id = :id")
    suspend fun getDetails(id: Long): TimesheetDetails?

    @Query("SELECT * FROM timesheets WHERE employee_id = :employeeId AND week_start = :weekStart LIMIT 1")
    suspend fun findForWeek(employeeId: Long, weekStart: LocalDate): TimesheetEntity?

    @Transaction
    @Query(
        """
        SELECT * FROM timesheets
        WHERE employee_id = :employeeId AND status = 'APPROVED'
          AND week_start >= :from AND week_start <= :to
        """
    )
    suspend fun approvedInPeriod(employeeId: Long, from: LocalDate, to: LocalDate): List<TimesheetDetails>

    @Query("SELECT COUNT(*) FROM timesheets WHERE status = :status")
    fun observeCountByStatus(status: TimesheetStatus): Flow<Int>

    @Query(
        """
        SELECT COUNT(*) FROM timesheets t JOIN employees e ON e.id = t.employee_id
        WHERE t.status = :status AND e.department_id = :departmentId
        """
    )
    fun observeCountByStatusInDepartment(status: TimesheetStatus, departmentId: Long): Flow<Int>

    @Insert
    suspend fun insert(timesheet: TimesheetEntity): Long

    @Update
    suspend fun update(timesheet: TimesheetEntity)

    @Insert
    suspend fun insertEntries(entries: List<TimesheetEntryEntity>)

    @Query("DELETE FROM timesheet_entries WHERE timesheet_id = :timesheetId")
    suspend fun deleteEntries(timesheetId: Long)

    @Transaction
    suspend fun replaceEntries(timesheetId: Long, entries: List<TimesheetEntryEntity>) {
        deleteEntries(timesheetId)
        insertEntries(entries)
    }
}
