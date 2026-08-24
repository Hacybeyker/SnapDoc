package com.hacybeyker.snapdoc.feature.library.domain

import javax.inject.Inject

/**
 * Removes the archive entry. The page files on disk are deliberately left alone for now: they are
 * still referenced by whatever screen is showing them, and deleting them here would be the kind of
 * cleanup that fails silently. Reclaiming that storage is its own job, not a side effect of this one.
 */
class DeleteDocumentUseCase @Inject constructor(private val documentRepository: DocumentRepository) {

    suspend operator fun invoke(id: Long) = documentRepository.delete(id)
}
