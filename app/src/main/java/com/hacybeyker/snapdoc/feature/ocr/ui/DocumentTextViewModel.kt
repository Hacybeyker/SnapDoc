package com.hacybeyker.snapdoc.feature.ocr.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.feature.library.domain.SaveScannedDocumentUseCase
import com.hacybeyker.snapdoc.feature.ocr.domain.GenerateDocumentInsightUseCase
import com.hacybeyker.snapdoc.feature.ocr.domain.ModelAvailability
import com.hacybeyker.snapdoc.feature.ocr.domain.ModelDownload
import com.hacybeyker.snapdoc.feature.ocr.domain.OnDeviceDocumentAnalyzer
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizeDocumentTextUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class DocumentTextViewModel @Inject constructor(
    private val recognizeDocumentTextUseCase: RecognizeDocumentTextUseCase,
    private val generateDocumentInsightUseCase: GenerateDocumentInsightUseCase,
    private val onDeviceDocumentAnalyzer: OnDeviceDocumentAnalyzer,
    private val saveScannedDocumentUseCase: SaveScannedDocumentUseCase
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
            DocumentTextIntent.EnableOnDeviceModel -> onEnableOnDeviceModel()
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
                    if (document.isEmpty) {
                        _uiState.value = DocumentTextUiState.Empty
                    } else {
                        _uiState.value = DocumentTextUiState.Content(document)
                        refreshModelStatus()
                        analyze(document.fullText)
                    }
                }
                .onFailure { _uiState.value = DocumentTextUiState.Error }
        }
    }

    /**
     * Reading the text and understanding it are reported separately on purpose: the text is the part
     * the user came for, so it is shown the moment it exists rather than waiting on an analysis that
     * may take a second and can only ever add to it.
     */
    private suspend fun analyze(text: String) {
        val insight = generateDocumentInsightUseCase(text)
        updateContent { it.copy(insight = insight) }
        archive(text, insight)
    }

    /**
     * Archiving happens after the analysis, not after the OCR, so the stored row carries the best
     * answer the device could give. Re-reading the same pages later replaces that row rather than
     * adding another, which is how an insight upgraded by a freshly downloaded model reaches the
     * archive too. A failure here is swallowed on purpose: the user came to read the document, and
     * losing the archive entry is not worth replacing what they came for with an error.
     */
    private suspend fun archive(text: String, insight: DocumentInsight) {
        val imagePaths = requestedPaths ?: return
        runCatching {
            saveScannedDocumentUseCase(
                imagePaths = imagePaths,
                text = text,
                insight = insight,
                createdAtEpochMillis = System.currentTimeMillis()
            )
        }
    }

    /**
     * Deliberately separate from [analyze]. Folding the two together meant every re-analysis also
     * re-read availability, so finishing a download set the status to Ready and the analysis that
     * followed immediately overwrote it with whatever the platform still reported — the status the
     * download flow had just earned was lost. Status is owned by whoever changes it, not by analysis.
     */
    private suspend fun refreshModelStatus() {
        val availability = runCatching { onDeviceDocumentAnalyzer.availability() }
            .getOrDefault(ModelAvailability.Unavailable)
        updateContent { it.copy(modelStatus = availability.toModelStatus()) }
    }

    private fun onEnableOnDeviceModel() {
        val content = _uiState.value as? DocumentTextUiState.Content ?: return
        if (content.modelStatus != DocumentTextUiState.ModelStatus.Downloadable) return
        viewModelScope.launch {
            onDeviceDocumentAnalyzer.download()
                .catch { emit(ModelDownload.Failed(it)) }
                .collect { progress -> onDownloadProgress(progress) }
        }
    }

    private suspend fun onDownloadProgress(progress: ModelDownload) {
        when (progress) {
            is ModelDownload.InProgress -> updateContent {
                it.copy(modelStatus = DocumentTextUiState.ModelStatus.Downloading(progress.bytesDownloaded))
            }

            // The document was already analyzed by the rules; now that the model is here it is worth
            // asking again, which is the only moment the user sees the answer actually improve.
            ModelDownload.Completed -> {
                updateContent { it.copy(modelStatus = DocumentTextUiState.ModelStatus.Ready) }
                (_uiState.value as? DocumentTextUiState.Content)?.let { analyze(it.document.fullText) }
            }

            is ModelDownload.Failed -> updateContent {
                it.copy(modelStatus = DocumentTextUiState.ModelStatus.DownloadFailed)
            }
        }
    }

    private fun onCopyText() {
        val content = _uiState.value as? DocumentTextUiState.Content ?: return
        _effects.trySend(DocumentTextEffect.CopyToClipboard(content.document.fullText))
    }

    private fun updateContent(transform: (DocumentTextUiState.Content) -> DocumentTextUiState.Content) {
        val content = _uiState.value as? DocumentTextUiState.Content ?: return
        _uiState.value = transform(content)
    }
}

private fun ModelAvailability.toModelStatus(): DocumentTextUiState.ModelStatus = when (this) {
    ModelAvailability.Available -> DocumentTextUiState.ModelStatus.Ready
    ModelAvailability.Downloadable -> DocumentTextUiState.ModelStatus.Downloadable
    ModelAvailability.Downloading -> DocumentTextUiState.ModelStatus.Downloading(bytesDownloaded = 0)
    ModelAvailability.Unavailable -> DocumentTextUiState.ModelStatus.Unavailable
}
