package com.hacybeyker.snapdoc.feature.camera.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hacybeyker.snapdoc.feature.camera.domain.EvaluateLiveTextHintUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.ImportScannedPagesUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextHint
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextReading
import com.hacybeyker.snapdoc.feature.camera.domain.SaveCapturedPhotoUseCase
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
class CameraPreviewViewModel @Inject constructor(
    private val saveCapturedPhotoUseCase: SaveCapturedPhotoUseCase,
    private val importScannedPagesUseCase: ImportScannedPagesUseCase,
    private val evaluateLiveTextHintUseCase: EvaluateLiveTextHintUseCase
) : ViewModel() {

    /**
     * Kept outside the UiState: it is the debounce's working memory, not something the screen draws,
     * and it changes several times per second. Bounded so a long session cannot grow it.
     */
    private val recentReadings = ArrayDeque<LiveTextReading>()

    private val _uiState = MutableStateFlow<CameraPreviewUiState>(CameraPreviewUiState.Starting)
    val uiState: StateFlow<CameraPreviewUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CameraPreviewEffect>(Channel.BUFFERED)
    val effects: Flow<CameraPreviewEffect> = _effects.receiveAsFlow()

    fun onIntent(intent: CameraPreviewIntent) {
        when (intent) {
            CameraPreviewIntent.ViewfinderReady -> onViewfinderReady()
            CameraPreviewIntent.CameraUnavailable -> _uiState.value = CameraPreviewUiState.Unavailable
            CameraPreviewIntent.CapturePhoto -> onCapturePhoto()
            is CameraPreviewIntent.PhotoCaptured -> onPhotoCaptured(intent)
            CameraPreviewIntent.CaptureFailed ->
                updateReady { it.copy(isCapturing = false, captureError = CameraPreviewUiState.CaptureError.Camera) }

            is CameraPreviewIntent.FrameAnalyzed -> onFrameAnalyzed(intent.reading)
            CameraPreviewIntent.ToggleLiveAnalysis -> onToggleLiveAnalysis()

            CameraPreviewIntent.ScanDocument -> onScanDocument()
            is CameraPreviewIntent.PagesScanned -> onPagesScanned(intent)
            CameraPreviewIntent.ScanDismissed -> updateReady { it.copy(isScanning = false) }

            CameraPreviewIntent.ScanFailed ->
                updateReady { it.copy(isScanning = false, captureError = CameraPreviewUiState.CaptureError.Scanner) }
        }
    }

    /**
     * CameraX re-emits a surface whenever it is recreated (rotation, coming back to the screen), so
     * this keeps whatever the user already captured instead of resetting the screen.
     */
    private fun onViewfinderReady() {
        if (_uiState.value is CameraPreviewUiState.Ready) return
        _uiState.value = CameraPreviewUiState.Ready()
    }

    private fun onCapturePhoto() {
        val ready = _uiState.value as? CameraPreviewUiState.Ready ?: return
        if (ready.isCapturing) return
        _uiState.value = ready.copy(isCapturing = true, captureError = null)
        _effects.trySend(CameraPreviewEffect.TakePicture)
    }

    /**
     * Runs on every analyzed frame, so it does as little as possible: it only publishes a new state
     * when the debounced hint actually changed, which keeps a steady document from allocating a
     * fresh Ready several times a second for no visible difference.
     */
    private fun onFrameAnalyzed(reading: LiveTextReading) {
        val ready = _uiState.value as? CameraPreviewUiState.Ready ?: return
        if (!ready.isLiveAnalysisEnabled) return
        recentReadings.addLast(reading)
        while (recentReadings.size > EvaluateLiveTextHintUseCase.AGREEING_FRAMES) {
            recentReadings.removeFirst()
        }
        val hint = evaluateLiveTextHintUseCase(recentReadings.toList(), ready.liveTextHint)
        if (hint != ready.liveTextHint) {
            _uiState.value = ready.copy(liveTextHint = hint)
        }
    }

    /** Turning it back on starts from scratch: frames from before the pause say nothing about now. */
    private fun onToggleLiveAnalysis() {
        val ready = _uiState.value as? CameraPreviewUiState.Ready ?: return
        recentReadings.clear()
        _uiState.value = ready.copy(
            isLiveAnalysisEnabled = !ready.isLiveAnalysisEnabled,
            liveTextHint = LiveTextHint.Searching
        )
    }

    private fun onScanDocument() {
        val ready = _uiState.value as? CameraPreviewUiState.Ready ?: return
        if (ready.isScanning) return
        _uiState.value = ready.copy(isScanning = true, captureError = null)
        _effects.trySend(CameraPreviewEffect.LaunchDocumentScanner)
    }

    private fun onPagesScanned(intent: CameraPreviewIntent.PagesScanned) {
        viewModelScope.launch {
            runCatching { importScannedPagesUseCase(intent.pageUris, intent.scannedAtEpochMillis) }
                .onSuccess { document ->
                    // lastPhoto is cleared so the two never coexist: whichever the user produced
                    // last is the one the status line describes and "Extract text" acts on.
                    updateReady {
                        it.copy(isScanning = false, lastScan = document, lastPhoto = null, captureError = null)
                    }
                }
                .onFailure {
                    updateReady {
                        it.copy(isScanning = false, captureError = CameraPreviewUiState.CaptureError.Storage)
                    }
                }
        }
    }

    private fun onPhotoCaptured(intent: CameraPreviewIntent.PhotoCaptured) {
        viewModelScope.launch {
            runCatching { saveCapturedPhotoUseCase(intent.jpegBytes, intent.capturedAtEpochMillis) }
                .onSuccess { photo ->
                    updateReady {
                        it.copy(isCapturing = false, lastPhoto = photo, lastScan = null, captureError = null)
                    }
                }
                .onFailure {
                    updateReady {
                        it.copy(isCapturing = false, captureError = CameraPreviewUiState.CaptureError.Storage)
                    }
                }
        }
    }

    private fun updateReady(transform: (CameraPreviewUiState.Ready) -> CameraPreviewUiState.Ready) {
        val ready = _uiState.value as? CameraPreviewUiState.Ready ?: return
        _uiState.value = transform(ready)
    }
}
