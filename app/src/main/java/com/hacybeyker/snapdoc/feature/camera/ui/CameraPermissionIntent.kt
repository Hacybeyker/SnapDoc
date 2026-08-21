package com.hacybeyker.snapdoc.feature.camera.ui

sealed interface CameraPermissionIntent {
    data object RequestPermission : CameraPermissionIntent
    data class PermissionResultReceived(val isGranted: Boolean, val shouldShowRationale: Boolean) :
        CameraPermissionIntent
    data object OpenAppSettings : CameraPermissionIntent

    /** The user came back to the screen — the only way to notice a permission granted in Settings. */
    data object ScreenResumed : CameraPermissionIntent
}
