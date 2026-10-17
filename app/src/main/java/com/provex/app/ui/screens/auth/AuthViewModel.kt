package com.provex.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.repository.AuthRepository
import com.provex.app.domain.usecase.Validators
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isRegistering: Boolean = false,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isBackendConfigured: Boolean = false,
)

class AuthViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        AuthUiState(isBackendConfigured = authRepository.backendConfigured)
    )
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    val authState: StateFlow<AuthState?> = MutableStateFlow<AuthState?>(null).apply {
        viewModelScope.launch {
            authRepository.authState.collect { value = it }
        }
    }

    fun onToggleMode() {
        _uiState.update {
            it.copy(
                isRegistering = !it.isRegistering,
                emailError = null,
                passwordError = null,
                generalError = null,
            )
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onSubmit() {
        val current = _uiState.value
        val emailErr = Validators.emailError(current.email)
        val passErr = Validators.passwordError(current.password)

        if (emailErr != null || passErr != null) {
            _uiState.update { it.copy(emailError = emailErr, passwordError = passErr) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = if (current.isRegistering) {
                authRepository.register(current.email.substringBefore('@'), current.email, current.password)
            } else {
                authRepository.signIn(current.email, current.password)
            }
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = err.message ?: "Authentication failed.",
                        )
                    }
                },
            )
        }
    }

    fun onEnterDemo() {
        viewModelScope.launch {
            authRepository.enterDemoMode()
        }
    }
}
