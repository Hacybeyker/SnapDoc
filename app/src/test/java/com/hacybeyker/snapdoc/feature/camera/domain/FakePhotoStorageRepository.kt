package com.hacybeyker.snapdoc.feature.camera.domain

class FakePhotoStorageRepository(private val failure: Throwable? = null) : PhotoStorageRepository {

    var savedBytes: ByteArray? = null
        private set
    var savedFileName: String? = null
        private set

    /** What the fake still holds, so a test can assert a rollback left nothing behind. */
    val storedFileNames = mutableListOf<String>()
    val deletedFileNames = mutableListOf<String>()

    override suspend fun savePhoto(jpegBytes: ByteArray, fileName: String, capturedAtEpochMillis: Long): CapturedPhoto {
        failure?.let { throw it }
        savedBytes = jpegBytes
        savedFileName = fileName
        storedFileNames += fileName
        return CapturedPhoto(
            filePath = "/fake/scans/$fileName",
            fileName = fileName,
            capturedAtEpochMillis = capturedAtEpochMillis
        )
    }

    override suspend fun deletePhoto(fileName: String) {
        storedFileNames -= fileName
        deletedFileNames += fileName
    }
}
