package com.hacybeyker.snapdoc.feature.camera.domain

import javax.inject.Inject

class IsCameraPermissionGrantedUseCase @Inject constructor(
    private val cameraPermissionRepository: CameraPermissionRepository
) {

    operator fun invoke(): Boolean = cameraPermissionRepository.hasCameraPermission()
}
