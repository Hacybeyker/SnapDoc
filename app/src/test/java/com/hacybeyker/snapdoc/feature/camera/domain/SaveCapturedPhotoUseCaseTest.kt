package com.hacybeyker.snapdoc.feature.camera.domain

import java.time.ZoneId
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class SaveCapturedPhotoUseCaseTest {

    private val repository = FakePhotoStorageRepository()
    private val sut = SaveCapturedPhotoUseCase(
        photoStorageRepository = repository,
        buildScanFileNameUseCase = BuildScanFileNameUseCase(ZoneId.of("America/Lima"))
    )

    @Test
    fun `saves the photo under the generated file name`() = runTest {
        val photo = sut(jpegBytes = byteArrayOf(1, 2, 3), capturedAtEpochMillis = 1_787_250_612_345)

        assertEquals("scan_20260820_133012_345.jpg", photo.fileName)
        assertEquals("/fake/scans/scan_20260820_133012_345.jpg", photo.filePath)
        assertEquals("scan_20260820_133012_345.jpg", repository.savedFileName)
    }

    @Test
    fun `rejects an empty capture without touching storage`() = runTest {
        assertThrows(IllegalArgumentException::class.java) {
            kotlinx.coroutines.runBlocking { sut(jpegBytes = byteArrayOf(), capturedAtEpochMillis = 0) }
        }

        assertNull(repository.savedFileName)
    }
}
