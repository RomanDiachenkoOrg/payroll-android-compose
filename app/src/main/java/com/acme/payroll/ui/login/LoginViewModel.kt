package com.acme.payroll.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.acme.payroll.data.repository.AuthRepository
import com.acme.payroll.data.repository.LoginResult
import com.acme.payroll.domain.validation.EmployeeValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Named

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val error: String? = null,
    val loading: Boolean = false,
    val signedIn: Boolean = false,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    @Named("lastEmail") lastEmail: Flow<String>,
) : ViewModel() {

    private val _state = MutableStateFlow(LoginUiState())
    val state: StateFlow<LoginUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val remembered = lastEmail.first()
            if (remembered.isNotEmpty() && _state.value.email.isEmpty()) {
                _state.update { it.copy(email = remembered) }
            }
        }
    }

    fun onEmailChange(value: String) = _state.update { it.copy(email = value, emailError = null, error = null) }

    fun onPasswordChange(value: String) = _state.update { it.copy(password = value, passwordError = null, error = null) }

    fun submit() {
        val current = _state.value
        if (current.loading) return
        val email = current.email.trim()
        val emailError = when {
            email.isEmpty() -> "Email is required"
            !EmployeeValidator.isValidEmail(email) -> "Enter a valid email address"
            else -> null
        }
        val passwordError = if (current.password.isEmpty()) "Password is required" else null
        if (emailError != null || passwordError != null) {
            _state.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            when (authRepository.login(email, current.password)) {
                is LoginResult.Success -> _state.update { it.copy(loading = false, signedIn = true, password = "") }
                LoginResult.InvalidCredentials -> _state.update {
                    it.copy(loading = false, error = "Incorrect email or password", password = "")
                }
            }
        }
    }
}
