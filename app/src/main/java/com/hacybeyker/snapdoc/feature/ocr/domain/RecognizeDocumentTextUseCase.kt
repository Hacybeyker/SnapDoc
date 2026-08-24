package com.hacybeyker.snapdoc.feature.ocr.domain

import javax.inject.Inject

/**
 * Runs OCR over every page of a scan and returns them as one document.
 *
 * Pages are processed one after another on purpose. The recognizer holds a single model instance and
 * the phase's whole point is that inference stays on the device, so firing every page at once would
 * compete for the same hardware without finishing any sooner — and would make the failure of one
 * page harder to attribute.
 */
class RecognizeDocumentTextUseCase @Inject constructor(private val documentTextRecognizer: DocumentTextRecognizer) {

    suspend operator fun invoke(imagePaths: List<String>): RecognizedDocument {
        require(imagePaths.isNotEmpty()) { "There is no page to read text from" }
        val pages = imagePaths.mapIndexed { index, path ->
            RecognizedPage(pageNumber = index + 1, blocks = documentTextRecognizer.recognizeBlocks(path))
        }
        return RecognizedDocument(pages)
    }
}
