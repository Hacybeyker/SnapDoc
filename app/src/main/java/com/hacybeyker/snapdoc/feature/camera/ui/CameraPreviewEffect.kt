package com.hacybeyker.snapdoc.feature.camera.ui

/** One-shot side effects: only the Screen holds the CameraX and ML Kit handles. */
sealed interface CameraPreviewEffect {
    data object TakePicture : CameraPreviewEffect
    data object LaunchDocumentScanner : CameraPreviewEffect

    /**
     * The pages are saved and filed, so the camera is done and hands them over. Emitted once per
     * capture, which is what makes it navigation rather than state: staying would only offer to
     * photograph the same page again.
     */
    data class PagesReady(val imagePaths: List<String>) : CameraPreviewEffect
}
