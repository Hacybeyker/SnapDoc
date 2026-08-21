package com.hacybeyker.snapdoc.feature.camera.domain

class FakeCameraPermissionRepository(private val granted: Boolean = false) : CameraPermissionRepository {
    override fun hasCameraPermission(): Boolean = granted
}
