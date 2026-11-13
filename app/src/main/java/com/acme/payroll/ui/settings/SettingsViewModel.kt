package com.acme.payroll.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.local.entity.AuditLogEntity
import com.acme.payroll.data.local.entity.UserEntity
import com.acme.payroll.data.repository.AuditRepository
import com.acme.payroll.data.repository.SettingsRepository
import com.acme.payroll.data.session.AppSettings
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.payroll.PayrollRates
import com.acme.payroll.domain.payroll.TaxBand
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.math.BigDecimal
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val sessionStore: SessionStore,
) : ViewModel() {
    val settings: StateFlow<AppSettings> =
        sessionStore.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    fun setRememberEmail(enabled: Boolean) = viewModelScope.launch { sessionStore.setRememberEmail(enabled) }
    fun setDarkTheme(enabled: Boolean) = viewModelScope.launch { sessionStore.setDarkTheme(enabled) }
}

/** Editable copy of the tax configuration; amounts kept as text while the user types. */
data class TaxBandRow(val from: String, val upTo: String, val rate: String)

data class TaxSettingsUiState(
    val loading: Boolean = true,
    val bands: List<TaxBandRow> = emptyList(),
    val socialPercent: String = "",
    val pensionPercent: String = "",
    val error: String? = null,
)

@HiltViewModel
class TaxSettingsViewModel @Inject constructor(
    private val repository: SettingsRepository,
) : ViewModel() {
    val state = MutableStateFlow(TaxSettingsUiState())

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages: SharedFlow<String> = _messages

    init {
        viewModelScope.launch {
            val rates = repository.observeRates().first()
            state.value = TaxSettingsUiState(
                loading = false,
                bands = rates.taxBands.map { TaxBandRow(it.from.toPlainString(), it.upTo?.toPlainString().orEmpty(), it.ratePercent.toPlainString()) },
                socialPercent = rates.socialPercent.toPlainString(),
                pensionPercent = rates.pensionPercent.toPlainString(),
            )
        }
    }

    fun updateBand(index: Int, row: TaxBandRow) {
        val s = state.value
        state.value = s.copy(bands = s.bands.toMutableList().also { it[index] = row }, error = null)
    }

    fun addBand() {
        val s = state.value
        state.value = s.copy(bands = s.bands + TaxBandRow(s.bands.lastOrNull()?.upTo.orEmpty(), "", ""), error = null)
    }

    fun removeLastBand() {
        val s = state.value
        if (s.bands.size > 1) state.value = s.copy(bands = s.bands.dropLast(1), error = null)
    }

    fun setSocial(v: String) { state.value = state.value.copy(socialPercent = v, error = null) }
    fun setPension(v: String) { state.value = state.value.copy(pensionPercent = v, error = null) }

    fun save() {
        val s = state.value
        val bands = s.bands.map { row ->
            TaxBand(
                from = row.from.toBigDecimalOrNull() ?: return fail("Band start must be a number"),
                upTo = if (row.upTo.isBlank()) null else row.upTo.toBigDecimalOrNull() ?: return fail("Band end must be a number"),
                ratePercent = row.rate.toBigDecimalOrNull() ?: return fail("Rate must be a number"),
            )
        }
        SettingsRepository.validateBands(bands)?.let { return fail(it) }
        val social = s.socialPercent.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal(100) }
            ?: return fail("Social contribution must be 0-100")
        val pension = s.pensionPercent.toBigDecimalOrNull()?.takeIf { it >= BigDecimal.ZERO && it <= BigDecimal(100) }
            ?: return fail("Pension must be 0-100")
        viewModelScope.launch {
            repository.saveRates(PayrollRates(bands, social, pension))
            _messages.emit("Tax settings saved. They apply to the next calculation.")
        }
    }

    private fun fail(message: String) {
        state.value = state.value.copy(error = message)
    }
}

@HiltViewModel
class UsersViewModel @Inject constructor(repository: SettingsRepository) : ViewModel() {
    val users: StateFlow<List<UserEntity>> =
        repository.observeUsers().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
}

data class AuditUiState(
    val query: String = "",
    val action: String? = null,
    val actions: List<String> = emptyList(),
    val entries: List<AuditLogEntity> = emptyList(),
)

@HiltViewModel
class AuditLogViewModel @Inject constructor(repository: AuditRepository) : ViewModel() {
    private val query = MutableStateFlow("")
    private val action = MutableStateFlow<String?>(null)

    val state: StateFlow<AuditUiState> = combine(repository.observeRecent(), query, action) { all, q, a ->
        AuditUiState(
            query = q,
            action = a,
            actions = all.map { it.action }.distinct().sorted(),
            entries = all.filter { e ->
                (a == null || e.action == a) &&
                    (q.isBlank() || e.details.contains(q, true) || e.userEmail.contains(q, true) || e.entity.contains(q, true))
            },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuditUiState())

    fun setQuery(q: String) { query.value = q }
    fun setAction(a: String?) { action.value = a }
}
