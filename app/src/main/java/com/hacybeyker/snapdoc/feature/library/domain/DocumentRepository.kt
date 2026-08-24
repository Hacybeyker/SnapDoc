package com.hacybeyker.snapdoc.feature.library.domain

import kotlinx.coroutines.flow.Flow

interface DocumentRepository {

    fun observeAll(): Flow<List<StoredDocument>>

    /** [query] is already sanitized into full-text syntax; the repository passes it straight through. */
    fun search(query: String): Flow<List<StoredDocument>>

    suspend fun save(document: StoredDocument)

    suspend fun delete(id: Long)
}
