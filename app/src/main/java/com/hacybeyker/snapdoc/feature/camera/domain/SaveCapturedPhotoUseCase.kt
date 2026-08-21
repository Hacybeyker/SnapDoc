package com.hacybeyker.snapdoc.feature.camera.domain

import javax.inject.Inject

class SaveCapturedPhotoUseCase @Inject constructor(
    private val photoStorageRepository: PhotoStorageRepository,
    private val buildScanFileNameUseCase: BuildScanFileNameUseCase
) {

    /**
     * An empty buffer means CameraX handed back a frame with no payload — writing it would leave a
     * 0-byte file on disk that later stages (OCR) would happily try to decode. Fail here instead.
     */
    suspend operator fun invoke(jpegBytes: ByteArray, capturedAtEpochMillis: Long): CapturedPhoto {
        require(jpegBytes.isNotEmpty()) { "Cannot save an empty photo" }
        val fileName = buildScanFileNameUseCase(capturedAtEpochMillis)
        return photoStorageRepository.savePhoto(jpegBytes, fileName, capturedAtEpochMillis)
    }
}
