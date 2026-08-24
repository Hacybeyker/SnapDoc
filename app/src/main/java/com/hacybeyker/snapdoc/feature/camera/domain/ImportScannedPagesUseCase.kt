package com.hacybeyker.snapdoc.feature.camera.domain

import javax.inject.Inject

/**
 * Copies the scanner's pages into the app's own storage. This is not a formality: the files the
 * scanner returns live in a cache it controls and may vanish, so a document that is not copied is a
 * document that can disappear between two launches.
 */
class ImportScannedPagesUseCase @Inject constructor(
    private val scannedPageReader: ScannedPageReader,
    private val saveCapturedPhotoUseCase: SaveCapturedPhotoUseCase,
    private val photoStorageRepository: PhotoStorageRepository
) {

    /**
     * The import is all-or-nothing. Nothing references a page on its own — the UI only ever receives
     * a whole [ScannedDocument] — so pages written before a mid-way failure would be unreachable
     * bytes taking up internal storage forever. A failure deletes what this run wrote and rethrows.
     */
    suspend operator fun invoke(pageUris: List<String>, scannedAtEpochMillis: Long): ScannedDocument {
        require(pageUris.isNotEmpty()) { "A scan must have at least one page" }
        val imported = mutableListOf<CapturedPhoto>()
        return runCatching {
            pageUris.forEachIndexed { index, uri ->
                imported += saveCapturedPhotoUseCase(
                    jpegBytes = scannedPageReader.readPageBytes(uri),
                    capturedAtEpochMillis = scannedAtEpochMillis,
                    pageNumber = index + 1
                )
            }
            ScannedDocument(imported.toList())
        }.onFailure { discardQuietly(imported) }.getOrThrow()
    }

    /**
     * A rollback that throws would replace the error that caused it, hiding why the import actually
     * failed. Leftover bytes are the lesser problem, so each delete is allowed to fail on its own.
     */
    private suspend fun discardQuietly(pages: List<CapturedPhoto>) {
        pages.forEach { page -> runCatching { photoStorageRepository.deletePhoto(page.fileName) } }
    }
}
