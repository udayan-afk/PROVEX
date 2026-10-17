package com.provex.app.ui.screens.repositories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.domain.repository.GithubRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RepositoriesUiState(
    val githubState: GithubState = GithubState(
        status = RepoConnectionStatus.NOT_CONNECTED,
        accountLogin = null,
        repos = emptyList(),
        isSample = false,
        message = null,
    ),
    val isConnecting: Boolean = false,
    val isDisconnecting: Boolean = false,
    val authUrlToLaunch: String? = null,
    val feedbackMessage: String? = null,
)

class RepositoriesViewModel(
    private val githubRepository: GithubRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepositoriesUiState())
    val uiState: StateFlow<RepositoriesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            githubRepository.state.collect { state ->
                _uiState.update { it.copy(githubState = state) }
            }
        }
        onRefresh()
    }

    fun onConnectClicked() {
        viewModelScope.launch {
            _uiState.update { it.copy(isConnecting = true, feedbackMessage = null) }
            val result = githubRepository.beginAuthorization()
            result.fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(isConnecting = false, authUrlToLaunch = url) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isConnecting = false,
                            feedbackMessage = err.message ?: "Failed to start GitHub authorization.",
                        )
                    }
                },
            )
        }
    }

    fun onRefresh() {
        viewModelScope.launch {
            githubRepository.refresh()
        }
    }

    fun onDisconnect() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDisconnecting = true, feedbackMessage = null) }
            val result = githubRepository.disconnect()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDisconnecting = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isDisconnecting = false,
                            feedbackMessage = err.message ?: "Failed to disconnect GitHub.",
                        )
                    }
                }
            )
        }
    }

    fun onToggleSampleRepos(enabled: Boolean) {
        viewModelScope.launch {
            githubRepository.setSampleEnabled(enabled)
        }
    }

    fun onToggleRepoSelection(repoId: String) {
        viewModelScope.launch {
            githubRepository.toggleSelected(repoId)
        }
    }

    fun clearAuthUrl() {
        _uiState.update { it.copy(authUrlToLaunch = null) }
    }
}
