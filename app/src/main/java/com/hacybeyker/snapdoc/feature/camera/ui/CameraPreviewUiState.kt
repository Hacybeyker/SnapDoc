package com.hacybeyker.snapdoc.feature.camera.ui

import com.hacybeyker.snapdoc.feature.camera.domain.CapturedPhoto
import com.hacybeyker.snapdoc.feature.camera.domain.ScannedDocument

/**
 * Deliberately free of CameraX types: the viewfinder's `SurfaceRequest` is a platform handle with
 * identity semantics that cannot be built in a JVM test, so it stays in the Screen (next to the
 * `ImageCapture` instance) and this state models only what is worth asserting on.
 */
sealed interface CameraPreviewUiState {
    data object Starting : CameraPreviewUiState

    data class Ready(
        val isCapturing: Boolean = false,
        val isScanning: Boolean = false,
        val lastPhoto: CapturedPhoto? = null,
        val lastScan: ScannedDocument? = null,
        val captureError: CaptureError? = null
    ) : CameraPreviewUiState {

        /**
         * The pages "Extract text" would read. A capture and a scan clear each other in the
         * ViewModel, so only one of the two is ever set and "the last thing produced" is unambiguous.
         */
        val lastImagePaths: List<String>
            get() = lastScan?.pages?.map { it.filePath } ?: listOfNotNull(lastPhoto?.filePath)
    }

    /** Kept apart because they fail for unrelated reasons and the user can act on only one of them. */
    enum class CaptureError { Camera, Storage, Scanner }

    data object Unavailable : CameraPreviewUiState
}
