package com.acme.payroll.data.repository

import com.acme.payroll.data.local.dao.DepartmentDao
import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.DepartmentSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface DepartmentResult {
    data object Ok : DepartmentResult
    data class Error(val message: String) : DepartmentResult
}

@Singleton
class DepartmentRepository @Inject constructor(
    private val departmentDao: DepartmentDao,
    private val audit: AuditRepository,
) {
    fun observeAll(): Flow<List<DepartmentEntity>> = departmentDao.observeAll()

    fun observeSummaries(): Flow<List<DepartmentSummary>> = departmentDao.observeSummaries()

    suspend fun save(department: DepartmentEntity): DepartmentResult {
        val name = department.name.trim()
        val code = department.code.trim().uppercase()
        if (name.isEmpty()) return DepartmentResult.Error("Name is required")
        if (code.length !in 2..6) return DepartmentResult.Error("Code must be 2-6 characters")
        val clash = departmentDao.getByCode(code)
        if (clash != null && clash.id != department.id) {
            return DepartmentResult.Error("Code $code is already used by ${clash.name}")
        }
        val cleaned = department.copy(name = name, code = code, costCenter = department.costCenter.trim())
        if (department.id == 0L) {
            departmentDao.insert(cleaned)
            audit.log("CREATE", "Department", "Created $name ($code)")
        } else {
            departmentDao.update(cleaned)
            audit.log("UPDATE", "Department", "Updated $name ($code)")
        }
        return DepartmentResult.Ok
    }

    suspend fun delete(departmentId: Long): DepartmentResult {
        val department = departmentDao.getById(departmentId) ?: return DepartmentResult.Error("Department not found")
        val active = departmentDao.countActiveEmployees(departmentId)
        if (active > 0) {
            return DepartmentResult.Error(
                "Cannot delete ${department.name}: it has $active active employee${if (active == 1) "" else "s"}."
            )
        }
        val former = departmentDao.countAllEmployees(departmentId)
        if (former > 0) {
            return DepartmentResult.Error(
                "Cannot delete ${department.name}: move its $former inactive employee record(s) to another department first."
            )
        }
        departmentDao.delete(department)
        audit.log("DELETE", "Department", "Deleted ${department.name}")
        return DepartmentResult.Ok
    }
}
