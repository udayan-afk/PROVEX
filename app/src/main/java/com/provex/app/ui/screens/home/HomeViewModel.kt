package com.provex.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.GithubState
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.Opportunity
import com.provex.app.domain.model.RepoConnectionStatus
import com.provex.app.domain.model.SkillAssessment
import com.provex.app.domain.model.UserProfile
import com.provex.app.domain.repository.AnalysisRepository
import com.provex.app.domain.repository.GithubRepository
import com.provex.app.domain.repository.InsightsRepository
import com.provex.app.domain.repository.ProfileRepository
import com.provex.app.domain.usecase.Validators
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeUiState(
    val jobText: String = "",
    val jobUrl: String = "",
    val jobTextError: String? = null,
    val jobUrlError: String? = null,
    val isAnalyzing: Boolean = false,
    val profile: UserProfile = UserProfile(),
    val githubState: GithubState = GithubState(
        status = RepoConnectionStatus.NOT_CONNECTED,
        accountLogin = null,
        repos = emptyList(),
        isSample = false,
        message = null,
    ),
    val recentAnalyses: List<JobAnalysis> = emptyList(),
    val attentionSkills: List<SkillAssessment> = emptyList(),
    val opportunities: List<Opportunity> = emptyList(),
    val notice: String? = null,
    val authState: com.provex.app.domain.model.AuthState = com.provex.app.domain.model.AuthState.SignedOut,
)

sealed interface HomeEvent {
    data class NavigateToExtraction(val analysisId: String) : HomeEvent
}

class HomeViewModel(
    private val analysisRepository: AnalysisRepository,
    private val profileRepository: ProfileRepository,
    private val githubRepository: GithubRepository,
    private val insightsRepository: InsightsRepository,
    private val authRepository: com.provex.app.domain.repository.AuthRepository? = null,
    private val jobDraftHolder: com.provex.app.di.JobDraftHolder? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<HomeEvent>()
    val events: SharedFlow<HomeEvent> = _events.asSharedFlow()

    init {
        checkDraft()
        if (authRepository != null) {
            viewModelScope.launch {
                authRepository.authState.collect { auth ->
                    _uiState.update { it.copy(authState = auth) }
                }
            }
        }
        viewModelScope.launch {
            profileRepository.profile.collect { p ->
                _uiState.update { it.copy(profile = p) }
                updateOpportunities(p)
            }
        }
        viewModelScope.launch {
            githubRepository.state.collect { g ->
                _uiState.update { it.copy(githubState = g) }
            }
        }
        viewModelScope.launch {
            analysisRepository.analyses.collect { list ->
                _uiState.update { it.copy(recentAnalyses = list) }
                val latest = list.firstOrNull()
                if (latest != null) {
                    val result = analysisRepository.assess(latest)
                    val gaps = result.assessments.filter {
                        it.status != com.provex.app.domain.model.EvidenceStatus.STRONG
                    }.take(4)
                    _uiState.update { it.copy(attentionSkills = gaps) }
                }
            }
        }
    }

    private suspend fun updateOpportunities(profile: UserProfile) {
        val samples = insightsRepository.sampleOpportunities(profile)
        _uiState.update { it.copy(opportunities = samples) }
    }

    fun checkDraft() {
        val draft = jobDraftHolder?.consumeDraft()
        if (draft != null && !draft.first.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    jobText = draft.first!!,
                    jobUrl = draft.second ?: "",
                    jobTextError = null,
                    jobUrlError = null,
                )
            }
        }
    }

    fun onJobTextChanged(text: String) {
        _uiState.update { it.copy(jobText = text, jobTextError = null) }
    }

    fun onJobUrlChanged(url: String) {
        _uiState.update { it.copy(jobUrl = url, jobUrlError = null) }
    }

    fun onAnalyze() {
        val current = _uiState.value
        val textErr = Validators.jobTextError(current.jobText)
        val urlErr = Validators.urlError(current.jobUrl)

        if (textErr != null || urlErr != null) {
            _uiState.update { it.copy(jobTextError = textErr, jobUrlError = urlErr) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzing = true, notice = null) }
            val outcome = analysisRepository.analyze(current.jobText, current.jobUrl)
            outcome.fold(
                onSuccess = { res ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            jobText = "",
                            jobUrl = "",
                            notice = res.notice,
                        )
                    }
                    _events.emit(HomeEvent.NavigateToExtraction(res.analysis.id))
                },
                onFailure = { err ->
                    _uiState.update {
                        it.copy(
                            isAnalyzing = false,
                            notice = err.message ?: "Analysis failed. Please check the text and try again.",
                        )
                    }
                },
            )
        }
    }

    fun loadSampleJob() {
        val sample = """
Job Title: Android Software Engineer Intern
Company: Acme Tech Solutions
Location: Remote

About the Role:
We are looking for an Android Software Engineer Intern to join our mobile team for summer 2027.

Requirements:
- Strong knowledge of Kotlin and the Android SDK
- Experience building UIs with Jetpack Compose
- Solid grasp of MVVM architecture, Coroutines, and Flow
- Unit testing experience with JUnit
- Understanding of Git and GitHub workflows

Preferred Qualifications:
- Familiarity with SQLite or Room persistence
- Experience integrating REST APIs
- Familiarity with Docker or CI/CD pipelines
- Currently enrolled in a Computer Science or related degree
        """.trimIndent()
        _uiState.update {
            it.copy(
                jobText = sample,
                jobUrl = "https://example.com/careers/android-intern",
                jobTextError = null,
                jobUrlError = null,
            )
        }
    }
}
