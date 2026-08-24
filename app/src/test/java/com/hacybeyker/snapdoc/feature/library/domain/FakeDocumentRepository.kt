package com.hacybeyker.snapdoc.feature.library.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Keeps documents in memory, identified by their pages the way the Room unique index does — so a
 * re-save replaces rather than duplicates here too, and the test exercises the real contract.
 */
class FakeDocumentRepository(initial: List<StoredDocument> = emptyList()) : DocumentRepository {

    private val documents = MutableStateFlow(initial)

    val searchedQueries = mutableListOf<String>()

    override fun observeAll(): Flow<List<StoredDocument>> = documents

    override fun search(query: String): Flow<List<StoredDocument>> {
        searchedQueries += query
        val terms = query.split(" ").map { it.removeSuffix("*") }.filter { it.isNotEmpty() }
        return documents.map { stored ->
            stored.filter { document -> terms.all { it.lowercase() in document.text.lowercase() } }
        }
    }

    /** Mirrors the DAO: a re-save keeps the original id and creation time and only updates the reading. */
    override suspend fun save(document: StoredDocument) {
        val existing = documents.value.firstOrNull { it.imagePaths == document.imagePaths }
        val stored = if (existing == null) {
            document.copy(id = documents.value.size + 1L)
        } else {
            existing.copy(text = document.text, insight = document.insight)
        }
        documents.value = documents.value.filterNot { it.imagePaths == document.imagePaths } + stored
    }

    override suspend fun delete(id: Long) {
        documents.value = documents.value.filterNot { it.id == id }
    }
}
