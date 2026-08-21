package com.hacybeyker.snapdoc.feature.camera.ui

import android.util.Log
import androidx.camera.compose.CameraXViewfinder
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier, viewModel: CameraPreviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // Binding CameraX is framework plumbing, not business logic — the same split the permission
    // screen already uses: the Screen owns the platform handles, the ViewModel owns the state.
    // bindToLifecycle must run on the main thread, which LaunchedEffect guarantees.
    LaunchedEffect(lifecycleOwner) {
        runCatching {
            val provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider { request ->
                    surfaceRequest = request
                    viewModel.onIntent(CameraPreviewIntent.ViewfinderReady)
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            provider
        }.onSuccess { provider ->
            cameraProvider = provider
        }.onFailure { cause ->
            Log.e(CAMERA_LOG_TAG, "Could not bind the camera", cause)
            viewModel.onIntent(CameraPreviewIntent.CameraUnavailable)
        }
    }

    // The camera is bound to the Activity's lifecycle, so leaving this screen would otherwise keep
    // it streaming in the background. The key is Unit on purpose: keying on `cameraProvider` would
    // dispose the effect the moment the provider is assigned — reading the fresh value and unbinding
    // everything right after binding it, which left takePicture with no camera.
    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CameraPreviewEffect.TakePicture -> imageCapture.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            image.use { viewModel.onIntent(it.toPhotoCapturedIntent()) }
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e(CAMERA_LOG_TAG, "takePicture failed (code=${exception.imageCaptureError})", exception)
                            viewModel.onIntent(CameraPreviewIntent.CaptureFailed)
                        }
                    }
                )
            }
        }
    }

    CameraPreviewContent(
        uiState = uiState,
        surfaceRequest = surfaceRequest,
        onCaptureClick = { viewModel.onIntent(CameraPreviewIntent.CapturePhoto) },
        modifier = modifier
    )
}

private const val CAMERA_LOG_TAG = "SnapDocCamera"

/** ImageCapture defaults to JPEG output, which arrives whole in the first plane. */
private fun ImageProxy.toPhotoCapturedIntent(): CameraPreviewIntent.PhotoCaptured {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return CameraPreviewIntent.PhotoCaptured(jpegBytes = bytes, capturedAtEpochMillis = System.currentTimeMillis())
}

@Composable
private fun CameraPreviewContent(
    uiState: CameraPreviewUiState,
    surfaceRequest: SurfaceRequest?,
    onCaptureClick: () -> Unit,
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
                onCaptureClick = onCaptureClick
            )
        }
    }
}

@Composable
private fun ReadyContent(
    uiState: CameraPreviewUiState.Ready,
    surfaceRequest: SurfaceRequest?,
    onCaptureClick: () -> Unit
) {
    if (surfaceRequest != null) {
        CameraXViewfinder(surfaceRequest = surfaceRequest, modifier = Modifier.fillMaxSize())
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(MaterialTheme.spacing.lg),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CaptureStatus(uiState = uiState)
        Button(onClick = onCaptureClick, enabled = !uiState.isCapturing) {
            Text(text = stringResource(R.string.camera_preview_shutter))
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
        CameraPreviewContent(uiState = CameraPreviewUiState.Starting, surfaceRequest = null, onCaptureClick = {})
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraPreviewContentUnavailablePreview() {
    SnapDocTheme {
        CameraPreviewContent(uiState = CameraPreviewUiState.Unavailable, surfaceRequest = null, onCaptureClick = {})
    }
}

@ComposePreview(showBackground = true)
@Composable
private fun CameraPreviewContentReadyPreview() {
    SnapDocTheme {
        CameraPreviewContent(
            uiState = CameraPreviewUiState.Ready(),
            surfaceRequest = null,
            onCaptureClick = {}
        )
    }
}
