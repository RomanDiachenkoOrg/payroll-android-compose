package com.acme.payroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.acme.payroll.data.local.entity.DepartmentCostRow
import com.acme.payroll.data.local.entity.PayRunEntity
import com.acme.payroll.data.local.entity.PayRunSummary
import com.acme.payroll.data.local.entity.PayslipDetails
import com.acme.payroll.data.local.entity.PayslipEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface PayRunDao {
    @Query(
        """
        SELECT r.*, COUNT(p.id) AS employee_count,
               COALESCE(SUM(p.gross_cents), 0) AS gross_cents,
               COALESCE(SUM(p.net_cents), 0) AS net_cents
        FROM pay_runs r LEFT JOIN payslips p ON p.pay_run_id = r.id
        GROUP BY r.id
        ORDER BY r.period_start DESC
        """
    )
    fun observeSummaries(): Flow<List<PayRunSummary>>

    @Query("SELECT * FROM pay_runs WHERE id = :id")
    fun observeById(id: Long): Flow<PayRunEntity?>

    @Query("SELECT * FROM pay_runs WHERE id = :id")
    suspend fun getById(id: Long): PayRunEntity?

    @Query("SELECT * FROM pay_runs WHERE period_start = :periodStart LIMIT 1")
    suspend fun findByPeriodStart(periodStart: LocalDate): PayRunEntity?

    @Query("SELECT * FROM pay_runs WHERE status = 'PAID' ORDER BY period_start DESC LIMIT 1")
    fun observeLastPaid(): Flow<PayRunEntity?>

    @Query("SELECT * FROM pay_runs WHERE status != 'PAID' ORDER BY pay_date LIMIT 1")
    fun observeNextOpen(): Flow<PayRunEntity?>

    @Insert
    suspend fun insert(run: PayRunEntity): Long

    @Update
    suspend fun update(run: PayRunEntity)

    @Transaction
    @Query(
        """
        SELECT p.* FROM payslips p JOIN employees e ON e.id = p.employee_id
        WHERE p.pay_run_id = :runId ORDER BY e.last_name, e.first_name
        """
    )
    fun observePayslipsForRun(runId: Long): Flow<List<PayslipDetails>>

    @Transaction
    @Query("SELECT * FROM payslips WHERE id = :id")
    fun observePayslip(id: Long): Flow<PayslipDetails?>

    @Transaction
    @Query(
        """
        SELECT p.* FROM payslips p JOIN pay_runs r ON r.id = p.pay_run_id
        WHERE p.employee_id = :employeeId AND r.status IN ('APPROVED', 'PAID')
        ORDER BY r.period_start DESC
        """
    )
    fun observePayslipsForEmployee(employeeId: Long): Flow<List<PayslipDetails>>

    @Query("DELETE FROM payslips WHERE pay_run_id = :runId")
    suspend fun deletePayslips(runId: Long)

    @Insert
    suspend fun insertPayslips(payslips: List<PayslipEntity>)

    @Transaction
    suspend fun replacePayslips(runId: Long, payslips: List<PayslipEntity>) {
        deletePayslips(runId)
        insertPayslips(payslips)
    }

    @Query("SELECT COALESCE(SUM(gross_cents), 0) FROM payslips WHERE pay_run_id = :runId")
    fun observeGrossTotal(runId: Long): Flow<Long>

    @Query(
        """
        SELECT d.name AS department_name,
               COUNT(DISTINCT p.employee_id) AS headcount,
               SUM(p.gross_cents) AS gross_cents,
               SUM(p.tax_cents) AS tax_cents,
               SUM(p.social_cents) AS social_cents,
               SUM(p.pension_cents) AS pension_cents,
               SUM(p.net_cents) AS net_cents
        FROM payslips p
        JOIN pay_runs r ON r.id = p.pay_run_id
        JOIN employees e ON e.id = p.employee_id
        JOIN departments d ON d.id = e.department_id
        WHERE r.status IN ('APPROVED', 'PAID')
          AND r.period_start >= :from AND r.period_end <= :to
        GROUP BY d.id
        ORDER BY d.name
        """
    )
    suspend fun costByDepartment(from: LocalDate, to: LocalDate): List<DepartmentCostRow>
}
