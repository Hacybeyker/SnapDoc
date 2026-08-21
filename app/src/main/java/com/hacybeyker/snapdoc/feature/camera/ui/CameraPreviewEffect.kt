package com.hacybeyker.snapdoc.feature.camera.ui

/** One-shot side effect: only the Screen holds the CameraX ImageCapture instance. */
sealed interface CameraPreviewEffect {
    data object TakePicture : CameraPreviewEffect
}
