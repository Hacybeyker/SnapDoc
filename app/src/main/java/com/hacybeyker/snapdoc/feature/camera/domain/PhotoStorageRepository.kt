package com.hacybeyker.snapdoc.feature.camera.domain

interface PhotoStorageRepository {
    suspend fun savePhoto(jpegBytes: ByteArray, fileName: String, capturedAtEpochMillis: Long): CapturedPhoto

    /** Removing a page again is what lets a half-finished import undo itself. */
    suspend fun deletePhoto(fileName: String)
}
