package com.hacybeyker.snapdoc.feature.camera.domain

import java.io.FileNotFoundException
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ImportScannedPagesUseCaseTest {

    private val reader = FakeScannedPageReader()
    private val storage = FakePhotoStorageRepository()

    private fun useCase(pageReader: ScannedPageReader = reader) = ImportScannedPagesUseCase(
        scannedPageReader = pageReader,
        saveCapturedPhotoUseCase = SaveCapturedPhotoUseCase(
            photoStorageRepository = storage,
            buildScanFileNameUseCase = BuildScanFileNameUseCase(ZoneId.of("America/Lima"))
        ),
        photoStorageRepository = storage
    )

    @Test
    fun `imports every page and numbers them so one scan does not overwrite itself`() = runTest {
        val document = useCase()(
            pageUris = listOf("content://scan/1", "content://scan/2"),
            scannedAtEpochMillis = 1_787_250_612_345
        )

        assertEquals(2, document.pageCount)
        assertEquals(
            listOf("scan_20260820_133012_345_p1.jpg", "scan_20260820_133012_345_p2.jpg"),
            document.pages.map { it.fileName }
        )
    }

    @Test
    fun `reads the pages the scanner reported, in order`() = runTest {
        useCase()(pageUris = listOf("content://scan/a", "content://scan/b"), scannedAtEpochMillis = 0)

        assertEquals(listOf("content://scan/a", "content://scan/b"), reader.readUris)
    }

    @Test
    fun `a scan without pages is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { useCase()(pageUris = emptyList(), scannedAtEpochMillis = 0) }
        }
    }

    @Test
    fun `a page the scanner no longer owns fails the import`() {
        val unavailable = FakeScannedPageReader(FileNotFoundException("gone"))

        assertThrows(FileNotFoundException::class.java) {
            runBlocking { useCase(unavailable)(listOf("content://scan/1"), scannedAtEpochMillis = 0) }
        }
    }

    @Test
    fun `a page that fails halfway deletes the pages already written`() {
        val failsOnSecondPage = FakeScannedPageReader(
            failure = FileNotFoundException("gone"),
            failOnUri = "content://scan/2"
        )

        assertThrows(FileNotFoundException::class.java) {
            runBlocking {
                useCase(failsOnSecondPage)(
                    pageUris = listOf("content://scan/1", "content://scan/2"),
                    scannedAtEpochMillis = 1_787_250_612_345
                )
            }
        }

        assertEquals(listOf("scan_20260820_133012_345_p1.jpg"), storage.deletedFileNames)
        assertEquals(emptyList<String>(), storage.storedFileNames)
    }

    @Test
    fun `a rollback that cannot delete still reports why the import failed`() {
        val undeletable = object : PhotoStorageRepository by storage {
            override suspend fun deletePhoto(fileName: String) = throw IllegalStateException("read-only")
        }
        val useCase = ImportScannedPagesUseCase(
            scannedPageReader = FakeScannedPageReader(FileNotFoundException("gone"), "content://scan/2"),
            saveCapturedPhotoUseCase = SaveCapturedPhotoUseCase(
                photoStorageRepository = storage,
                buildScanFileNameUseCase = BuildScanFileNameUseCase(ZoneId.of("America/Lima"))
            ),
            photoStorageRepository = undeletable
        )

        assertThrows(FileNotFoundException::class.java) {
            runBlocking { useCase(listOf("content://scan/1", "content://scan/2"), scannedAtEpochMillis = 0) }
        }
    }
}
