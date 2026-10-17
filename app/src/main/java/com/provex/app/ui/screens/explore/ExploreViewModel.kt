package com.provex.app.ui.screens.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.di.JobDraftHolder
import com.provex.app.domain.model.AssistantOpportunityCard
import com.provex.app.domain.model.Opportunity
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.repository.InsightsRepository
import com.provex.app.domain.repository.ProfileRepository
import com.provex.app.domain.usecase.CareerOpportunityEngine
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExploreUiState(
    val profile: UserProfile = UserProfile(),
    val selectedTab: Int = 0, // 0 = Job Skill Analyzer, 1 = Role Archetypes
    val selectedCategory: String = "All",
    val availableCategories: List<String> = listOf("All", "Mobile", "Backend", "AI / ML", "Frontend", "Cloud & DevOps"),
    val roleArchetypes: List<AssistantOpportunityCard> = emptyList(),
    val sampleOpportunities: List<Opportunity> = emptyList(),
    val sampleEnabled: Boolean = false,
)

sealed interface ExploreNavigationEvent {
    data object NavigateToHome : ExploreNavigationEvent
    data class NavigateToRoleAdvisor(val initialPrompt: String? = null) : ExploreNavigationEvent
    data class PreloadAnalyzer(val text: String) : ExploreNavigationEvent
}

class ExploreViewModel(
    private val profileRepository: ProfileRepository,
    private val insightsRepository: InsightsRepository,
    private val jobDraftHolder: JobDraftHolder? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExploreUiState())
    val uiState: StateFlow<ExploreUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<ExploreNavigationEvent>()
    val navigationEvents: SharedFlow<ExploreNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update { it.copy(profile = p) }
                generateArchetypes(p, _uiState.value.selectedCategory)
                refreshSampleList(p, _uiState.value.sampleEnabled)
            }
        }

        viewModelScope.launch {
            insightsRepository.sampleOpportunitiesEnabled.collect { enabled ->
                _uiState.update { it.copy(sampleEnabled = enabled) }
                refreshSampleList(_uiState.value.profile, enabled)
            }
        }
    }

    fun onTabSelected(tab: Int) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun onCategorySelected(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        generateArchetypes(_uiState.value.profile, category)
    }

    fun onToggleSample(enabled: Boolean) {
        viewModelScope.launch {
            insightsRepository.setSampleOpportunities(enabled)
        }
    }

    fun onAnalyzeRole(roleTitle: String, sampleJobDescription: String?) {
        val textToAnalyze = sampleJobDescription ?: """
            Job Title: $roleTitle
            Requirements:
            - Demonstrable projects and skills aligned with $roleTitle.
            - Paste your actual employer posting requirements here to evaluate evidence.
        """.trimIndent()

        jobDraftHolder?.setDraft(textToAnalyze)
        _uiState.update { it.copy(selectedTab = 0) }
        viewModelScope.launch {
            _navigationEvents.emit(ExploreNavigationEvent.PreloadAnalyzer(textToAnalyze))
        }
    }

    fun onConsultAdvisor(roleTitle: String) {
        viewModelScope.launch {
            _navigationEvents.emit(ExploreNavigationEvent.NavigateToRoleAdvisor(roleTitle))
        }
    }

    private fun generateArchetypes(profile: UserProfile, category: String) {
        val queries = when (category) {
            "Mobile" -> listOf("Android Developer Intern", "Kotlin Mobile Engineer")
            "Backend" -> listOf("Python Backend Developer", "Java Software Developer")
            "AI / ML" -> listOf("AI/ML Intern", "Junior Data Scientist")
            "Frontend" -> listOf("Frontend Developer Intern", "React Web Engineer")
            "Cloud & DevOps" -> listOf("Cloud & DevOps Intern")
            else -> listOf(
                "Android Developer Intern",
                "Python Backend Developer",
                "AI/ML Intern",
                "Junior Software Developer",
                "Frontend Developer Intern",
                "Cloud & DevOps Intern",
            )
        }

        val cards = queries.flatMap { query ->
            CareerOpportunityEngine.generateGuidance(query, profile).opportunityCards
        }.distinctBy { it.roleTitle }

        _uiState.update { it.copy(roleArchetypes = cards) }
    }

    private suspend fun refreshSampleList(profile: UserProfile, sampleEnabled: Boolean) {
        val real = insightsRepository.realOpportunities()
        val list = if (sampleEnabled && real.isEmpty()) {
            insightsRepository.sampleOpportunities(profile)
        } else {
            real
        }
        _uiState.update { it.copy(sampleOpportunities = list) }
    }
}
