package com.acme.payroll.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.repository.AuthRepository
import com.acme.payroll.data.seed.DatabaseSeeder
import com.acme.payroll.data.session.SessionStore
import com.acme.payroll.domain.model.Session
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MainUiState(
    val ready: Boolean = false,
    val session: Session? = null,
    val darkTheme: Boolean = false,
)

@HiltViewModel
class MainViewModel @Inject constructor(
    seeder: DatabaseSeeder,
    sessionStore: SessionStore,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val seeded = MutableStateFlow(false)

    val state: StateFlow<MainUiState> = combine(seeded, sessionStore.session, sessionStore.settings) { ready, session, settings ->
        MainUiState(ready = ready, session = session, darkTheme = settings.darkTheme)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, MainUiState())

    init {
        viewModelScope.launch {
            seeder.seedIfEmpty()
            seeded.value = true
        }
    }

    fun logout() {
        viewModelScope.launch { authRepository.logout() }
    }
}
