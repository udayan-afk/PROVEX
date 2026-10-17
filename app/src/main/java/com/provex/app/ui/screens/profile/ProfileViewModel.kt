package com.provex.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.model.WorkMode
import com.provex.app.domain.repository.GithubRepository
import com.provex.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProfileUiState(
    val name: String = "",
    val level: String = "",
    val targetRole: String = "",
    val location: String = "",
    val workMode: WorkMode = WorkMode.NO_PREFERENCE,
    val selectedLanguages: Set<String> = emptySet(),
    val selectedFrameworks: Set<String> = emptySet(),
    val selectedOtherSkills: Set<String> = emptySet(),
    val isSaved: Boolean = false,
    val isLoading: Boolean = false,
    val isSyncing: Boolean = false,
    val errorMessage: String? = null,
    val githubState: GithubState = GithubState(
        status = RepoConnectionStatus.NOT_CONNECTED,
        accountLogin = null,
        repos = emptyList(),
        isSample = false,
        message = null,
    ),
    val isConnectingGithub: Boolean = false,
    val isDisconnectingGithub: Boolean = false,
    val githubAuthUrlToLaunch: String? = null,
    val githubActionMessage: String? = null,
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
    private val githubRepository: GithubRepository? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update {
                    it.copy(
                        name = p.name,
                        level = p.level,
                        targetRole = p.targetRole,
                        location = p.location,
                        workMode = p.workMode,
                        selectedLanguages = p.languages.toSet(),
                        selectedFrameworks = p.frameworks.toSet(),
                        selectedOtherSkills = p.skills.toSet(),
                    )
                }
            }
        }
        githubRepository?.let { ghRepo ->
            viewModelScope.launch {
                ghRepo.state.collect { ghState ->
                    _uiState.update { it.copy(githubState = ghState) }
                }
            }
        }
        syncProfile()
        onRefreshGithub()
    }

    fun syncProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSyncing = true, errorMessage = null) }
            val res = profileRepository.syncFromRemote()
            res.fold(
                onSuccess = { _uiState.update { it.copy(isSyncing = false) } },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isSyncing = false,
                            errorMessage = "Couldn't reach backend: ${err.message ?: "Network error"}",
                        )
                    }
                }
            )
        }
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name, isSaved = false, errorMessage = null) }
    fun onLevelChanged(level: String) = _uiState.update { it.copy(level = level, isSaved = false, errorMessage = null) }
    fun onTargetRoleChanged(role: String) = _uiState.update { it.copy(targetRole = role, isSaved = false, errorMessage = null) }
    fun onLocationChanged(loc: String) = _uiState.update { it.copy(location = loc, isSaved = false, errorMessage = null) }
    fun onWorkModeChanged(mode: WorkMode) = _uiState.update { it.copy(workMode = mode, isSaved = false, errorMessage = null) }

    fun toggleLanguage(lang: String) = _uiState.update {
        val next = if (lang in it.selectedLanguages) it.selectedLanguages - lang else it.selectedLanguages + lang
        it.copy(selectedLanguages = next, isSaved = false, errorMessage = null)
    }

    fun toggleFramework(framework: String) = _uiState.update {
        val next = if (framework in it.selectedFrameworks) it.selectedFrameworks - framework else it.selectedFrameworks + framework
        it.copy(selectedFrameworks = next, isSaved = false, errorMessage = null)
    }

    fun toggleOtherSkill(skill: String) = _uiState.update {
        val next = if (skill in it.selectedOtherSkills) it.selectedOtherSkills - skill else it.selectedOtherSkills + skill
        it.copy(selectedOtherSkills = next, isSaved = false, errorMessage = null)
    }

    fun saveProfile() {
        val s = _uiState.value
        val profile = UserProfile(
            name = s.name.trim(),
            level = s.level.trim(),
            targetRole = s.targetRole.trim(),
            languages = s.selectedLanguages.toList(),
            frameworks = s.selectedFrameworks.toList(),
            skills = s.selectedOtherSkills.toList(),
            location = s.location.trim(),
            workMode = s.workMode,
            interests = emptyList(),
        )
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSaved = false) }
            val res = profileRepository.save(profile)
            res.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, isSaved = true) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSaved = false,
                            errorMessage = "Saved locally, but remote sync failed: ${err.message ?: "Server unavailable"}",
                        )
                    }
                }
            )
        }
    }

    fun onConnectGithub() {
        val ghRepo = githubRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isConnectingGithub = true, githubActionMessage = null) }
            val result = ghRepo.beginAuthorization()
            result.fold(
                onSuccess = { url ->
                    _uiState.update { it.copy(isConnectingGithub = false, githubAuthUrlToLaunch = url) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isConnectingGithub = false,
                            githubActionMessage = err.message ?: "Failed to initiate GitHub authorization.",
                        )
                    }
                }
            )
        }
    }

    fun onDisconnectGithub() {
        val ghRepo = githubRepository ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isDisconnectingGithub = true, githubActionMessage = null) }
            val result = ghRepo.disconnect()
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isDisconnectingGithub = false) }
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isDisconnectingGithub = false,
                            githubActionMessage = err.message ?: "Failed to disconnect GitHub.",
                        )
                    }
                }
            )
        }
    }

    fun onRefreshGithub() {
        viewModelScope.launch {
            githubRepository?.refresh()
        }
    }

    fun clearGithubAuthUrl() {
        _uiState.update { it.copy(githubAuthUrlToLaunch = null) }
    }
}
