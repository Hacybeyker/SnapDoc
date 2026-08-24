package com.hacybeyker.snapdoc.feature.ocr.domain

/**
 * One page's text, kept as the blocks the recognizer found instead of a single flat string. A block
 * is a visually grouped run of text — a paragraph, a column, a receipt's total line — and that
 * grouping is the only structural hint the image gives us. Joining it away here would force a later
 * phase to guess the sections back from line breaks.
 */
data class RecognizedPage(val pageNumber: Int, val blocks: List<String>) {

    val text: String = blocks.joinToString(separator = "\n")

    val isEmpty: Boolean get() = blocks.isEmpty()
}
