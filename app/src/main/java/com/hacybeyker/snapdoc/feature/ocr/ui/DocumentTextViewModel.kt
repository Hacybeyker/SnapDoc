package com.hacybeyker.snapdoc.feature.ocr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizeDocumentTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DocumentTextViewModel @Inject constructor(
    private val recognizeDocumentTextUseCase: RecognizeDocumentTextUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<DocumentTextUiState>(DocumentTextUiState.Recognizing)
    val uiState: StateFlow<DocumentTextUiState> = _uiState.asStateFlow()

    private val _effects = Channel<DocumentTextEffect>(Channel.BUFFERED)
    val effects: Flow<DocumentTextEffect> = _effects.receiveAsFlow()

    /** Null until the Screen reports the pages, which is what tells a repeat request from the first. */
    private var requestedPaths: List<String>? = null

    fun onIntent(intent: DocumentTextIntent) {
        when (intent) {
            is DocumentTextIntent.RecognizeText -> onRecognizeText(intent.imagePaths)
            DocumentTextIntent.Retry -> recognize()
            DocumentTextIntent.CopyText -> onCopyText()
        }
    }

    /**
     * The Screen re-sends the pages every time the composition restarts (a rotation, returning to
     * the screen), so asking for the same pages again must not run the model over them a second time.
     */
    private fun onRecognizeText(imagePaths: List<String>) {
        if (imagePaths == requestedPaths) return
        requestedPaths = imagePaths
        recognize()
    }

    private fun recognize() {
        val imagePaths = requestedPaths.orEmpty()
        if (imagePaths.isEmpty()) {
            // Navigation handed over a scan with no pages; there is nothing a retry could fix.
            _uiState.value = DocumentTextUiState.Error
            return
        }
        _uiState.value = DocumentTextUiState.Recognizing
        viewModelScope.launch {
            runCatching { recognizeDocumentTextUseCase(imagePaths) }
                .onSuccess { document ->
                    _uiState.value = if (document.isEmpty) {
                        DocumentTextUiState.Empty
                    } else {
                        DocumentTextUiState.Content(document)
                    }
                }
                .onFailure { _uiState.value = DocumentTextUiState.Error }
        }
    }

    private fun onCopyText() {
        val content = _uiState.value as? DocumentTextUiState.Content ?: return
        _effects.trySend(DocumentTextEffect.CopyToClipboard(content.document.fullText))
    }
}
