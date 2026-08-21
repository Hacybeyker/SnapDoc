package com.hacybeyker.snapdoc.feature.camera.ui

sealed interface CameraPermissionUiState {
    data object Checking : CameraPermissionUiState
    data object Granted : CameraPermissionUiState
    data object RationaleRequired : CameraPermissionUiState
    data object PermanentlyDenied : CameraPermissionUiState
}
