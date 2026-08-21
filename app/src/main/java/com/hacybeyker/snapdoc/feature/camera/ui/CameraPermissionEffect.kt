package com.hacybeyker.snapdoc.feature.camera.ui

/** One-shot side effects the Screen must perform (launch a system dialog, open Settings). */
sealed interface CameraPermissionEffect {
    data object LaunchPermissionRequest : CameraPermissionEffect
    data object OpenAppSettings : CameraPermissionEffect
}
