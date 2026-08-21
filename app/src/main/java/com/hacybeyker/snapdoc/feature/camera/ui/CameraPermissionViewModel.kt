package com.hacybeyker.snapdoc.feature.camera.ui

import androidx.lifecycle.ViewModel
import com.hacybeyker.snapdoc.feature.camera.domain.CameraPermissionStatus
import com.hacybeyker.snapdoc.feature.camera.domain.EvaluateCameraPermissionUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.IsCameraPermissionGrantedUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

@HiltViewModel
class CameraPermissionViewModel @Inject constructor(
    private val isCameraPermissionGrantedUseCase: IsCameraPermissionGrantedUseCase,
    private val evaluateCameraPermissionUseCase: EvaluateCameraPermissionUseCase
) : ViewModel() {

    private var hasRequestedPermission = false

    private val _uiState = MutableStateFlow<CameraPermissionUiState>(CameraPermissionUiState.Checking)
    val uiState: StateFlow<CameraPermissionUiState> = _uiState.asStateFlow()

    private val _effects = Channel<CameraPermissionEffect>(Channel.BUFFERED)
    val effects: Flow<CameraPermissionEffect> = _effects.receiveAsFlow()

    init {
        react(isGranted = isCameraPermissionGrantedUseCase(), shouldShowRationale = false)
    }

    fun onIntent(intent: CameraPermissionIntent) {
        when (intent) {
            CameraPermissionIntent.RequestPermission ->
                _effects.trySend(CameraPermissionEffect.LaunchPermissionRequest)

            is CameraPermissionIntent.PermissionResultReceived ->
                react(intent.isGranted, intent.shouldShowRationale)

            CameraPermissionIntent.OpenAppSettings ->
                _effects.trySend(CameraPermissionEffect.OpenAppSettings)

            CameraPermissionIntent.ScreenResumed -> onScreenResumed()
        }
    }

    /**
     * Only the permanently-denied branch is re-evaluated: that is the state a user can leave by
     * flipping the permission in Settings. Re-running the full evaluation on every resume would
     * mistake the first resume (right after `init` already requested) for a denial.
     */
    private fun onScreenResumed() {
        if (_uiState.value != CameraPermissionUiState.PermanentlyDenied) return
        react(isGranted = isCameraPermissionGrantedUseCase(), shouldShowRationale = false)
    }

    private fun react(isGranted: Boolean, shouldShowRationale: Boolean) {
        val status = evaluateCameraPermissionUseCase(
            isGranted = isGranted,
            shouldShowRationale = shouldShowRationale,
            hasRequestedBefore = hasRequestedPermission
        )
        _uiState.value = status.toUiState()
        if (status == CameraPermissionStatus.NotRequested) {
            hasRequestedPermission = true
            _effects.trySend(CameraPermissionEffect.LaunchPermissionRequest)
        }
    }
}

private fun CameraPermissionStatus.toUiState(): CameraPermissionUiState = when (this) {
    CameraPermissionStatus.Granted -> CameraPermissionUiState.Granted
    CameraPermissionStatus.NotRequested -> CameraPermissionUiState.Checking
    CameraPermissionStatus.RationaleRequired -> CameraPermissionUiState.RationaleRequired
    CameraPermissionStatus.PermanentlyDenied -> CameraPermissionUiState.PermanentlyDenied
}
