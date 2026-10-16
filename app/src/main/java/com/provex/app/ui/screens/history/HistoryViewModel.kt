package com.provex.app.ui.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.provex.app.domain.model.AssessmentSnapshot
import com.provex.app.domain.repository.AnalysisRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HistoryUiState(
    val snapshots: List<AssessmentSnapshot> = emptyList(),
    val isLoading: Boolean = true,
)

class HistoryViewModel(
    private val analysisRepository: AnalysisRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            analysisRepository.snapshots.collect { list ->
                _uiState.update { it.copy(snapshots = list, isLoading = false) }
            }
        }
    }
}
