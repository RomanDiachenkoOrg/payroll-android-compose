package com.acme.payroll.data.repository

import androidx.room.withTransaction
import com.acme.payroll.data.local.PayrollDatabase
import com.acme.payroll.data.local.dao.EmployeeDao
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.data.local.entity.EmployeeWithDepartment
import com.acme.payroll.data.local.entity.SalaryHistoryEntity
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.payroll.Money
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

interface EmployeeRepository {
    fun observeEmployees(): Flow<List<EmployeeWithDepartment>>
    fun observeEmployee(id: Long): Flow<EmployeeWithDepartment?>
    fun observeSalaryHistory(employeeId: Long): Flow<List<SalaryHistoryEntity>>
    suspend fun getEmployee(id: Long): EmployeeEntity?
    suspend fun isEmailTaken(email: String, excludeId: Long?): Boolean
    suspend fun save(employee: EmployeeEntity): Long
    suspend fun setActive(id: Long, active: Boolean)
    suspend fun addRaise(employeeId: Long, newAmountCents: Long, effectiveDate: LocalDate, reason: String)
}

@Singleton
class RoomEmployeeRepository @Inject constructor(
    private val db: PayrollDatabase,
    private val employeeDao: EmployeeDao,
    private val audit: AuditRepository,
) : EmployeeRepository {

    override fun observeEmployees() = employeeDao.observeAllWithDepartment()

    override fun observeEmployee(id: Long) = employeeDao.observeWithDepartment(id)

    override fun observeSalaryHistory(employeeId: Long) = employeeDao.observeSalaryHistory(employeeId)

    override suspend fun getEmployee(id: Long) = employeeDao.getById(id)

    override suspend fun isEmailTaken(email: String, excludeId: Long?): Boolean {
        val existing = employeeDao.getByEmail(email.trim()) ?: return false
        return existing.id != excludeId
    }

    override suspend fun save(employee: EmployeeEntity): Long = db.withTransaction {
        if (employee.id == 0L) {
            val id = employeeDao.insert(employee)
            employeeDao.insertSalaryHistory(
                SalaryHistoryEntity(
                    employeeId = id,
                    effectiveDate = employee.hireDate,
                    annualSalaryCents = employee.annualSalaryCents,
                    hourlyRateCents = employee.hourlyRateCents,
                    reason = "Starting pay",
                )
            )
            audit.log("CREATE", "Employee", "Created ${employee.fullName} (#$id)")
            id
        } else {
            val before = employeeDao.getById(employee.id)
            employeeDao.update(employee)
            val payChanged = before != null &&
                (before.annualSalaryCents != employee.annualSalaryCents || before.hourlyRateCents != employee.hourlyRateCents)
            if (payChanged) {
                employeeDao.insertSalaryHistory(
                    SalaryHistoryEntity(
                        employeeId = employee.id,
                        effectiveDate = LocalDate.now(),
                        annualSalaryCents = employee.annualSalaryCents,
                        hourlyRateCents = employee.hourlyRateCents,
                        reason = "Pay corrected in profile",
                    )
                )
            }
            audit.log("UPDATE", "Employee", "Updated ${employee.fullName} (#${employee.id})")
            employee.id
        }
    }

    override suspend fun setActive(id: Long, active: Boolean) {
        employeeDao.setActive(id, active)
        audit.log(if (active) "REACTIVATE" else "DEACTIVATE", "Employee", "Employee #$id")
    }

    override suspend fun addRaise(employeeId: Long, newAmountCents: Long, effectiveDate: LocalDate, reason: String) {
        db.withTransaction {
            val employee = employeeDao.getById(employeeId) ?: return@withTransaction
            val updated = when (employee.payType) {
                PayType.SALARIED -> employee.copy(annualSalaryCents = newAmountCents)
                PayType.HOURLY -> employee.copy(hourlyRateCents = newAmountCents)
            }
            employeeDao.update(updated)
            employeeDao.insertSalaryHistory(
                SalaryHistoryEntity(
                    employeeId = employeeId,
                    effectiveDate = effectiveDate,
                    annualSalaryCents = updated.annualSalaryCents,
                    hourlyRateCents = updated.hourlyRateCents,
                    reason = reason,
                )
            )
            audit.log("RAISE", "Employee", "${employee.fullName}: ${Money.formatCents(newAmountCents)} from $effectiveDate")
        }
    }
}
