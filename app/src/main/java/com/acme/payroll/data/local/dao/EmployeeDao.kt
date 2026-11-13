package com.acme.payroll.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.data.local.entity.EmployeeWithDepartment
import com.acme.payroll.data.local.entity.SalaryHistoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EmployeeDao {
    @Transaction
    @Query("SELECT * FROM employees ORDER BY last_name, first_name")
    fun observeAllWithDepartment(): Flow<List<EmployeeWithDepartment>>

    @Transaction
    @Query("SELECT * FROM employees WHERE id = :id")
    fun observeWithDepartment(id: Long): Flow<EmployeeWithDepartment?>

    @Query("SELECT * FROM employees WHERE id = :id")
    suspend fun getById(id: Long): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE email = :email COLLATE NOCASE LIMIT 1")
    suspend fun getByEmail(email: String): EmployeeEntity?

    @Query("SELECT * FROM employees WHERE active = 1 ORDER BY last_name")
    suspend fun getActive(): List<EmployeeEntity>

    @Query("SELECT * FROM employees WHERE active = 1 AND pay_type = 'HOURLY' ORDER BY last_name")
    fun observeActiveHourly(): Flow<List<EmployeeEntity>>

    @Query("SELECT COUNT(*) FROM employees WHERE active = 1")
    fun observeActiveCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM employees WHERE active = 1 AND department_id = :departmentId")
    fun observeActiveCountInDepartment(departmentId: Long): Flow<Int>

    @Insert
    suspend fun insert(employee: EmployeeEntity): Long

    @Insert
    suspend fun insertAll(employees: List<EmployeeEntity>): List<Long>

    @Update
    suspend fun update(employee: EmployeeEntity)

    @Query("UPDATE employees SET active = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("SELECT * FROM salary_history WHERE employee_id = :employeeId ORDER BY effective_date DESC")
    fun observeSalaryHistory(employeeId: Long): Flow<List<SalaryHistoryEntity>>

    @Insert
    suspend fun insertSalaryHistory(entry: SalaryHistoryEntity): Long
}
