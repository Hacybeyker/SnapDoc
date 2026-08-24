package com.hacybeyker.snapdoc.feature.ocr.domain

/** The text of a whole scan, one entry per page, in the order the pages were scanned. */
data class RecognizedDocument(val pages: List<RecognizedPage>) {

    val fullText: String = pages.filterNot { it.isEmpty }.joinToString(separator = "\n\n") { it.text }

    /**
     * A photo of a wall recognizes cleanly and yields nothing — an empty result is a normal outcome,
     * not a failure, so the UI states have to tell the two apart.
     */
    val isEmpty: Boolean get() = pages.all { it.isEmpty }
}
