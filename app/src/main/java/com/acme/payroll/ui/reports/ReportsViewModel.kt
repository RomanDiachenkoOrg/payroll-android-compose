package com.acme.payroll.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.DepartmentCostRow
import com.acme.payroll.data.repository.ReportRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.YearMonth
import javax.inject.Inject

data class ReportsUiState(
    val from: YearMonth = YearMonth.now().minusMonths(2),
    val to: YearMonth = YearMonth.now(),
    val rows: List<DepartmentCostRow> = emptyList(),
    val loading: Boolean = true,
    val error: String? = null,
) {
    val totalGross: Long get() = rows.sumOf { it.grossCents }
    val totalNet: Long get() = rows.sumOf { it.netCents }
    val totalEmployerCost: Long get() = rows.sumOf { it.grossCents + it.socialCents }
}

@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val repository: ReportRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsUiState())
    val state: StateFlow<ReportsUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun setFrom(month: YearMonth) {
        _state.update { it.copy(from = month) }
        reload()
    }

    fun setTo(month: YearMonth) {
        _state.update { it.copy(to = month) }
        reload()
    }

    private fun reload() {
        val s = _state.value
        if (s.from.isAfter(s.to)) {
            _state.update { it.copy(rows = emptyList(), loading = false, error = "Start month must be before end month") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val rows = repository.costByDepartment(s.from, s.to)
            _state.update { it.copy(rows = rows, loading = false) }
        }
    }

    fun csv(): String = repository.toCsv(_state.value.rows)
}
