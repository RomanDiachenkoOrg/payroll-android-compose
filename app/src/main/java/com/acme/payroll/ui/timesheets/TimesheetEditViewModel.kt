package com.acme.payroll.ui.timesheets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.data.repository.TimesheetException
import com.acme.payroll.data.repository.TimesheetRepository
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.model.Role
import com.acme.payroll.domain.model.Session
import com.acme.payroll.domain.model.TimesheetStatus
import com.acme.payroll.domain.validation.TimesheetValidator
import com.acme.payroll.ui.navigation.Routes
import com.acme.payroll.util.Dates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class TimesheetEditUiState(
    val loading: Boolean = true,
    val id: Long? = null,
    val employeeId: Long? = null,
    val employeeName: String = "",
    val employeeOptions: List<Pair<Long, String>> = emptyList(),
    val weekStart: LocalDate = LocalDate.now(),
    val hours: List<String> = List(7) { "0" },
    val notes: String = "",
    val status: TimesheetStatus = TimesheetStatus.DRAFT,
    val rejectReason: String? = null,
    val decidedBy: String? = null,
    val hourErrors: Map<Int, String> = emptyMap(),
    val error: String? = null,
    val busy: Boolean = false,
    val canEdit: Boolean = true,
    val canDecide: Boolean = false,
) {
    val total: BigDecimal get() = TimesheetValidator.total(hours)
    val canSubmit: Boolean get() = canEdit && (status == TimesheetStatus.DRAFT || status == TimesheetStatus.REJECTED)
}

@HiltViewModel
class TimesheetEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionStore: SessionStore,
    private val repository: TimesheetRepository,
    private val employeeRepository: EmployeeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val timesheetId: Long = savedStateHandle.get<Long>(Routes.ARG_TIMESHEET_ID) ?: 0L

    private val _state = MutableStateFlow(TimesheetEditUiState())
    val state: StateFlow<TimesheetEditUiState> = _state.asStateFlow()

    private val _events = MutableSharedFlow<String>(extraBufferCapacity = 1)
    /** Emits a message and asks the screen to close. */
    val closeEvents: SharedFlow<String> = _events

    private lateinit var session: Session

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        session = sessionStore.session.first() ?: return
        if (timesheetId == 0L) {
            val options = when (session.role) {
                Role.ADMIN -> employeeRepository.observeEmployees().first()
                    .filter { it.employee.active }
                    .map { it.employee.id to it.employee.fullName }
                else -> emptyList()
            }
            val ownId = session.employeeId
            _state.update {
                it.copy(
                    loading = false,
                    employeeId = ownId ?: options.firstOrNull()?.first,
                    employeeName = session.displayName,
                    employeeOptions = options,
                    weekStart = Dates.weekStart(LocalDate.now(clock)),
                )
            }
            return
        }
        val details = repository.get(timesheetId)
        if (details == null) {
            _state.update { it.copy(loading = false, error = "Timesheet not found", canEdit = false) }
            return
        }
        val t = details.timesheet
        val byDay = (0 until 7).map { day ->
            details.entries.firstOrNull { it.workDate == t.weekStart.plusDays(day.toLong()) }?.hours ?: BigDecimal.ZERO
        }
        val isOwner = details.employee.id == session.employeeId
        val editableStatus = t.status == TimesheetStatus.DRAFT || t.status == TimesheetStatus.REJECTED
        val canDecide = t.status == TimesheetStatus.SUBMITTED && !isOwner && when (session.role) {
            Role.ADMIN -> true
            Role.MANAGER -> details.employee.departmentId == session.departmentId
            Role.EMPLOYEE -> false
        }
        _state.update {
            it.copy(
                loading = false,
                id = t.id,
                employeeId = details.employee.id,
                employeeName = details.employee.fullName,
                weekStart = t.weekStart,
                hours = byDay.map { h -> h.stripTrailingZeros().toPlainString() },
                notes = t.notes,
                status = t.status,
                rejectReason = t.rejectReason,
                decidedBy = t.decidedBy,
                canEdit = editableStatus && (isOwner || session.role == Role.ADMIN),
                canDecide = canDecide,
            )
        }
    }

    fun setHours(day: Int, value: String) = _state.update {
        it.copy(hours = it.hours.toMutableList().also { list -> list[day] = value }, hourErrors = it.hourErrors - day, error = null)
    }

    fun setNotes(value: String) = _state.update { it.copy(notes = value) }

    fun setWeek(date: LocalDate) = _state.update { it.copy(weekStart = Dates.weekStart(date), error = null) }

    fun setEmployee(id: Long) = _state.update { it.copy(employeeId = id, error = null) }

    fun saveDraft() = persist(submit = false)

    fun submit() = persist(submit = true)

    private fun persist(submit: Boolean) {
        val s = _state.value
        if (s.busy) return
        val errors = TimesheetValidator.validateHours(s.hours)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(hourErrors = errors) }
            return
        }
        val employeeId = s.employeeId ?: run {
            _state.update { it.copy(error = "Select an employee") }
            return
        }
        if (submit && s.total.signum() == 0) {
            _state.update { it.copy(error = "Enter hours before submitting") }
            return
        }
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val hours = s.hours.map { it.trim().ifEmpty { "0" }.toBigDecimal() }
                val id = repository.saveDraft(s.id, employeeId, s.weekStart, hours, s.notes.trim())
                if (submit) repository.submit(id)
                _events.emit(if (submit) "Timesheet submitted" else "Draft saved")
            } catch (e: TimesheetException) {
                _state.update { it.copy(busy = false, error = e.message) }
            }
        }
    }

    fun approve() = decide { id -> repository.approve(id, session.email); "Timesheet approved" }

    fun reject(reason: String) {
        if (reason.isBlank()) {
            _state.update { it.copy(error = "A reason is required to reject") }
            return
        }
        decide { id -> repository.reject(id, session.email, reason); "Timesheet rejected" }
    }

    private fun decide(action: suspend (Long) -> String) {
        val id = _state.value.id ?: return
        _state.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                _events.emit(action(id))
            } catch (e: TimesheetException) {
                _state.update { it.copy(busy = false, error = e.message) }
            }
        }
    }
}
