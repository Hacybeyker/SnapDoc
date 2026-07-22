package com.hacybeyker.scaffoldingandroidcompose.feature.home.ui

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.Greeting
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase.ObserveGreetingUseCase
import com.hacybeyker.scaffoldingandroidcompose.feature.home.domain.usecase.UpdateGreetingNameUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Stable
@HiltViewModel
class HomeViewModel @Inject constructor(
    observeGreeting: ObserveGreetingUseCase,
    private val updateGreetingName: UpdateGreetingNameUseCase
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeGreeting()
        .map<Greeting, HomeUiState> { HomeUiState.Content(greeting = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = HomeUiState.Loading
        )

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.SubmitName -> viewModelScope.launch { updateGreetingName(intent.name) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
