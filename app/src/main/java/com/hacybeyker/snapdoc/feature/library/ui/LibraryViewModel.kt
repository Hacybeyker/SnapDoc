package com.hacybeyker.snapdoc.feature.library.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.snapdoc.feature.library.domain.DeleteDocumentUseCase
import com.hacybeyker.snapdoc.feature.library.domain.ObserveLibraryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class LibraryViewModel @Inject constructor(
    observeLibraryUseCase: ObserveLibraryUseCase,
    private val deleteDocumentUseCase: DeleteDocumentUseCase
) : ViewModel() {

    private val query = MutableStateFlow("")

    /**
     * Two flows deliberately: [query] drives the text field and must update on every keystroke, while
     * the search behind it is debounced so a five-letter word costs one query instead of five. They
     * are recombined at the end, which is why the box never lags behind what was typed.
     *
     * `flatMapLatest` is what makes an in-flight search abandonable — the results of a query the user
     * has already moved past are worse than useless, they would overwrite the current ones.
     */
    private val results = query
        .debounce { if (it.isBlank()) 0 else SEARCH_DEBOUNCE_MILLIS }
        .distinctUntilChanged()
        .flatMapLatest { observeLibraryUseCase(it) }
        .catch { emit(emptyList()) }

    val uiState: StateFlow<LibraryUiState> = combine(query, results) { currentQuery, documents ->
        LibraryUiState(query = currentQuery, documents = documents, isLoading = false)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT_MILLIS),
        initialValue = LibraryUiState()
    )

    fun onIntent(intent: LibraryIntent) {
        when (intent) {
            is LibraryIntent.QueryChanged -> query.value = intent.query
            is LibraryIntent.DeleteDocument -> viewModelScope.launch { deleteDocumentUseCase(intent.id) }
        }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MILLIS = 250L

        /** Outlives a rotation, so coming back does not re-run the query and re-read the database. */
        const val SUBSCRIPTION_TIMEOUT_MILLIS = 5_000L
    }
}
