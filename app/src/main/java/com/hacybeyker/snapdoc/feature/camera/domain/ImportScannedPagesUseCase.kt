package com.hacybeyker.snapdoc.feature.camera.domain

import javax.inject.Inject

/**
 * Copies the scanner's pages into the app's own storage. This is not a formality: the files the
 * scanner returns live in a cache it controls and may vanish, so a document that is not copied is a
 * document that can disappear between two launches.
 */
class ImportScannedPagesUseCase @Inject constructor(
    private val scannedPageReader: ScannedPageReader,
    private val saveCapturedPhotoUseCase: SaveCapturedPhotoUseCase
) {

    suspend operator fun invoke(pageUris: List<String>, scannedAtEpochMillis: Long): ScannedDocument {
        require(pageUris.isNotEmpty()) { "A scan must have at least one page" }
        val pages = pageUris.mapIndexed { index, uri ->
            saveCapturedPhotoUseCase(
                jpegBytes = scannedPageReader.readPageBytes(uri),
                capturedAtEpochMillis = scannedAtEpochMillis,
                pageNumber = index + 1
            )
        }
        return ScannedDocument(pages)
    }
}
