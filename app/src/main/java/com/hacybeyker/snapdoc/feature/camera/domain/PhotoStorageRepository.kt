package com.hacybeyker.snapdoc.feature.camera.domain

interface PhotoStorageRepository {
    suspend fun savePhoto(jpegBytes: ByteArray, fileName: String, capturedAtEpochMillis: Long): CapturedPhoto
}
