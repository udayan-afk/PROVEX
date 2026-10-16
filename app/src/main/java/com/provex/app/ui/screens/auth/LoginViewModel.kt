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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val email: String = "",
    val emailError: String? = null,
    val password: String = "",
    val passwordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val rememberMe: Boolean = true,
    val isLoading: Boolean = false,
    val generalError: String? = null,
    val showForgotPasswordDialog: Boolean = false,
    val isBackendConfigured: Boolean = false,
)

sealed interface LoginNavigationEvent {
    data object NavigateToHome : LoginNavigationEvent
    data object NavigateToRegister : LoginNavigationEvent
}

class LoginViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(isBackendConfigured = authRepository.backendConfigured)
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<LoginNavigationEvent>()
    val navigationEvents: SharedFlow<LoginNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            val remember = authRepository.rememberMe.first()
            _uiState.update { it.copy(rememberMe = remember) }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, generalError = null) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, generalError = null) }
    }

    fun onTogglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun onRememberMeChanged(checked: Boolean) {
        _uiState.update { it.copy(rememberMe = checked) }
        viewModelScope.launch {
            authRepository.setRememberMe(checked)
        }
    }

    fun onForgotPasswordClicked() {
        _uiState.update { it.copy(showForgotPasswordDialog = true) }
    }

    fun onDismissForgotPasswordDialog() {
        _uiState.update { it.copy(showForgotPasswordDialog = false) }
    }

    fun onSignIn() {
        val current = _uiState.value
        if (current.isLoading) return

        val emailErr = Validators.emailError(current.email)
        val passErr = Validators.loginPasswordError(current.password)

        if (emailErr != null || passErr != null) {
            _uiState.update { it.copy(emailError = emailErr, passwordError = passErr) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = authRepository.signIn(current.email, current.password, current.rememberMe)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    _navigationEvents.emit(LoginNavigationEvent.NavigateToHome)
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            generalError = err.message ?: "Sign-in failed. Please verify credentials.",
                        )
                    }
                },
            )
        }
    }

    fun onEnterDemoMode() {
        viewModelScope.launch {
            authRepository.enterDemoMode(
                name = null,
                email = _uiState.value.email.takeIf { it.isNotBlank() },
            )
            _navigationEvents.emit(LoginNavigationEvent.NavigateToHome)
        }
    }

    fun onNavigateToRegister() {
        viewModelScope.launch {
            _navigationEvents.emit(LoginNavigationEvent.NavigateToRegister)
        }
    }
}
