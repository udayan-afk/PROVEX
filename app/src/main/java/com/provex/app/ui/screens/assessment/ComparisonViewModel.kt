package com.provex.app.ui.screens.assessment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.EvidenceStatus
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.SkillAssessment
import com.provex.app.domain.repository.AnalysisRepository
import com.provex.app.domain.repository.AssessmentResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ComparisonUiState(
    val analysisId: String = "",
    val analysis: JobAnalysis? = null,
    val assessments: List<SkillAssessment> = emptyList(),
    val revision: String? = null,
    val notice: String? = null,
    val isLoading: Boolean = true,
    val filterStatus: EvidenceStatus? = null,
)

class ComparisonViewModel(
    private val analysisId: String,
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ComparisonUiState(analysisId = analysisId))
    val uiState: StateFlow<ComparisonUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            analysisRepository.observeAnalysis(analysisId).collect { a ->
                if (a != null) {
                    val result = analysisRepository.assess(a)
                    _uiState.update {
                        it.copy(
                            analysis = a,
                            assessments = result.assessments,
                            revision = result.revision,
                            notice = result.notice,
                            isLoading = false,
                        )
                    }
                }
            }
        }
    }

    fun setFilter(status: EvidenceStatus?) {
        _uiState.update { it.copy(filterStatus = status) }
    }
}
