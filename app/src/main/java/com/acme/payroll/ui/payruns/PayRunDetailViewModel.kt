package com.acme.payroll.ui.payruns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.PayRunEntity
import com.acme.payroll.data.local.entity.PayslipDetails
import com.acme.payroll.data.repository.PayRunException
import com.acme.payroll.data.repository.PayRunRepository
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.model.PayRunStatus
import com.acme.payroll.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PayRunDetailUiState(
    val loading: Boolean = true,
    val run: PayRunEntity? = null,
    val lines: List<PayslipDetails> = emptyList(),
    val busy: Boolean = false,
) {
    val grossCents: Long get() = lines.sumOf { it.payslip.grossCents }
    val taxCents: Long get() = lines.sumOf { it.payslip.taxCents }
    val deductionsCents: Long get() = lines.sumOf { it.payslip.socialCents + it.payslip.pensionCents + it.payslip.otherCents }
    val netCents: Long get() = lines.sumOf { it.payslip.netCents }

    val canCalculate: Boolean get() = run?.status == PayRunStatus.DRAFT || run?.status == PayRunStatus.CALCULATED
    val canApprove: Boolean get() = run?.status == PayRunStatus.CALCULATED && lines.isNotEmpty()
    val canMarkPaid: Boolean get() = run != null && run.status != PayRunStatus.DRAFT && run.status != PayRunStatus.PAID
}

@HiltViewModel
class PayRunDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionStore: SessionStore,
    private val repository: PayRunRepository,
) : ViewModel() {

    private val runId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_RUN_ID))
    private val busy = MutableStateFlow(false)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    val state: StateFlow<PayRunDetailUiState> = combine(
        repository.observeRun(runId),
        repository.observePayslips(runId),
        busy,
    ) { run, lines, isBusy ->
        PayRunDetailUiState(loading = false, run = run, lines = lines, busy = isBusy)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PayRunDetailUiState())

    fun calculate() = runAction("Pay run calculated") { repository.calculate(runId) }

    fun approve() = runAction("Pay run approved") {
        repository.approve(runId, sessionStore.current()?.email ?: "unknown")
    }

    fun markPaid() = runAction("Pay run marked as paid and locked") { repository.markPaid(runId) }

    private fun runAction(success: String, block: suspend () -> Unit) {
        if (busy.value) return
        busy.value = true
        viewModelScope.launch {
            try {
                block()
                _messages.emit(success)
            } catch (e: PayRunException) {
                _messages.emit(e.message ?: "Action failed")
            } finally {
                busy.value = false
            }
        }
    }
}
