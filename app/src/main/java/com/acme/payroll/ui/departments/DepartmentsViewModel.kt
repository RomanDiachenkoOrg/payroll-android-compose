package com.acme.payroll.ui.departments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.DepartmentEntity
import com.acme.payroll.data.local.entity.DepartmentSummary
import com.acme.payroll.data.repository.DepartmentRepository
import com.acme.payroll.data.repository.DepartmentResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DepartmentsViewModel @Inject constructor(
    private val repository: DepartmentRepository,
) : ViewModel() {

    val departments: StateFlow<List<DepartmentSummary>?> = repository.observeSummaries()
        .map<List<DepartmentSummary>, List<DepartmentSummary>?> { it }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    /** Saves and reports the error back to the dialog; null means success. */
    fun save(id: Long, name: String, code: String, costCenter: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            when (val r = repository.save(DepartmentEntity(id = id, name = name, code = code, costCenter = costCenter))) {
                DepartmentResult.Ok -> {
                    onResult(null)
                    _messages.emit(if (id == 0L) "Department created" else "Department updated")
                }
                is DepartmentResult.Error -> onResult(r.message)
            }
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            when (val r = repository.delete(id)) {
                DepartmentResult.Ok -> _messages.emit("Department deleted")
                is DepartmentResult.Error -> _messages.emit(r.message)
            }
        }
    }
}
