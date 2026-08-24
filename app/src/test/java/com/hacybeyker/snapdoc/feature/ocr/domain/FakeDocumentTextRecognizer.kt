package com.hacybeyker.snapdoc.feature.ocr.domain

/** A path with no entry in [blocksByPath] recognizes cleanly and finds nothing, like a blank page. */
class FakeDocumentTextRecognizer(
    private val blocksByPath: Map<String, List<String>> = emptyMap(),
    private val failure: Throwable? = null
) : DocumentTextRecognizer {

    val readPaths = mutableListOf<String>()

    override suspend fun recognizeBlocks(imagePath: String): List<String> {
        failure?.let { throw it }
        readPaths += imagePath
        return blocksByPath[imagePath].orEmpty()
    }
}
