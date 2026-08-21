package com.hacybeyker.snapdoc.feature.camera.domain

class FakePhotoStorageRepository(private val failure: Throwable? = null) : PhotoStorageRepository {

    var savedBytes: ByteArray? = null
        private set
    var savedFileName: String? = null
        private set

    override suspend fun savePhoto(jpegBytes: ByteArray, fileName: String, capturedAtEpochMillis: Long): CapturedPhoto {
        failure?.let { throw it }
        savedBytes = jpegBytes
        savedFileName = fileName
        return CapturedPhoto(
            filePath = "/fake/scans/$fileName",
            fileName = fileName,
            capturedAtEpochMillis = capturedAtEpochMillis
        )
    }
}
