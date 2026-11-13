package com.acme.payroll.ui.payruns

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.PayRunSummary
import com.acme.payroll.data.repository.PayRunException
import com.acme.payroll.data.repository.PayRunRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

sealed interface PayRunListEvent {
    data class Created(val id: Long) : PayRunListEvent
    data class Error(val message: String) : PayRunListEvent
}

@HiltViewModel
class PayRunListViewModel @Inject constructor(
    private val repository: PayRunRepository,
) : ViewModel() {

    val runs: StateFlow<List<PayRunSummary>?> = repository.observeSummaries()
        .map<List<PayRunSummary>, List<PayRunSummary>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _events = MutableSharedFlow<PayRunListEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<PayRunListEvent> = _events

    /** Suggests the month after the latest existing run. */
    fun suggestedMonth(): YearMonth =
        runs.value?.maxByOrNull { it.run.periodStart }?.let { YearMonth.from(it.run.periodStart).plusMonths(1) } ?: YearMonth.now()

    fun create(month: YearMonth) {
        viewModelScope.launch {
            try {
                _events.emit(PayRunListEvent.Created(repository.create(month)))
            } catch (e: PayRunException) {
                _events.emit(PayRunListEvent.Error(e.message ?: "Could not create pay run"))
            }
        }
    }
}
