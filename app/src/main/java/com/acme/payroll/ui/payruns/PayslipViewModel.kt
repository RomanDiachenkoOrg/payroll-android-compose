package com.acme.payroll.ui.payruns

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.PayslipDetails
import com.acme.payroll.data.repository.PayRunRepository
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.ui.navigation.Routes
import com.acme.payroll.util.Csv
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class PayslipUiState(val loading: Boolean = true, val details: PayslipDetails? = null)

@HiltViewModel
class PayslipViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    repository: PayRunRepository,
) : ViewModel() {
    private val payslipId: Long = checkNotNull(savedStateHandle.get<Long>(Routes.ARG_PAYSLIP_ID))

    val state: StateFlow<PayslipUiState> = repository.observePayslip(payslipId)
        .map { PayslipUiState(loading = false, details = it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), PayslipUiState())

    companion object {
        fun fileName(d: PayslipDetails): String =
            "payslip_${d.payRun.periodStart.toString().take(7)}_${d.employee.lastName.lowercase().filter(Char::isLetter)}.csv"

        fun toCsv(d: PayslipDetails): String {
            val p = d.payslip
            return Csv.build(
                header = listOf(
                    "Employee", "Email", "Period start", "Period end", "Pay date",
                    "Regular hours", "Overtime hours", "Gross", "Income tax", "Social", "Pension", "Other", "Net",
                ),
                rows = listOf(
                    listOf(
                        d.employee.fullName, d.employee.email,
                        d.payRun.periodStart, d.payRun.periodEnd, d.payRun.payDate,
                        p.regularHours.toPlainString(), p.overtimeHours.toPlainString(),
                        Money.plain(p.grossCents), Money.plain(p.taxCents), Money.plain(p.socialCents),
                        Money.plain(p.pensionCents), Money.plain(p.otherCents), Money.plain(p.netCents),
                    ),
                ),
            )
        }
    }
}
