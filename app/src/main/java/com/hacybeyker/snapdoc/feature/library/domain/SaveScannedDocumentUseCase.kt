package com.hacybeyker.snapdoc.feature.library.domain

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import javax.inject.Inject

/**
 * Keeps a scan in the archive. This is the contract the OCR slice calls: the two features never see
 * each other's internals, they meet on this use case and on [StoredDocument].
 */
class SaveScannedDocumentUseCase @Inject constructor(private val documentRepository: DocumentRepository) {

    /**
     * Called twice in a scan's life: once by the camera the moment the pages hit disk, and again by
     * the reader once there is text and an insight to attach. Storing it at acquisition is the point
     * — the user scanned it, so it is theirs to keep whether or not they ever open it, and an earlier
     * version of this only archived documents that had been read, which made "Capture" look broken.
     *
     * Text-less entries are therefore allowed: they cannot be *searched* yet, but they are listed,
     * and reading them later fills the rest in without creating a second entry.
     */
    suspend operator fun invoke(
        imagePaths: List<String>,
        text: String,
        insight: DocumentInsight,
        createdAtEpochMillis: Long
    ) {
        require(imagePaths.isNotEmpty()) { "A stored document must have at least one page" }
        documentRepository.save(
            StoredDocument(
                id = 0,
                imagePaths = imagePaths,
                createdAtEpochMillis = createdAtEpochMillis,
                text = text,
                insight = insight
            )
        )
    }
}
