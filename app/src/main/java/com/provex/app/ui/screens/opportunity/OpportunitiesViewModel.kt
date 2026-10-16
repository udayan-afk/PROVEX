package com.provex.app.ui.screens.opportunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.Opportunity
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.repository.InsightsRepository
import com.provex.app.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OpportunitiesUiState(
    val profile: UserProfile = UserProfile(),
    val opportunities: List<Opportunity> = emptyList(),
    val sampleEnabled: Boolean = true,
)

class OpportunitiesViewModel(
    private val profileRepository: ProfileRepository,
    private val insightsRepository: InsightsRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpportunitiesUiState())
    val uiState: StateFlow<OpportunitiesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update { it.copy(profile = p) }
                refreshList(p, _uiState.value.sampleEnabled)
            }
        }
        viewModelScope.launch {
            insightsRepository.sampleOpportunitiesEnabled.collect { enabled ->
                _uiState.update { it.copy(sampleEnabled = enabled) }
                refreshList(_uiState.value.profile, enabled)
            }
        }
    }

    private suspend fun refreshList(profile: UserProfile, sampleEnabled: Boolean) {
        val real = insightsRepository.realOpportunities()
        val list = if (sampleEnabled && real.isEmpty()) {
            insightsRepository.sampleOpportunities(profile)
        } else {
            real
        }
        _uiState.update { it.copy(opportunities = list) }
    }

    fun onToggleSample(enabled: Boolean) {
        viewModelScope.launch {
            insightsRepository.setSampleOpportunities(enabled)
        }
    }
}
