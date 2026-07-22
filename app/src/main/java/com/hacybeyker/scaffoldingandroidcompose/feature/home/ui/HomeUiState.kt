package com.hacybeyker.scaffoldingandroidcompose.feature.home.ui

import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.Greeting

sealed interface HomeUiState {
    data object Loading : HomeUiState

    data class Content(val greeting: Greeting) : HomeUiState
}

sealed interface HomeIntent {
    data class SubmitName(val name: String) : HomeIntent
}
