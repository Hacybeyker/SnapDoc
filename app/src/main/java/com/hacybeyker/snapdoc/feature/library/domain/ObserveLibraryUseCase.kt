package com.hacybeyker.snapdoc.feature.library.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

/**
 * The one entry point the library screen needs: an empty or unsearchable query means the whole
 * archive, anything else means the full-text search. Keeping that decision here rather than in the
 * ViewModel is what stops "the user cleared the box" from being mistaken for "nothing matched".
 */
class ObserveLibraryUseCase @Inject constructor(
    private val documentRepository: DocumentRepository,
    private val buildFtsQueryUseCase: BuildFtsQueryUseCase
) {

    operator fun invoke(rawQuery: String): Flow<List<StoredDocument>> {
        val ftsQuery = buildFtsQueryUseCase(rawQuery)
        return if (ftsQuery == null) documentRepository.observeAll() else documentRepository.search(ftsQuery)
    }
}
