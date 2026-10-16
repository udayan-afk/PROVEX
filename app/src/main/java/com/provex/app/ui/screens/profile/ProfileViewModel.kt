package com.provex.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.model.WorkMode
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
)

class ProfileViewModel(
    private val profileRepository: ProfileRepository,
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
    }

    fun onNameChanged(name: String) = _uiState.update { it.copy(name = name, isSaved = false) }
    fun onLevelChanged(level: String) = _uiState.update { it.copy(level = level, isSaved = false) }
    fun onTargetRoleChanged(role: String) = _uiState.update { it.copy(targetRole = role, isSaved = false) }
    fun onLocationChanged(loc: String) = _uiState.update { it.copy(location = loc, isSaved = false) }
    fun onWorkModeChanged(mode: WorkMode) = _uiState.update { it.copy(workMode = mode, isSaved = false) }

    fun toggleLanguage(lang: String) = _uiState.update {
        val next = if (lang in it.selectedLanguages) it.selectedLanguages - lang else it.selectedLanguages + lang
        it.copy(selectedLanguages = next, isSaved = false)
    }

    fun toggleFramework(framework: String) = _uiState.update {
        val next = if (framework in it.selectedFrameworks) it.selectedFrameworks - framework else it.selectedFrameworks + framework
        it.copy(selectedFrameworks = next, isSaved = false)
    }

    fun toggleOtherSkill(skill: String) = _uiState.update {
        val next = if (skill in it.selectedOtherSkills) it.selectedOtherSkills - skill else it.selectedOtherSkills + skill
        it.copy(selectedOtherSkills = next, isSaved = false)
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
            profileRepository.save(profile)
            _uiState.update { it.copy(isSaved = true) }
        }
    }
}
