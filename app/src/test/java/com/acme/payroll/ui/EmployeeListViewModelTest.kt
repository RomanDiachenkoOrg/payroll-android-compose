package com.acme.payroll.ui

import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.data.local.entity.EmployeeWithDepartment
import com.acme.payroll.data.local.entity.SalaryHistoryEntity
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.ui.employees.EmployeeFilters
import com.acme.payroll.ui.employees.EmployeeListViewModel
import com.acme.payroll.ui.employees.EmployeeSort
import com.acme.payroll.ui.employees.StatusFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.LocalDate

class EmployeeListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val eng = DepartmentEntity(1, "Engineering", "ENG", "CC-100")
    private val fin = DepartmentEntity(2, "Finance", "FIN", "CC-200")

    private fun employee(id: Long, last: String, dept: DepartmentEntity, active: Boolean = true) = EmployeeWithDepartment(
        EmployeeEntity(
            id = id,
            firstName = "Person$id",
            lastName = last,
            email = "p$id@acme.test",
            jobTitle = "Engineer",
            departmentId = dept.id,
            payType = PayType.SALARIED,
            annualSalaryCents = 5_000_000 + id,
            hireDate = LocalDate.of(2020, 1, 1).plusDays(id),
            active = active,
        ),
        dept,
    )

    private class FakeRepo(list: List<EmployeeWithDepartment>) : EmployeeRepository {
        val data = MutableStateFlow(list)
        override fun observeEmployees(): Flow<List<EmployeeWithDepartment>> = data
        override fun observeEmployee(id: Long): Flow<EmployeeWithDepartment?> =
            data.map { l -> l.firstOrNull { it.employee.id == id } }
        override fun observeSalaryHistory(employeeId: Long): Flow<List<SalaryHistoryEntity>> = flowOf(emptyList())
        override suspend fun getEmployee(id: Long) = data.value.firstOrNull { it.employee.id == id }?.employee
        override suspend fun isEmailTaken(email: String, excludeId: Long?) = false
        override suspend fun save(employee: EmployeeEntity) = employee.id
        override suspend fun setActive(id: Long, active: Boolean) = Unit
        override suspend fun addRaise(employeeId: Long, newAmountCents: Long, effectiveDate: LocalDate, reason: String) = Unit
    }

    private val sample = (1L..23L).map { i ->
        employee(i, "Name%02d".format(i), if (i % 2 == 0L) fin else eng, active = i != 5L)
    }

    @Test
    fun `default filter shows active employees paged by ten`() = runTest {
        val vm = EmployeeListViewModel(FakeRepo(sample))
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.state.collect {} }
        val s = vm.state.value
        assertEquals(22, s.totalCount)
        assertEquals(3, s.pageCount)
        assertEquals(10, s.items.size)
        assertFalse(s.hasPrevious)
        assertTrue(s.hasNext)

        vm.nextPage()
        vm.nextPage()
        assertEquals(2, vm.state.value.items.size)
        assertFalse(vm.state.value.hasNext)
    }

    @Test
    fun `status and department filters`() {
        val inactive = EmployeeListViewModel.applyFilters(sample, EmployeeFilters(status = StatusFilter.INACTIVE))
        assertEquals(listOf(5L), inactive.map { it.employee.id })

        val finance = EmployeeListViewModel.applyFilters(sample, EmployeeFilters(departmentId = 2, status = StatusFilter.ALL))
        assertEquals(11, finance.size)
        assertTrue(finance.all { it.department.name == "Finance" })
    }

    @Test
    fun `search matches name case-insensitively`() {
        val result = EmployeeListViewModel.applyFilters(sample, EmployeeFilters(query = "name1"))
        assertEquals((10L..19L).toList(), result.map { it.employee.id })
    }

    @Test
    fun `sort by hire date puts newest first`() {
        val result = EmployeeListViewModel.applyFilters(sample, EmployeeFilters(sort = EmployeeSort.HIRE_DATE))
        assertEquals(23L, result.first().employee.id)
    }
}
