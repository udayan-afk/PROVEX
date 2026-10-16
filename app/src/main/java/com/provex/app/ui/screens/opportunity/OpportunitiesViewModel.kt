package com.provex.app.ui.screens.opportunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.di.JobDraftHolder
import com.provex.app.domain.model.AssistantChatMessage
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.repository.ProfileRepository
import com.provex.app.domain.usecase.CareerOpportunityEngine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class OpportunitiesUiState(
    val profile: UserProfile = UserProfile(),
    val messages: List<AssistantChatMessage> = emptyList(),
    val inputQuery: String = "",
    val isGenerating: Boolean = false,
    val errorMessage: String? = null,
    val suggestedPrompts: List<String> = listOf(
        "Android Developer Intern",
        "Python Developer",
        "AI/ML Intern",
        "Junior Software Developer",
    ),
)

sealed interface OpportunitiesNavigationEvent {
    data object NavigateToHome : OpportunitiesNavigationEvent
    data object NavigateToProfile : OpportunitiesNavigationEvent
}

class OpportunitiesViewModel(
    private val profileRepository: ProfileRepository,
    private val jobDraftHolder: JobDraftHolder? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpportunitiesUiState())
    val uiState: StateFlow<OpportunitiesUiState> = _uiState.asStateFlow()

    private val _navigationEvents = MutableSharedFlow<OpportunitiesNavigationEvent>()
    val navigationEvents: SharedFlow<OpportunitiesNavigationEvent> = _navigationEvents.asSharedFlow()

    init {
        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update { it.copy(profile = p) }
            }
        }
    }

    fun onQueryChanged(query: String) {
        _uiState.update { it.copy(inputQuery = query, errorMessage = null) }
    }

    fun onSendQuery(queryOverride: String? = null) {
        val query = (queryOverride ?: _uiState.value.inputQuery).trim()
        if (query.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Please enter a role or select a prompt below.") }
            return
        }

        val userMessage = AssistantChatMessage(
            id = UUID.randomUUID().toString(),
            isUser = true,
            text = query,
        )

        val updatedMessages = _uiState.value.messages + userMessage
        _uiState.update {
            it.copy(
                messages = updatedMessages,
                inputQuery = "",
                isGenerating = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            // Brief simulation of assistant reasoning for natural user interaction
            delay(300L)
            val currentProfile = _uiState.value.profile
            val guidance = CareerOpportunityEngine.generateGuidance(query, currentProfile)

            val assistantMessage = AssistantChatMessage(
                id = UUID.randomUUID().toString(),
                isUser = false,
                text = guidance.summary,
                guidance = guidance,
            )

            _uiState.update {
                it.copy(
                    messages = it.messages + assistantMessage,
                    isGenerating = false,
                )
            }
        }
    }

    fun onSelectPrompt(prompt: String) {
        onSendQuery(queryOverride = prompt)
    }

    fun onAnalyzeRole(roleTitle: String, sampleJobDescription: String?) {
        val textToAnalyze = sampleJobDescription ?: """
            Job Title: $roleTitle
            Requirements:
            - Demonstrable projects and skills aligned with $roleTitle.
            - Paste your actual employer posting requirements here to evaluate evidence.
        """.trimIndent()

        jobDraftHolder?.setDraft(textToAnalyze)
        viewModelScope.launch {
            _navigationEvents.emit(OpportunitiesNavigationEvent.NavigateToHome)
        }
    }

    fun onClearConversation() {
        _uiState.update { it.copy(messages = emptyList(), errorMessage = null) }
    }

    fun onNavigateToProfile() {
        viewModelScope.launch {
            _navigationEvents.emit(OpportunitiesNavigationEvent.NavigateToProfile)
        }
    }
}
