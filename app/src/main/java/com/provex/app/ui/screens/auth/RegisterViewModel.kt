package com.provex.app.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.repository.AuthRepository
import com.provex.app.domain.usecase.Validators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val fullName: String = "",
    val fullNameError: String? = null,
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val confirmPassword: String = "",
    val confirmPasswordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val isBackendConfigured: Boolean = false,
)

sealed interface RegisterNavigationEvent {
    data object NavigateToHome : RegisterNavigationEvent
    data object NavigateToLogin : RegisterNavigationEvent
}

class RegisterViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        RegisterUiState(isBackendConfigured = authRepository.backendConfigured)
    )
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<RegisterNavigationEvent>()
    val navigationEvents: SharedFlow<RegisterNavigationEvent> = _navigationEvents.asSharedFlow()

    fun onFullNameChanged(name: String) {
        _uiState.update { it.copy(fullName = name, fullNameError = null, generalError = null) }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                confirmPasswordError = if (it.confirmPassword.isNotEmpty()) {
                    Validators.confirmPasswordError(password, it.confirmPassword)
                } else null,
                generalError = null,
            )
        }
    }

    fun onConfirmPasswordChanged(confirm: String) {
        _uiState.update {
            it.copy(
                confirmPassword = confirm,
                confirmPasswordError = null,
                generalError = null,
            )
        }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onToggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun onCreateAccount() {
        val current = _uiState.value
        if (current.isLoading) return

        val nameErr = Validators.nameError(current.fullName)
        val emailErr = Validators.emailError(current.email)
        val passErr = Validators.passwordError(current.password)
        val confirmErr = Validators.confirmPasswordError(current.password, current.confirmPassword)

        if (nameErr != null || emailErr != null || passErr != null || confirmErr != null) {
            _uiState.update {
                it.copy(
                    fullNameError = nameErr,
                    emailError = emailErr,
                    passwordError = passErr,
                    confirmPasswordError = confirmErr,
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = authRepository.register(current.email, current.password)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvents.emit(RegisterNavigationEvent.NavigateToHome)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = err.message ?: "Account registration failed.",
                        )
                    }
                },
            )
        }
    }

    fun onContinueDemoMode() {
        viewModelScope.launch {
            val current = _uiState.value
            authRepository.enterDemoMode(
                name = current.fullName.trim().takeIf { it.isNotBlank() },
                email = current.email.trim().takeIf { it.isNotBlank() },
            )
            _navigationEvents.emit(RegisterNavigationEvent.NavigateToHome)
        }
    }

    fun onNavigateToLogin() {
        viewModelScope.launch {
            _navigationEvents.emit(RegisterNavigationEvent.NavigateToLogin)
        }
    }
}
