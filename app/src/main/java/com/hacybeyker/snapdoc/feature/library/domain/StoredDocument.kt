package com.hacybeyker.snapdoc.feature.library.domain

import com.hacybeyker.snapdoc.core.document.DocumentInsight

/** A scan the user kept: the pages on disk, the text read off them, and what the app made of it. */
data class StoredDocument(
    val id: Long,
    val imagePaths: List<String>,
    val createdAtEpochMillis: Long,
    val text: String,
    val insight: DocumentInsight
) {

    val pageCount: Int get() = imagePaths.size

    val coverPath: String? get() = imagePaths.firstOrNull()

    /**
     * A scan is archived the moment the camera produces it, so an entry can exist before anyone has
     * read it. Until then it is listed but cannot be found by searching its contents, and the screen
     * says so rather than showing an empty card that looks like a failed scan.
     */
    val hasBeenRead: Boolean get() = text.isNotBlank()
}
