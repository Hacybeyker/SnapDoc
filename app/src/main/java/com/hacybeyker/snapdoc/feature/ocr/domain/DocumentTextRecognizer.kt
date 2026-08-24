package com.hacybeyker.snapdoc.feature.ocr.domain

/**
 * Reads the text out of one stored page. The path is a plain String so this contract stays free of
 * Android types; decoding the file and running the model is the data layer's job.
 */
interface DocumentTextRecognizer {

    /** Returns the text blocks found in the image, empty when the page carries no readable text. */
    suspend fun recognizeBlocks(imagePath: String): List<String>
}
