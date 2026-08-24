package com.hacybeyker.snapdoc.feature.camera.ui

/** One-shot side effects: only the Screen holds the CameraX and ML Kit handles. */
sealed interface CameraPreviewEffect {
    data object TakePicture : CameraPreviewEffect
    data object LaunchDocumentScanner : CameraPreviewEffect
}
