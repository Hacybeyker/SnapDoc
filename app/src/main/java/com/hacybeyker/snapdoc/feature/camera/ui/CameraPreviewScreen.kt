package com.hacybeyker.snapdoc.feature.camera.ui

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview as ComposePreview
import androidx.core.content.ContextCompat
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing

private const val CAMERA_LOG_TAG = "SnapDocCamera"

/** Enough for a multi-page contract without letting a runaway session fill internal storage. */
private const val SCANNER_PAGE_LIMIT = 10

@Composable
fun CameraPreviewScreen(modifier: Modifier = Modifier, viewModel: CameraPreviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val surfaceRequest = rememberBoundCamera(
        imageCapture = imageCapture,
        onViewfinderReady = { viewModel.onIntent(CameraPreviewIntent.ViewfinderReady) },
        onCameraUnavailable = { viewModel.onIntent(CameraPreviewIntent.CameraUnavailable) }
    )

    CameraPreviewEffects(viewModel = viewModel, imageCapture = imageCapture)

    CameraPreviewContent(
        uiState = uiState,
        surfaceRequest = surfaceRequest,
        onCaptureClick = { viewModel.onIntent(CameraPreviewIntent.CapturePhoto) },
        onScanClick = { viewModel.onIntent(CameraPreviewIntent.ScanDocument) },
        modifier = modifier
    )
}

/**
 * Binding CameraX is framework plumbing, not business logic — the same split the permission screen
 * uses: the Screen owns the platform handles, the ViewModel owns the state. Returns the surface the
 * viewfinder must draw on, which never reaches the ViewModel because it cannot be built in a test.
 */
@Composable
private fun rememberBoundCamera(
    imageCapture: ImageCapture,
    onViewfinderReady: () -> Unit,
    onCameraUnavailable: () -> Unit
): SurfaceRequest? {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var surfaceRequest by remember { mutableStateOf<SurfaceRequest?>(null) }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    // bindToLifecycle must run on the main thread, which LaunchedEffect guarantees.
    LaunchedEffect(lifecycleOwner) {
        runCatching {
            val provider = ProcessCameraProvider.awaitInstance(context)
            val preview = Preview.Builder().build().apply {
                setSurfaceProvider { request ->
                    surfaceRequest = request
                    onViewfinderReady()
                }
            }
            provider.unbindAll()
            provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            provider
        }.onSuccess { provider ->
            cameraProvider = provider
        }.onFailure { cause ->
            Log.e(CAMERA_LOG_TAG, "Could not bind the camera", cause)
            onCameraUnavailable()
        }
    }

    // The camera is bound to the Activity's lifecycle, so leaving this screen would otherwise keep
    // it streaming in the background. The key is Unit on purpose: keying on `cameraProvider` would
    // dispose the effect the moment the provider is assigned — reading the fresh value and unbinding
    // everything right after binding it, which left takePicture with no camera.
    DisposableEffect(Unit) {
        onDispose { cameraProvider?.unbindAll() }
    }

    return surfaceRequest
}

@Composable
private fun CameraPreviewEffects(viewModel: CameraPreviewViewModel, imageCapture: ImageCapture) {
    val context = LocalContext.current
    val activity = LocalActivity.current

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val pages = GmsDocumentScanningResult.fromActivityResultIntent(result.data)?.pages
        if (result.resultCode != Activity.RESULT_OK || pages.isNullOrEmpty()) {
            viewModel.onIntent(CameraPreviewIntent.ScanDismissed)
        } else {
            viewModel.onIntent(
                CameraPreviewIntent.PagesScanned(
                    pageUris = pages.map { it.imageUri.toString() },
                    scannedAtEpochMillis = System.currentTimeMillis()
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CameraPreviewEffect.LaunchDocumentScanner -> launchDocumentScanner(
                    activity = activity,
                    onIntentSender = { scannerLauncher.launch(IntentSenderRequest.Builder(it).build()) },
                    onFailure = { viewModel.onIntent(CameraPreviewIntent.ScanFailed) }
                )

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
}

/**
 * The guided scanner runs in its own Play services activity, so the app hands over control and gets
 * back cropped, deskewed pages instead of reimplementing edge detection. Asking for the intent is
 * asynchronous because Play services may have to download the scanner module first — and that Task
 * is exactly where a device without Play services fails.
 */
private fun launchDocumentScanner(activity: Activity?, onIntentSender: (IntentSender) -> Unit, onFailure: () -> Unit) {
    if (activity == null) {
        Log.e(CAMERA_LOG_TAG, "No activity available to launch the document scanner")
        onFailure()
        return
    }
    val options = GmsDocumentScannerOptions.Builder()
        .setScannerMode(GmsDocumentScannerOptions.SCANNER_MODE_FULL)
        .setResultFormats(GmsDocumentScannerOptions.RESULT_FORMAT_JPEG)
        .setGalleryImportAllowed(true)
        .setPageLimit(SCANNER_PAGE_LIMIT)
        .build()
    GmsDocumentScanning.getClient(options)
        .getStartScanIntent(activity)
        .addOnSuccessListener(onIntentSender)
        .addOnFailureListener { cause ->
            Log.e(CAMERA_LOG_TAG, "Could not start the document scanner", cause)
            onFailure()
        }
}

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
    onScanClick: () -> Unit,
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
                onScanClick = onScanClick
            )
        }
    }
}

@Composable
private fun ReadyContent(
    uiState: CameraPreviewUiState.Ready,
    surfaceRequest: SurfaceRequest?,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit
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
        Button(onClick = onScanClick, enabled = !uiState.isScanning) {
            Text(text = stringResource(R.string.camera_preview_scan_document))
        }
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        OutlinedButton(onClick = onCaptureClick, enabled = !uiState.isCapturing) {
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
            onScanClick = {}
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
            onScanClick = {}
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
            onScanClick = {}
        )
    }
}
