package com.hacybeyker.snapdoc.feature.camera.domain

/** `granted` is a `var` so a test can simulate the user flipping the permission in Settings. */
class FakeCameraPermissionRepository(var granted: Boolean = false) : CameraPermissionRepository {
    override fun hasCameraPermission(): Boolean = granted
}
