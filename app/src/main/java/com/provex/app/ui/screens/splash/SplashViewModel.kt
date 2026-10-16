package com.provex.app.ui.screens.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.repository.AuthRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed interface SplashNavigationTarget {
    data object Home : SplashNavigationTarget
    data object Login : SplashNavigationTarget
}

class SplashViewModel(
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _navigationEvent = MutableSharedFlow<SplashNavigationTarget>()
    val navigationEvent: SharedFlow<SplashNavigationTarget> = _navigationEvent.asSharedFlow()

    init {
        determineStartupDestination()
    }

    private fun determineStartupDestination() {
        viewModelScope.launch {
            // Briefly hold splash (1.5s) to display PROVEX identity and check session
            val startTime = System.currentTimeMillis()
            val state = authRepository.authState.first()
            val elapsed = System.currentTimeMillis() - startTime
            val remaining = (1500L - elapsed).coerceAtLeast(0L)
            delay(remaining)

            when (state) {
                is AuthState.Account, is AuthState.Demo -> {
                    _navigationEvent.emit(SplashNavigationTarget.Home)
                }
                AuthState.SignedOut -> {
                    _navigationEvent.emit(SplashNavigationTarget.Login)
                }
            }
        }
    }
}
