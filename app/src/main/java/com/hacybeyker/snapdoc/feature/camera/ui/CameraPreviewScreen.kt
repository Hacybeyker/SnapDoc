package com.hacybeyker.snapdoc.feature.camera.ui

import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.ImageCapture
import androidx.camera.core.SurfaceRequest
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextHint

@Composable
fun CameraPreviewScreen(
    modifier: Modifier = Modifier,
    onExtractText: (List<String>) -> Unit = {},
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

    CameraPreviewEffects(viewModel = viewModel, imageCapture = imageCapture)

    CameraPreviewContent(
        uiState = uiState,
        surfaceRequest = surfaceRequest,
        onCaptureClick = { viewModel.onIntent(CameraPreviewIntent.CapturePhoto) },
        onScanClick = { viewModel.onIntent(CameraPreviewIntent.ScanDocument) },
        onExtractTextClick = onExtractText,
        onToggleLiveAnalysisClick = { viewModel.onIntent(CameraPreviewIntent.ToggleLiveAnalysis) },
        modifier = modifier
    )
}

@Composable
private fun CameraPreviewContent(
    uiState: CameraPreviewUiState,
    surfaceRequest: SurfaceRequest?,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit,
    onExtractTextClick: (List<String>) -> Unit,
    onToggleLiveAnalysisClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            CameraPreviewUiState.Starting -> CircularProgressIndicator()

            CameraPreviewUiState.Unavailable -> Text(
                text = stringResource(R.string.camera_preview_unavailable),
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(MaterialTheme.spacing.lg)
            )

            is CameraPreviewUiState.Ready -> ReadyContent(
                uiState = uiState,
                surfaceRequest = surfaceRequest,
                onCaptureClick = onCaptureClick,
                onScanClick = onScanClick,
                onExtractTextClick = onExtractTextClick,
                onToggleLiveAnalysisClick = onToggleLiveAnalysisClick
            )
        }
    }
}

@Composable
private fun ReadyContent(
    uiState: CameraPreviewUiState.Ready,
    surfaceRequest: SurfaceRequest?,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit,
    onExtractTextClick: (List<String>) -> Unit,
    onToggleLiveAnalysisClick: () -> Unit
) {
    if (surfaceRequest != null) {
        CameraXViewfinder(surfaceRequest = surfaceRequest, modifier = Modifier.fillMaxSize())
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.lg),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LiveTextStatus(
            hint = uiState.liveTextHint,
            isEnabled = uiState.isLiveAnalysisEnabled,
            onToggleClick = onToggleLiveAnalysisClick
        )
        CaptureStatus(uiState = uiState)
        CaptureActions(
            uiState = uiState,
            onCaptureClick = onCaptureClick,
            onScanClick = onScanClick,
            onExtractTextClick = onExtractTextClick
        )
    }
}

@Composable
private fun CaptureActions(
    uiState: CameraPreviewUiState.Ready,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit,
    onExtractTextClick: (List<String>) -> Unit
) {
    // Only offered once something is on disk — there is nothing to read text from before that.
    val imagePaths = uiState.lastImagePaths
    if (imagePaths.isNotEmpty()) {
        Button(onClick = { onExtractTextClick(imagePaths) }) {
            Text(text = stringResource(R.string.camera_preview_extract_text))
        }
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
    }
    Button(onClick = onScanClick, enabled = !uiState.isScanning) {
        Text(text = stringResource(R.string.camera_preview_scan_document))
    }
    Spacer(Modifier.height(MaterialTheme.spacing.sm))
    OutlinedButton(onClick = onCaptureClick, enabled = !uiState.isCapturing) {
        Text(text = stringResource(R.string.camera_preview_shutter))
    }
}

@Composable
private fun LiveTextStatus(hint: LiveTextHint, isEnabled: Boolean, onToggleClick: () -> Unit) {
    val label = when {
        !isEnabled -> stringResource(R.string.camera_preview_live_off)
        hint is LiveTextHint.NoTextVisible -> stringResource(R.string.camera_preview_live_no_text)
        hint is LiveTextHint.TextVisible -> pluralStringResource(
            R.plurals.camera_preview_live_text_visible,
            hint.blockCount,
            hint.blockCount
        )

        else -> stringResource(R.string.camera_preview_live_searching)
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        TextButton(onClick = onToggleClick) {
            Text(
                text = stringResource(
                    if (isEnabled) R.string.camera_preview_live_turn_off else R.string.camera_preview_live_turn_on
                )
            )
        }
    }
}

@Composable
private fun CaptureStatus(uiState: CameraPreviewUiState.Ready) {
    val statusText = when {
        uiState.captureError == CameraPreviewUiState.CaptureError.Camera ->
            stringResource(R.string.camera_preview_capture_failed)

        uiState.captureError == CameraPreviewUiState.CaptureError.Storage ->
            stringResource(R.string.camera_preview_save_failed)

        uiState.captureError == CameraPreviewUiState.CaptureError.Scanner ->
            stringResource(R.string.camera_preview_scanner_unavailable)

        uiState.lastScan != null -> pluralStringResource(
            R.plurals.camera_preview_pages_scanned,
            uiState.lastScan.pageCount,
            uiState.lastScan.pageCount
        )

        uiState.lastPhoto != null -> stringResource(R.string.camera_preview_photo_saved, uiState.lastPhoto.fileName)
        else -> null
    }
    if (statusText != null) {
        Text(
            text = statusText,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = MaterialTheme.spacing.sm)
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraPreviewContentStartingPreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Starting,
            surfaceRequest = null,
            onCaptureClick = {},
            onScanClick = {},
            onExtractTextClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraPreviewContentUnavailablePreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Unavailable,
            surfaceRequest = null,
            onCaptureClick = {},
            onScanClick = {},
            onExtractTextClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraPreviewContentReadyPreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Ready(),
            surfaceRequest = null,
            onCaptureClick = {},
            onScanClick = {},
            onExtractTextClick = {},
            onToggleLiveAnalysisClick = {}
        )
    }
}
