package com.acme.payroll.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.AuditLogEntity
import com.acme.payroll.data.repository.AuditRepository
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.data.repository.PayRunRepository
import com.acme.payroll.data.repository.TimesheetRepository
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.model.PayRunStatus
import com.acme.payroll.domain.model.Role
import com.acme.payroll.util.Dates
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

data class PayrollBar(val label: String, val grossCents: Long)

data class DashboardUiState(
    val loading: Boolean = true,
    val greetingName: String = "",
    val headcount: Int = 0,
    val headcountLabel: String = "Active employees",
    val lastPayrollGrossCents: Long? = null,
    val lastPayrollLabel: String = "",
    val pendingTimesheets: Int = 0,
    val nextPayDate: LocalDate? = null,
    val nextRunId: Long? = null,
    val history: List<PayrollBar> = emptyList(),
    val recentActivity: List<AuditLogEntity> = emptyList(),
    val myLastNetCents: Long? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class DashboardViewModel @Inject constructor(
    sessionStore: SessionStore,
    employeeRepository: EmployeeRepository,
    payRunRepository: PayRunRepository,
    timesheetRepository: TimesheetRepository,
    auditRepository: AuditRepository,
) : ViewModel() {

    private val session = sessionStore.session.filterNotNull()

    private val headcount = combine(session, employeeRepository.observeEmployees()) { s, all ->
        if (s.role == Role.MANAGER && s.departmentId != null) {
            all.count { it.employee.active && it.employee.departmentId == s.departmentId }
        } else {
            all.count { it.employee.active }
        }
    }

    private val pending = session.flatMapLatest { s ->
        timesheetRepository.observePendingCount(if (s.role == Role.MANAGER) s.departmentId else null)
    }

    private val lastPaid = payRunRepository.observeLastPaid().flatMapLatest { run ->
        if (run == null) flowOf(null to "")
        else payRunRepository.observeGrossTotal(run.id).map { it to YearMonth.from(run.periodStart).format(Dates.monthDisplay) }
    }

    private val history = payRunRepository.observeSummaries().map { runs ->
        runs.filter { it.run.status == PayRunStatus.PAID || it.run.status == PayRunStatus.APPROVED }
            .take(6)
            .reversed()
            .map { PayrollBar(it.run.periodStart.month.name.take(3).lowercase().replaceFirstChar(Char::uppercase), it.grossCents) }
    }

    private val myLastNet = session.flatMapLatest { s ->
        val id = s.employeeId
        if (id == null) flowOf(null) else payRunRepository.observePayslipsForEmployee(id).map { it.firstOrNull()?.payslip?.netCents }
    }

    private val audit = session.flatMapLatest { s ->
        if (s.role == Role.ADMIN) auditRepository.observeRecent().map { it.take(5) } else flowOf(emptyList())
    }

    val state: StateFlow<DashboardUiState> = combine(
        combine(session, headcount, pending) { s, h, p -> Triple(s, h, p) },
        lastPaid,
        payRunRepository.observeNextOpen(),
        history,
        combine(myLastNet, audit) { net, log -> net to log },
    ) { (s, h, p), (gross, label), next, bars, (net, log) ->
        DashboardUiState(
            loading = false,
            greetingName = s.displayName.substringBefore(' '),
            headcount = h,
            headcountLabel = if (s.role == Role.MANAGER) "Active in your department" else "Active employees",
            lastPayrollGrossCents = gross,
            lastPayrollLabel = label,
            pendingTimesheets = p,
            nextPayDate = next?.payDate ?: Dates.payDateFor(YearMonth.now()),
            nextRunId = next?.id,
            history = bars,
            recentActivity = log,
            myLastNetCents = net,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardUiState())
}
