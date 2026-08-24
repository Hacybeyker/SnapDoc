package com.hacybeyker.snapdoc.feature.library.ui

import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument

data class LibraryUiState(
    val query: String = "",
    val documents: List<StoredDocument> = emptyList(),
    val isLoading: Boolean = true
) {

    /**
     * The two empty states read the same to the code and completely differently to the user: an
     * archive with nothing in it needs an invitation to scan, a search with no hits needs to say so.
     */
    val isEmptyArchive: Boolean get() = !isLoading && documents.isEmpty() && query.isBlank()

    val hasNoMatches: Boolean get() = !isLoading && documents.isEmpty() && query.isNotBlank()
}
