package com.hacybeyker.snapdoc.feature.camera.ui

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.components.ScannerFrame
import com.hacybeyker.snapdoc.core.ui.components.Spacer
import com.hacybeyker.snapdoc.core.ui.theme.CameraOnScrim
import com.hacybeyker.snapdoc.core.ui.theme.CameraOnScrimMuted
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextHint

/**
 * @param onPagesReady the pages this screen produced, already saved and filed. It fires on its own
 * as soon as a capture or a scan finishes — the camera has nothing left to say at that point, and
 * leaving the user in front of a viewfinder with their document somewhere off screen was the reason
 * the flow felt like it stopped halfway.
 */
@Composable
fun CameraPreviewScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPagesReady: (List<String>) -> Unit = {},
    viewModel: CameraPreviewViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val imageAnalysis = remember { buildLiveTextAnalysis() }
    val surfaceRequest = rememberBoundCamera(
        imageCapture = imageCapture,
        imageAnalysis = imageAnalysis,
        onViewfinderReady = { viewModel.onIntent(CameraPreviewIntent.ViewfinderReady) },
        onCameraUnavailable = { viewModel.onIntent(CameraPreviewIntent.CameraUnavailable) }
    )

    LiveTextAnalysis(
        imageAnalysis = imageAnalysis,
        isEnabled = (uiState as? CameraPreviewUiState.Ready)?.isLiveAnalysisEnabled == true,
        onReading = { viewModel.onIntent(CameraPreviewIntent.FrameAnalyzed(it)) }
    )

    CameraPreviewEffects(viewModel = viewModel, imageCapture = imageCapture, onPagesReady = onPagesReady)

    CameraPreviewContent(
        uiState = uiState,
        surfaceRequest = surfaceRequest,
        onBackClick = onBack,
        onCaptureClick = { viewModel.onIntent(CameraPreviewIntent.CapturePhoto) },
        onScanClick = { viewModel.onIntent(CameraPreviewIntent.ScanDocument) },
        onToggleLiveAnalysisClick = { viewModel.onIntent(CameraPreviewIntent.ToggleLiveAnalysis) },
        modifier = modifier
    )
}

@Composable
internal fun CameraPreviewContent(
    uiState: CameraPreviewUiState,
    surfaceRequest: SurfaceRequest?,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit,
    onToggleLiveAnalysisClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        when (uiState) {
            CameraPreviewUiState.Starting -> StartingContent()

            CameraPreviewUiState.Unavailable -> UnavailableContent()

            is CameraPreviewUiState.Ready -> ReadyContent(
                uiState = uiState,
                surfaceRequest = surfaceRequest,
                onBackClick = onBackClick,
                onCaptureClick = onCaptureClick,
                onScanClick = onScanClick,
                onToggleLiveAnalysisClick = onToggleLiveAnalysisClick
            )
        }
        // Ready puts its own back button in the status bar; the other two have no chrome to hold one,
        // and the unavailable state is a dead end without it.
        if (uiState !is CameraPreviewUiState.Ready) {
            CameraBackButton(
                onClick = onBackClick,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top)
                    )
                    .padding(MaterialTheme.spacing.md)
            )
        }
    }
}

/** A spinner alone on a black screen reads as a crash, so it says what it is waiting for. */
@Composable
private fun StartingContent() {
    Column(
        modifier = Modifier.padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = CameraOnScrim)
        Spacer(MaterialTheme.spacing.md)
        Text(
            text = stringResource(R.string.camera_starting),
            style = MaterialTheme.typography.bodyMedium,
            color = CameraOnScrimMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * The dead end gets the same shape as every other empty state in the app — icon, title, explanation —
 * instead of a lone sentence floating on black, which looked like the screen had failed to load.
 */
@Composable
private fun UnavailableContent() {
    Column(
        modifier = Modifier.padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Filled.Warning,
            contentDescription = null,
            tint = CameraOnScrimMuted,
            modifier = Modifier.size(UNAVAILABLE_ICON)
        )
        Spacer(MaterialTheme.spacing.md)
        Text(
            text = stringResource(R.string.camera_preview_unavailable_title),
            style = MaterialTheme.typography.titleMedium,
            color = CameraOnScrim,
            textAlign = TextAlign.Center
        )
        Spacer(MaterialTheme.spacing.xs)
        Text(
            text = stringResource(R.string.camera_preview_unavailable),
            style = MaterialTheme.typography.bodyMedium,
            color = CameraOnScrimMuted,
            textAlign = TextAlign.Center
        )
    }
}

/**
 * Three bands over a full-bleed viewfinder: status, aiming area, controls.
 *
 * The insets are applied *inside* the two scrim bands rather than to the column as a whole. Padding
 * the column pulled both scrims away from the screen edges, leaving a strip of bare camera above the
 * status bar and below the controls — the panel looked like it was floating rather than anchored.
 */
@Composable
private fun ReadyContent(
    uiState: CameraPreviewUiState.Ready,
    surfaceRequest: SurfaceRequest?,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit,
    onToggleLiveAnalysisClick: () -> Unit
) {
    if (surfaceRequest != null) {
        CameraXViewfinder(surfaceRequest = surfaceRequest, modifier = Modifier.fillMaxSize())
    }
    Column(modifier = Modifier.fillMaxSize()) {
        LiveTextBar(
            uiState = uiState,
            onBackClick = onBackClick,
            onToggleClick = onToggleLiveAnalysisClick,
            modifier = Modifier.fillMaxWidth()
        )
        // Only the free camera area, not the whole screen: at full size the two bottom brackets were
        // drawn underneath the controls panel, so the frame never looked closed.
        ScannerFrame(
            isTextDetected = uiState.liveTextHint is LiveTextHint.TextVisible,
            accent = MaterialTheme.colorScheme.primary,
            modifier = Modifier.fillMaxWidth().weight(1f)
        )
        CameraControls(
            uiState = uiState,
            onCaptureClick = onCaptureClick,
            onScanClick = onScanClick,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

private val UNAVAILABLE_ICON = 40.dp

@ComposePreview(showBackground = true)
@Composable
private fun CameraReadyPreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Ready(liveTextHint = LiveTextHint.TextVisible(4)),
            surfaceRequest = null,
            onBackClick = {},
            onCaptureClick = {},
            onScanClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraScanningPreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Ready(
                isScanning = true,
                isLiveAnalysisEnabled = false,
                liveTextHint = LiveTextHint.Searching
            ),
            surfaceRequest = null,
            onBackClick = {},
            onCaptureClick = {},
            onScanClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraErrorPreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Ready(
                captureError = CameraPreviewUiState.CaptureError.Scanner,
                liveTextHint = LiveTextHint.NoTextVisible
            ),
            surfaceRequest = null,
            onBackClick = {},
            onCaptureClick = {},
            onScanClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraUnavailablePreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Unavailable,
            surfaceRequest = null,
            onBackClick = {},
            onCaptureClick = {},
            onScanClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}
