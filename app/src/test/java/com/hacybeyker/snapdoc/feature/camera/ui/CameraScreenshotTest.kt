package com.hacybeyker.snapdoc.feature.camera.ui

import com.hacybeyker.snapdoc.core.test.ScreenshotTest
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextHint
import org.junit.Test

/**
 * The camera chrome is where the visual bugs of this project have actually happened — scrims that
 * stopped short of the screen edge, a scanner frame drawn under the controls, a state with no way
 * out. `surfaceRequest = null` renders everything except the feed itself, which is exactly the part
 * a golden can hold still.
 */
class CameraScreenshotTest : ScreenshotTest() {

    private fun preview(uiState: CameraPreviewUiState) = capture {
        CameraPreviewContent(
            uiState = uiState,
            surfaceRequest = null,
            onBackClick = {},
            onCaptureClick = {},
            onScanClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }

    private fun permission(uiState: CameraPermissionUiState) = capture {
        CameraPermissionContent(
            uiState = uiState,
            onRequestPermission = {},
            onOpenSettings = {},
            onBack = {},
            grantedContent = {}
        )
    }

    @Test
    fun camera_readyWithTextInView() = preview(CameraPreviewUiState.Ready(liveTextHint = LiveTextHint.TextVisible(4)))

    @Test
    fun camera_liveAnalysisOff() = preview(
        CameraPreviewUiState.Ready(isLiveAnalysisEnabled = false, liveTextHint = LiveTextHint.Searching)
    )

    @Test
    fun camera_scanning() = preview(
        CameraPreviewUiState.Ready(isScanning = true, liveTextHint = LiveTextHint.NoTextVisible)
    )

    @Test
    fun camera_captureFailed() = preview(
        CameraPreviewUiState.Ready(
            captureError = CameraPreviewUiState.CaptureError.Scanner,
            liveTextHint = LiveTextHint.NoTextVisible
        )
    )

    @Test
    fun camera_unavailable() = preview(CameraPreviewUiState.Unavailable)

    @Test
    fun permission_rationale() = permission(CameraPermissionUiState.RationaleRequired)

    @Test
    fun permission_permanentlyDenied() = permission(CameraPermissionUiState.PermanentlyDenied)
}
