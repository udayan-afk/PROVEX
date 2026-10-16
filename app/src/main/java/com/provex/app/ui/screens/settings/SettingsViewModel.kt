package com.provex.app.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.AuthState
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.domain.repository.AuthRepository
import com.provex.app.domain.repository.GithubRepository
import com.provex.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SettingsUiState(
    val authState: AuthState = AuthState.SignedOut,
    val githubState: GithubState = GithubState(
        status = RepoConnectionStatus.NOT_CONNECTED,
        accountLogin = null,
        repos = emptyList(),
        isSample = false,
        message = null,
    ),
    val notifyProofLoop: Boolean = false,
    val notifyLearning: Boolean = false,
    val isClearing: Boolean = false,
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val githubRepository: GithubRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authState.collect { a -> _uiState.update { it.copy(authState = a) } }
        }
        viewModelScope.launch {
            githubRepository.state.collect { g -> _uiState.update { it.copy(githubState = g) } }
        }
        viewModelScope.launch {
            settingsRepository.notifyProofLoop.collect { n -> _uiState.update { it.copy(notifyProofLoop = n) } }
        }
        viewModelScope.launch {
            settingsRepository.notifyLearning.collect { n -> _uiState.update { it.copy(notifyLearning = n) } }
        }
    }

    fun onToggleNotifyProofLoop(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotifyProofLoop(enabled) }
    }

    fun onToggleNotifyLearning(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setNotifyLearning(enabled) }
    }

    fun onClearAllData(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isClearing = true) }
            settingsRepository.deleteAllLocalData()
            _uiState.update { it.copy(isClearing = false) }
            onDone()
        }
    }

    fun onSignOut(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.signOut()
            onDone()
        }
    }
}
