package com.provex.app.ui.screens.analyzer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.di.JobDraftHolder
import com.provex.app.domain.model.ExtractedSkillModel
import com.provex.app.domain.model.JobAnalysisResult
import com.provex.app.domain.repository.JobAnalyzerRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class JobSkillAnalyzerUiState(
    val descriptionText: String = "",
    val urlText: String = "",
    val isLoading: Boolean = false,
    val isMatchingGithub: Boolean = false,
    val errorMessage: String? = null,
    val githubMatchError: String? = null,
    val inputNotice: String? = null,
    val result: JobAnalysisResult? = null,
    val selectedFilter: String = "ALL", // ALL, REQUIRED, PREFERRED
    val copiedMessage: Boolean = false,
) {
    val filteredSkills: List<ExtractedSkillModel>
        get() = when (selectedFilter) {
            "REQUIRED" -> result?.skills?.filter { it.importance.equals("Required", ignoreCase = true) } ?: emptyList()
            "PREFERRED" -> result?.skills?.filter { it.importance.equals("Preferred", ignoreCase = true) } ?: emptyList()
            else -> result?.skills ?: emptyList()
        }
}

class JobSkillAnalyzerViewModel(
    private val repository: JobAnalyzerRepository,
    private val draftHolder: JobDraftHolder? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(JobSkillAnalyzerUiState())
    val uiState: StateFlow<JobSkillAnalyzerUiState> = _uiState.asStateFlow()

    init {
        // If there is an existing draft, preload it
        draftHolder?.let { holder ->
            val (text, url) = holder.consumeDraft()
            if (!text.isNullOrBlank() || !url.isNullOrBlank()) {
                _uiState.update {
                    it.copy(
                        descriptionText = text ?: "",
                        urlText = url ?: "",
                    )
                }
            }
        }
    }

    fun onDescriptionChanged(text: String) {
        _uiState.update {
            it.copy(
                descriptionText = text,
                errorMessage = null,
                inputNotice = null,
            )
        }
        updateInputNotice()
    }

    fun onUrlChanged(url: String) {
        _uiState.update {
            it.copy(
                urlText = url,
                errorMessage = null,
                inputNotice = null,
            )
        }
        updateInputNotice()
    }

    private fun updateInputNotice() {
        val hasDesc = _uiState.value.descriptionText.isNotBlank()
        val hasUrl = _uiState.value.urlText.isNotBlank()
        if (hasDesc && hasUrl) {
            _uiState.update {
                it.copy(
                    inputNotice = "Both description and URL provided. PROVEX will prioritize analyzing the pasted description.",
                )
            }
        } else {
            _uiState.update { it.copy(inputNotice = null) }
        }
    }

    fun onLoadExample() {
        val exampleText = """
Job Title: Android Engineer Intern
Company: Apex Mobile Technologies

About the Team:
Join our mobile engineering team building high-performance Android consumer applications used by millions.

Responsibilities:
- Build reactive, modular UI components using Jetpack Compose and Material Design 3.
- Integrate asynchronous REST APIs using Retrofit, OkHttp, and Kotlin Coroutines.
- Implement offline-first local storage architectures using Room Database and SQLite.
- Write robust unit tests with JUnit and MockK to maintain high code reliability.
- Participate in code reviews, sprint planning, and Agile team ceremonies.

Required Qualifications:
- Strong programming fundamentals in Kotlin or Java.
- Familiarity with Android SDK development, Activity/Fragment lifecycles, and MVVM architecture.
- Hands-on experience with Git version control and GitHub.
- Solid understanding of data structures, algorithms, and problem solving.
- Currently pursuing a Bachelor's degree in Computer Science, Software Engineering, or equivalent.

Preferred Qualifications:
- Experience with CI/CD automation using GitHub Actions.
- Familiarity with Python, FastAPI, or cloud backends.
- Familiarity with Figma UI/UX design handoff tools.
        """.trimIndent()

        _uiState.update {
            it.copy(
                descriptionText = exampleText,
                urlText = "",
                errorMessage = null,
                inputNotice = null,
            )
        }
    }

    fun onFilterSelected(filter: String) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun onAnalyzeClicked() {
        val desc = _uiState.value.descriptionText.trim()
        val url = _uiState.value.urlText.trim()

        if (desc.isBlank() && url.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "Please paste a job description or provide a job posting URL.")
            }
            return
        }

        if (desc.isNotBlank() && desc.length < 30 && url.isBlank()) {
            _uiState.update {
                it.copy(errorMessage = "The provided job description is too brief. Please paste a more complete job posting.")
            }
            return
        }

        _uiState.update {
            it.copy(
                isLoading = true,
                errorMessage = null,
            )
        }

        viewModelScope.launch {
            val result = repository.analyzeJob(
                description = desc.ifBlank { null },
                url = url.ifBlank { null },
            )

            result.fold(
                onSuccess = { analysis ->
                    _uiState.update {
                        it.copy(
                            result = analysis,
                            isLoading = false,
                            errorMessage = null,
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "Failed to analyze job posting.",
                        )
                    }
                },
            )
        }
    }

    fun onCopySkills(context: Context) {
        val analysis = _uiState.value.result ?: return
        val sb = StringBuilder()
        sb.appendLine("PROVEX Skill Analysis: ${analysis.jobTitle}")
        if (!analysis.company.isNullOrBlank()) {
            sb.appendLine("Company: ${analysis.company}")
        }
        sb.appendLine("\nExtracted Skills (${analysis.skills.size}):")
        analysis.skills.forEach { s ->
            sb.appendLine("• ${s.name} [${s.category}] - ${s.importance}")
            if (s.evidence.isNotBlank()) {
                sb.appendLine("  Evidence: \"${s.evidence}\"")
            }
        }
        if (analysis.resumeKeywords.isNotEmpty()) {
            sb.appendLine("\nResume Keywords:")
            sb.appendLine(analysis.resumeKeywords.joinToString(", "))
        }

        try {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("PROVEX Extracted Skills", sb.toString())
            clipboard.setPrimaryClip(clip)
            _uiState.update { it.copy(copiedMessage = true) }
        } catch (_: Exception) {}
    }

    fun onDismissCopiedMessage() {
        _uiState.update { it.copy(copiedMessage = false) }
    }

    fun onResetForm() {
        _uiState.update {
            JobSkillAnalyzerUiState()
        }
    }

    fun onEditDescription() {
        _uiState.update {
            it.copy(result = null)
        }
    }

    fun onMatchGithubClicked() {
        val currentSkills = _uiState.value.result?.skills ?: return
        if (currentSkills.isEmpty()) return

        _uiState.update {
            it.copy(
                isMatchingGithub = true,
                githubMatchError = null,
            )
        }

        viewModelScope.launch {
            val result = repository.matchJobGithub(currentSkills)
            result.fold(
                onSuccess = { matchResult ->
                    _uiState.update { state ->
                        state.copy(
                            isMatchingGithub = false,
                            githubMatchError = null,
                            result = state.result?.copy(githubMatching = matchResult),
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update { state ->
                        state.copy(
                            isMatchingGithub = false,
                            githubMatchError = error.message ?: "Failed to match skills with GitHub.",
                        )
                    }
                },
            )
        }
    }
}
