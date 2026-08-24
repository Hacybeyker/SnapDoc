package com.hacybeyker.snapdoc.feature.camera.ui

import android.app.Activity
import android.content.IntentSender
import android.util.Log
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextReading
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/*
 * Everything in this file talks to CameraX, Play services or ML Kit directly. It lives apart from
 * CameraPreviewScreen.kt because the split the project already follows — the Screen owns the platform
 * handles, the ViewModel owns the state — says nothing about the handles sharing a file with the
 * composable tree the user actually sees. Nothing here reaches the ViewModel except pure Kotlin.
 */

internal const val CAMERA_LOG_TAG = "SnapDocCamera"

/** Enough for a multi-page contract without letting a runaway session fill internal storage. */
private const val SCANNER_PAGE_LIMIT = 10

/**
 * Binding CameraX is framework plumbing, not business logic. Returns the surface the viewfinder must
 * draw on, which never reaches the ViewModel because it cannot be built in a JVM test.
 */
@Composable
internal fun rememberBoundCamera(
    imageCapture: ImageCapture,
    imageAnalysis: ImageAnalysis,
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
            provider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageCapture,
                imageAnalysis
            )
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

/**
 * Builds the analysis use case. KEEP_ONLY_LATEST is the whole point: the sensor produces frames far
 * faster than OCR can read them, so CameraX holds exactly one pending frame and drops the rest,
 * leaving the analyzer always working on what the camera sees now instead of falling further behind.
 */
internal fun buildLiveTextAnalysis(): ImageAnalysis = ImageAnalysis.Builder()
    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
    .build()

/**
 * Wires the frame analyzer, which is a pipeline with a throttle — and closing the frame *is* the
 * throttle. Under KEEP_ONLY_LATEST, CameraX hands over the next frame only once the [ImageProxy] is
 * closed: closing before the recognizer finishes would let frames pile into ML Kit and the hint would
 * describe the past; never closing stalls the pipeline outright. So the close lives in the Task's
 * completion listener, which runs whether the frame was read or failed.
 */
@Composable
internal fun LiveTextAnalysis(imageAnalysis: ImageAnalysis, isEnabled: Boolean, onReading: (LiveTextReading) -> Unit) {
    val analysisExecutor = rememberAnalysisExecutor()
    val recognizer = remember { TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS) }
    // Recomposing must not re-register the analyzer just because the lambda is a new instance.
    val currentOnReading by rememberUpdatedState(onReading)

    DisposableEffect(imageAnalysis, isEnabled) {
        if (isEnabled) {
            imageAnalysis.setAnalyzer(analysisExecutor) { imageProxy ->
                analyzeFrame(recognizer, imageProxy) { currentOnReading(it) }
            }
        }
        onDispose { imageAnalysis.clearAnalyzer() }
    }

    DisposableEffect(recognizer) {
        onDispose { recognizer.close() }
    }
}

/** One thread is enough by design: KEEP_ONLY_LATEST keeps exactly one frame in flight at a time. */
@Composable
private fun rememberAnalysisExecutor(): Executor {
    val executor = remember { Executors.newSingleThreadExecutor() }
    DisposableEffect(executor) {
        onDispose { executor.shutdown() }
    }
    return executor
}

/*
 * androidx's OptIn, not Kotlin's: ExperimentalGetImage is an androidx.annotation.experimental marker,
 * which the Kotlin compiler ignores ("@OptIn has no effect") and only Android Lint enforces. Applying
 * the marker itself would work too, but it would then propagate the opt-in to every caller — this
 * form keeps it contained to the one function that actually reads `imageProxy.image`.
 */
@androidx.annotation.OptIn(markerClass = [ExperimentalGetImage::class])
private fun analyzeFrame(recognizer: TextRecognizer, imageProxy: ImageProxy, onReading: (LiveTextReading) -> Unit) {
    val mediaImage = imageProxy.image
    if (mediaImage == null) {
        imageProxy.close()
        return
    }
    recognizer.process(InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees))
        .addOnSuccessListener { text -> onReading(LiveTextReading(blockCount = text.textBlocks.size)) }
        // A single unreadable frame is normal (motion blur, mid-focus); the next one is moments away.
        .addOnFailureListener { cause -> Log.w(CAMERA_LOG_TAG, "Live analysis skipped a frame", cause) }
        .addOnCompleteListener { imageProxy.close() }
}

@Composable
internal fun CameraPreviewEffects(viewModel: CameraPreviewViewModel, imageCapture: ImageCapture) {
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
