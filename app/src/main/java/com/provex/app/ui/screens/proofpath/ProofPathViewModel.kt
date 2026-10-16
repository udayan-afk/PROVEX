package com.provex.app.ui.screens.proofpath

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.LearningPlanItem
import com.provex.app.domain.repository.AnalysisRepository
import com.provex.app.domain.repository.LearningRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ProofPathUiState(
    val analysisId: String = "",
    val analysis: JobAnalysis? = null,
    val items: List<LearningPlanItem> = emptyList(),
    val completedCriteria: Set<String> = emptySet(),
    val notice: String? = null,
    val isLoading: Boolean = true,
)

class ProofPathViewModel(
    private val analysisId: String,
    private val analysisRepository: AnalysisRepository,
    private val learningRepository: LearningRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProofPathUiState(analysisId = analysisId))
    val uiState: StateFlow<ProofPathUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            learningRepository.completedCriteria.collect { set ->
                _uiState.update { it.copy(completedCriteria = set) }
            }
        }
        viewModelScope.launch {
            analysisRepository.observeAnalysis(analysisId).collect { a ->
                if (a != null) {
                    val assessmentResult = analysisRepository.assess(a)
                    val planResult = learningRepository.plan(a, assessmentResult.assessments)
                    _uiState.update {
                        it.copy(
                            analysis = a,
                            items = planResult.items,
                            notice = planResult.notice,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun toggleCriterion(skill: String, criterionIndex: Int) {
        val key = "$analysisId:$skill:$criterionIndex"
        viewModelScope.launch {
            learningRepository.toggleCriterion(key)
        }
    }
}
