package com.provex.app.ui.screens.opportunity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.Importance
import com.provex.app.domain.model.JobAnalysis
import com.provex.app.domain.model.JobRequirement
import com.provex.app.domain.model.RequirementCategory
import com.provex.app.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class ExtractionUiState(
    val analysisId: String = "",
    val analysis: JobAnalysis? = null,
    val title: String = "",
    val company: String = "",
    val requirements: List<JobRequirement> = emptyList(),
    val isSaving: Boolean = false,
    val newSkillName: String = "",
    val newSkillCategory: RequirementCategory = RequirementCategory.GENERAL,
    val newSkillImportance: Importance = Importance.REQUIRED,
)

class ExtractionViewModel(
    private val analysisId: String,
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExtractionUiState(analysisId = analysisId))
    val uiState: StateFlow<ExtractionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            analysisRepository.observeAnalysis(analysisId).collect { a ->
                if (a != null) {
                    _uiState.update {
                        it.copy(
                            analysis = a,
                            title = a.title.orEmpty(),
                            company = a.company.orEmpty(),
                            requirements = a.requirements,
                        )
                    }
                }
            }
        }
    }

    fun onTitleChanged(title: String) = _uiState.update { it.copy(title = title) }
    fun onCompanyChanged(company: String) = _uiState.update { it.copy(company = company) }

    fun toggleImportance(reqId: String) {
        _uiState.update { state ->
            val updated = state.requirements.map { req ->
                if (req.id == reqId) {
                    val nextImportance = when (req.importance) {
                        Importance.REQUIRED -> Importance.PREFERRED
                        Importance.PREFERRED -> Importance.MENTIONED
                        Importance.MENTIONED -> Importance.REQUIRED
                    }
                    req.copy(importance = nextImportance)
                } else req
            }
            state.copy(requirements = updated)
        }
    }

    fun removeRequirement(reqId: String) {
        _uiState.update { state ->
            state.copy(requirements = state.requirements.filterNot { it.id == reqId })
        }
    }

    fun onNewSkillNameChanged(name: String) = _uiState.update { it.copy(newSkillName = name) }
    fun onNewSkillCategoryChanged(cat: RequirementCategory) = _uiState.update { it.copy(newSkillCategory = cat) }
    fun onNewSkillImportanceChanged(imp: Importance) = _uiState.update { it.copy(newSkillImportance = imp) }

    fun addCustomRequirement() {
        val s = _uiState.value
        if (s.newSkillName.isBlank()) return
        val newReq = JobRequirement(
            id = "custom-${UUID.randomUUID()}",
            name = s.newSkillName.trim(),
            category = s.newSkillCategory,
            importance = s.newSkillImportance,
            sourceText = "Added by user",
        )
        _uiState.update {
            it.copy(
                requirements = it.requirements + newReq,
                newSkillName = "",
            )
        }
    }

    fun saveAndProceed(onDone: () -> Unit) {
        val s = _uiState.value
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            analysisRepository.saveEdits(
                id = analysisId,
                title = s.title,
                company = s.company,
                requirements = s.requirements,
            )
            _uiState.update { it.copy(isSaving = false) }
            onDone()
        }
    }
}
