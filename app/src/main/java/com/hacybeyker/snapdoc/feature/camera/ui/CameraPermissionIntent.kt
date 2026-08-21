package com.hacybeyker.snapdoc.feature.camera.ui

sealed interface CameraPermissionIntent {
    data object RequestPermission : CameraPermissionIntent
    data class PermissionResultReceived(val isGranted: Boolean, val shouldShowRationale: Boolean) :
        CameraPermissionIntent
    data object OpenAppSettings : CameraPermissionIntent
}
