package com.hacybeyker.snapdoc.feature.camera.domain

fun interface CameraPermissionRepository {
    fun hasCameraPermission(): Boolean
}
