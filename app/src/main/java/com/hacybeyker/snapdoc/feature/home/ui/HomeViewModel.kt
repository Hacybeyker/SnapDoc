package com.hacybeyker.snapdoc.feature.home.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.snapdoc.feature.library.domain.ObserveLibraryUseCase
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

data class HomeUiState(val recentDocuments: List<StoredDocument> = emptyList(), val isLoading: Boolean = true) {
    val hasDocuments: Boolean get() = recentDocuments.isNotEmpty()
}

/**
 * Home shows the last few scans because an entry screen with nothing on it teaches nothing. Seeing
 * real documents is also what makes the archive discoverable — it used to hide behind a text link.
 *
 * It reaches the archive through the library's use case, the same domain contract the reader uses to
 * file a scan; neither feature touches the other's internals.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(observeLibraryUseCase: ObserveLibraryUseCase) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = observeLibraryUseCase("")
        .map { documents -> HomeUiState(recentDocuments = documents.take(RECENT_COUNT), isLoading = false) }
        .catch { emit(HomeUiState(isLoading = false)) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
            initialValue = HomeUiState()
        )

    private companion object {
        /** Enough to prove the archive is real without turning the entry screen into a second list. */
        const val RECENT_COUNT = 3
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
