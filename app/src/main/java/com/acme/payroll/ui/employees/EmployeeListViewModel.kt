package com.acme.payroll.ui.employees

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.EmployeeWithDepartment
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.domain.model.PayType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

enum class StatusFilter(val label: String) { ACTIVE("Active"), INACTIVE("Inactive"), ALL("All") }

enum class EmployeeSort(val label: String) { NAME("Name"), HIRE_DATE("Hire date"), DEPARTMENT("Department"), PAY("Pay") }

data class EmployeeFilters(
    val query: String = "",
    val departmentId: Long? = null,
    val status: StatusFilter = StatusFilter.ACTIVE,
    val sort: EmployeeSort = EmployeeSort.NAME,
    val page: Int = 0,
)

data class EmployeeListUiState(
    val loading: Boolean = true,
    val filters: EmployeeFilters = EmployeeFilters(),
    val departments: List<DepartmentEntity> = emptyList(),
    val items: List<EmployeeWithDepartment> = emptyList(),
    val totalCount: Int = 0,
    val pageCount: Int = 1,
) {
    val hasPrevious: Boolean get() = filters.page > 0
    val hasNext: Boolean get() = filters.page < pageCount - 1
}

@HiltViewModel
class EmployeeListViewModel @Inject constructor(
    repository: EmployeeRepository,
) : ViewModel() {

    private val filters = MutableStateFlow(EmployeeFilters())

    private val departments = repository.observeEmployees().map { list ->
        list.map { it.department }.distinctBy { it.id }.sortedBy { it.name }
    }

    val state: StateFlow<EmployeeListUiState> =
        combine(repository.observeEmployees(), departments, filters) { all, depts, f ->
            val filtered = applyFilters(all, f)
            val pageCount = ((filtered.size + PAGE_SIZE - 1) / PAGE_SIZE).coerceAtLeast(1)
            val page = f.page.coerceIn(0, pageCount - 1)
            EmployeeListUiState(
                loading = false,
                filters = f.copy(page = page),
                departments = depts,
                items = filtered.drop(page * PAGE_SIZE).take(PAGE_SIZE),
                totalCount = filtered.size,
                pageCount = pageCount,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmployeeListUiState())

    fun onQueryChange(query: String) = filters.update { it.copy(query = query, page = 0) }
    fun onDepartmentChange(id: Long?) = filters.update { it.copy(departmentId = id, page = 0) }
    fun onStatusChange(status: StatusFilter) = filters.update { it.copy(status = status, page = 0) }
    fun onSortChange(sort: EmployeeSort) = filters.update { it.copy(sort = sort) }
    fun nextPage() = filters.update { it.copy(page = it.page + 1) }
    fun previousPage() = filters.update { it.copy(page = (it.page - 1).coerceAtLeast(0)) }

    companion object {
        const val PAGE_SIZE = 10

        fun applyFilters(all: List<EmployeeWithDepartment>, f: EmployeeFilters): List<EmployeeWithDepartment> {
            val q = f.query.trim()
            val source = if (q.isEmpty()) {
                all.filter { matchesStatus(it, f.status) }
            } else {
                all.filter { matchesQuery(it, q) }
            }
            val byDepartment = source.filter { f.departmentId == null || it.employee.departmentId == f.departmentId }
            return when (f.sort) {
                EmployeeSort.NAME -> byDepartment.sortedWith(compareBy({ it.employee.lastName }, { it.employee.firstName }))
                EmployeeSort.HIRE_DATE -> byDepartment.sortedByDescending { it.employee.hireDate }
                EmployeeSort.DEPARTMENT -> byDepartment.sortedWith(compareBy({ it.department.name }, { it.employee.lastName }))
                EmployeeSort.PAY -> byDepartment.sortedByDescending { monthlyEquivalent(it) }
            }
        }

        private fun matchesStatus(e: EmployeeWithDepartment, status: StatusFilter) = when (status) {
            StatusFilter.ACTIVE -> e.employee.active
            StatusFilter.INACTIVE -> !e.employee.active
            StatusFilter.ALL -> true
        }

        private fun matchesQuery(e: EmployeeWithDepartment, q: String): Boolean =
            e.employee.fullName.contains(q, ignoreCase = true) ||
                e.employee.email.contains(q, ignoreCase = true) ||
                e.employee.jobTitle.contains(q, ignoreCase = true)

        /** Rough monthly figure used only for sorting: hourly staff assumed at 173 h/month. */
        private fun monthlyEquivalent(e: EmployeeWithDepartment): Long = when (e.employee.payType) {
            PayType.SALARIED -> (e.employee.annualSalaryCents ?: 0) / 12
            PayType.HOURLY -> (e.employee.hourlyRateCents ?: 0) * 173
        }
    }
}
