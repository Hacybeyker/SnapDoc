package com.hacybeyker.snapdoc.feature.library.domain

/**
 * Renders a document's pages into a PDF and returns where it was written. The path is a plain String
 * so the contract stays free of Android types; turning it into something shareable is the UI's job.
 */
interface PdfExporter {

    suspend fun export(fileName: String, imagePaths: List<String>): String
}
