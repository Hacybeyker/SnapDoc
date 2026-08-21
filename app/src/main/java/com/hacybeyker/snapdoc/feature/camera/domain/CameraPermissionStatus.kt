package com.hacybeyker.snapdoc.feature.camera.domain

sealed interface CameraPermissionStatus {
    data object Granted : CameraPermissionStatus
    data object NotRequested : CameraPermissionStatus
    data object RationaleRequired : CameraPermissionStatus
    data object PermanentlyDenied : CameraPermissionStatus
}
