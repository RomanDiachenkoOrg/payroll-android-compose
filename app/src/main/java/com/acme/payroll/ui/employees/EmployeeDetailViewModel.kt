package com.acme.payroll.ui.employees

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.EmployeeWithDepartment
import com.acme.payroll.data.local.entity.PayslipDetails
import com.acme.payroll.data.local.entity.SalaryHistoryEntity
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.data.repository.PayRunRepository
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import java.time.LocalDate
import javax.inject.Inject

data class EmployeeDetailUiState(
    val loading: Boolean = true,
    val employee: EmployeeWithDepartment? = null,
    val salaryHistory: List<SalaryHistoryEntity> = emptyList(),
    val payslips: List<PayslipDetails> = emptyList(),
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class EmployeeDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    sessionStore: SessionStore,
    private val employeeRepository: EmployeeRepository,
    payRunRepository: PayRunRepository,
) : ViewModel() {

    /** Explicit id from the route, or the signed-in employee for the "Profile" tab. */
    private val employeeId = flow {
        val fromRoute = savedStateHandle.get<Long>(Routes.ARG_EMPLOYEE_ID)
        emit(fromRoute ?: sessionStore.current()?.employeeId)
    }

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<EmployeeDetailUiState> = employeeId.flatMapLatest { id ->
        if (id == null) {
            flowOf(EmployeeDetailUiState(loading = false))
        } else {
            combine(
                employeeRepository.observeEmployee(id),
                employeeRepository.observeSalaryHistory(id),
                payRunRepository.observePayslipsForEmployee(id),
            ) { e, history, payslips ->
                EmployeeDetailUiState(loading = false, employee = e, salaryHistory = history, payslips = payslips)
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmployeeDetailUiState())

    fun setActive(active: Boolean) {
        val id = state.value.employee?.employee?.id ?: return
        viewModelScope.launch {
            employeeRepository.setActive(id, active)
            _messages.emit(if (active) "Employee reactivated" else "Employee deactivated")
        }
    }

    /** Returns an error message, or null when the raise was saved. */
    fun addRaise(amountText: String, effectiveDate: LocalDate?, reason: String): String? {
        val employee = state.value.employee?.employee ?: return "Employee not loaded"
        val amount = Money.parse(amountText) ?: return "Enter an amount"
        if (amount <= BigDecimal.ZERO) return "Amount must be greater than 0"
        if (effectiveDate == null) return "Pick an effective date"
        if (reason.isBlank()) return "Reason is required"
        viewModelScope.launch {
            employeeRepository.addRaise(employee.id, Money.toCents(amount), effectiveDate, reason.trim())
            _messages.emit("Pay change saved")
        }
        return null
    }
}
