package com.hacybeyker.snapdoc.feature.camera.domain

interface CameraPermissionRepository {
    fun hasCameraPermission(): Boolean
}
