package com.acme.payroll.ui.employees

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.EmployeeEntity
import com.acme.payroll.data.repository.DepartmentRepository
import com.acme.payroll.data.repository.EmployeeRepository
import com.acme.payroll.domain.model.PayType
import com.acme.payroll.domain.payroll.Money
import com.acme.payroll.domain.validation.EmployeeField
import com.acme.payroll.domain.validation.EmployeeForm
import com.acme.payroll.domain.validation.EmployeeValidator
import com.acme.payroll.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject

data class EmployeeEditUiState(
    val loading: Boolean = true,
    val isNew: Boolean = true,
    val form: EmployeeForm = EmployeeForm(),
    val errors: Map<EmployeeField, String> = emptyMap(),
    val saving: Boolean = false,
    val savedId: Long? = null,
    val active: Boolean = true,
)

@HiltViewModel
class EmployeeEditViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    departmentRepository: DepartmentRepository,
    private val employeeRepository: EmployeeRepository,
    private val clock: Clock,
) : ViewModel() {

    private val employeeId: Long = savedStateHandle.get<Long>(Routes.ARG_EMPLOYEE_ID) ?: 0L
    private var original: EmployeeEntity? = null

    private val _state = MutableStateFlow(EmployeeEditUiState(isNew = employeeId == 0L))
    val state: StateFlow<EmployeeEditUiState> = _state.asStateFlow()

    val departments: StateFlow<List<DepartmentEntity>> =
        departmentRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            if (employeeId == 0L) {
                _state.update { it.copy(loading = false, form = EmployeeForm(hireDate = LocalDate.now(clock))) }
            } else {
                val e = employeeRepository.getEmployee(employeeId)
                original = e
                _state.update {
                    it.copy(
                        loading = false,
                        active = e?.active ?: true,
                        form = if (e == null) EmployeeForm() else EmployeeForm(
                            firstName = e.firstName,
                            lastName = e.lastName,
                            email = e.email,
                            phone = e.phone,
                            jobTitle = e.jobTitle,
                            departmentId = e.departmentId,
                            payType = e.payType,
                            amount = Money.fromCents(
                                (if (e.payType == PayType.SALARIED) e.annualSalaryCents else e.hourlyRateCents) ?: 0
                            ).toPlainString(),
                            hireDate = e.hireDate,
                            pensionOptIn = e.pensionOptIn,
                            otherDeduction = Money.fromCents(e.otherDeductionCents).toPlainString(),
                        ),
                    )
                }
            }
        }
    }

    fun update(transform: (EmployeeForm) -> EmployeeForm) {
        _state.update { s ->
            val newForm = transform(s.form)
            // Clear errors only for fields that changed.
            val cleared = s.errors.filterKeys { field -> fieldValue(s.form, field) == fieldValue(newForm, field) }
            s.copy(form = newForm, errors = cleared)
        }
    }

    private fun fieldValue(form: EmployeeForm, field: EmployeeField): Any? = when (field) {
        EmployeeField.FIRST_NAME -> form.firstName
        EmployeeField.LAST_NAME -> form.lastName
        EmployeeField.EMAIL -> form.email
        EmployeeField.DEPARTMENT -> form.departmentId
        EmployeeField.AMOUNT -> form.amount to form.payType
        EmployeeField.HIRE_DATE -> form.hireDate
        EmployeeField.OTHER_DEDUCTION -> form.otherDeduction
    }

    fun save() {
        val current = _state.value
        if (current.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val form = current.form
            val taken = form.email.isNotBlank() &&
                employeeRepository.isEmailTaken(form.email, excludeId = employeeId.takeIf { it != 0L })
            val errors = EmployeeValidator.validate(form, LocalDate.now(clock), taken)
            if (errors.isNotEmpty()) {
                _state.update { it.copy(saving = false, errors = errors) }
                return@launch
            }
            val cents = Money.toCents(Money.parse(form.amount)!!)
            val entity = EmployeeEntity(
                id = employeeId,
                firstName = form.firstName.trim(),
                lastName = form.lastName.trim(),
                email = form.email.trim().lowercase(),
                phone = form.phone.trim(),
                jobTitle = form.jobTitle.trim().ifEmpty { "Staff" },
                departmentId = form.departmentId!!,
                payType = form.payType,
                annualSalaryCents = cents.takeIf { form.payType == PayType.SALARIED },
                hourlyRateCents = cents.takeIf { form.payType == PayType.HOURLY },
                hireDate = form.hireDate!!,
                active = original?.active ?: true,
                pensionOptIn = form.pensionOptIn,
                otherDeductionCents = Money.toCents(Money.parse(form.otherDeduction.ifBlank { "0" })!!),
            )
            val id = employeeRepository.save(entity)
            _state.update { it.copy(saving = false, savedId = id) }
        }
    }
}
