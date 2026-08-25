package com.hacybeyker.snapdoc.feature.library.domain

import javax.inject.Inject

/**
 * Turns a stored scan into a PDF ready to hand to another app, and returns where it landed.
 *
 * Nothing is cached between calls: an export is a throwaway artifact, and reusing a stale one would
 * quietly share the version from before the document was re-read.
 */
class ExportDocumentToPdfUseCase @Inject constructor(
    private val pdfExporter: PdfExporter,
    private val buildPdfFileNameUseCase: BuildPdfFileNameUseCase
) {

    suspend operator fun invoke(document: StoredDocument): String {
        require(document.imagePaths.isNotEmpty()) { "A document with no pages cannot be exported" }
        val fileName = buildPdfFileNameUseCase(
            kind = document.insight.kind,
            merchant = document.insight.merchant,
            createdAtEpochMillis = document.createdAtEpochMillis
        )
        return pdfExporter.export(fileName = fileName, imagePaths = document.imagePaths)
    }
}
