package com.provex.app.ui.screens.evidence

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.SkillAssessment
import com.provex.app.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EvidenceDetailUiState(
    val skillName: String = "",
    val assessment: SkillAssessment? = null,
    val revision: String? = null,
    val isLoading: Boolean = true,
)

class EvidenceDetailViewModel(
    private val analysisId: String,
    private val skill: String,
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(EvidenceDetailUiState(skillName = skill))
    val uiState: StateFlow<EvidenceDetailUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            analysisRepository.observeAnalysis(analysisId).collect { a ->
                if (a != null) {
                    val result = analysisRepository.assess(a)
                    val found = result.assessments.firstOrNull { it.skill.equals(skill, ignoreCase = true) }
                    _uiState.update {
                        it.copy(
                            assessment = found,
                            revision = result.revision,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }
}
