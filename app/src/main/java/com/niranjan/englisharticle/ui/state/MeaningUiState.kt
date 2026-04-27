package com.niranjan.englisharticle.ui.state

import com.niranjan.englisharticle.domain.MeaningResult

sealed interface MeaningUiState {
    data object Idle : MeaningUiState
    data object Loading : MeaningUiState
    data class Success(val result: MeaningResult) : MeaningUiState
    data class Error(val message: String) : MeaningUiState
}
