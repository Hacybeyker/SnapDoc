package com.hacybeyker.snapdoc.feature.camera.domain

import javax.inject.Inject

/**
 * Android's permission APIs can't tell "never asked" apart from "denied forever" on their own:
 * `shouldShowRequestPermissionRationale` returns `false` for both. [hasRequestedBefore] is the
 * caller-tracked flag that breaks the tie.
 */
class EvaluateCameraPermissionUseCase @Inject constructor() {

    operator fun invoke(
        isGranted: Boolean,
        shouldShowRationale: Boolean,
        hasRequestedBefore: Boolean
    ): CameraPermissionStatus = when {
        isGranted -> CameraPermissionStatus.Granted
        shouldShowRationale -> CameraPermissionStatus.RationaleRequired
        hasRequestedBefore -> CameraPermissionStatus.PermanentlyDenied
        else -> CameraPermissionStatus.NotRequested
    }
}
