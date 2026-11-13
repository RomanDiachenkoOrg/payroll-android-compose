package com.acme.payroll.ui.timesheets

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.TimesheetDetails
import com.acme.payroll.data.repository.TimesheetRepository
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import com.acme.payroll.domain.model.TimesheetStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class TimesheetListUiState(
    val loading: Boolean = true,
    val statusFilter: TimesheetStatus? = null,
    val items: List<TimesheetDetails> = emptyList(),
    val counts: Map<TimesheetStatus, Int> = emptyMap(),
)

@HiltViewModel
class TimesheetListViewModel @Inject constructor(
    sessionStore: SessionStore,
    repository: TimesheetRepository,
) : ViewModel() {

    private val statusFilter = MutableStateFlow<TimesheetStatus?>(null)

    val state: StateFlow<TimesheetListUiState> = combine(
        sessionStore.session.filterNotNull(),
        repository.observeAll(),
        statusFilter,
    ) { session, all, filter ->
        val visible = all.filter { isVisibleTo(it, session) }
        TimesheetListUiState(
            loading = false,
            statusFilter = filter,
            items = visible.filter { filter == null || it.timesheet.status == filter },
            counts = visible.groupingBy { it.timesheet.status }.eachCount(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TimesheetListUiState())

    fun setFilter(status: TimesheetStatus?) {
        statusFilter.value = status
    }

    private fun isVisibleTo(t: TimesheetDetails, session: Session): Boolean = when (session.role) {
        Role.ADMIN -> true
        Role.MANAGER -> t.employee.departmentId == session.departmentId
        Role.EMPLOYEE -> t.employee.id == session.employeeId
    }
}
