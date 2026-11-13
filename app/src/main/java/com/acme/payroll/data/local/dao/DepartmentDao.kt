package com.acme.payroll.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.DepartmentSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface DepartmentDao {
    @Query("SELECT * FROM departments ORDER BY name")
    fun observeAll(): Flow<List<DepartmentEntity>>

    @Query(
        """
        SELECT d.id, d.name, d.code, d.cost_center,
               COALESCE(SUM(CASE WHEN e.active = 1 THEN 1 ELSE 0 END), 0) AS active_count,
               COUNT(e.id) AS total_count
        FROM departments d
        LEFT JOIN employees e ON e.department_id = d.id
        GROUP BY d.id
        ORDER BY d.name
        """
    )
    fun observeSummaries(): Flow<List<DepartmentSummary>>

    @Query("SELECT * FROM departments WHERE id = :id")
    suspend fun getById(id: Long): DepartmentEntity?

    @Query("SELECT * FROM departments WHERE code = :code COLLATE NOCASE LIMIT 1")
    suspend fun getByCode(code: String): DepartmentEntity?

    @Query("SELECT COUNT(*) FROM employees WHERE department_id = :departmentId AND active = 1")
    suspend fun countActiveEmployees(departmentId: Long): Int

    @Query("SELECT COUNT(*) FROM employees WHERE department_id = :departmentId")
    suspend fun countAllEmployees(departmentId: Long): Int

    @Query("SELECT COUNT(*) FROM departments")
    suspend fun count(): Int

    @Insert
    suspend fun insert(department: DepartmentEntity): Long

    @Update
    suspend fun update(department: DepartmentEntity)

    @Delete
    suspend fun delete(department: DepartmentEntity)
}
