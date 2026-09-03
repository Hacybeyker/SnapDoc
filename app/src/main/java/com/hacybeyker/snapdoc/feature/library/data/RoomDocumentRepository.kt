package com.hacybeyker.snapdoc.feature.library.data

import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.feature.library.domain.DocumentRepository
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

class RoomDocumentRepository @Inject constructor(
    private val documentDao: DocumentDao,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : DocumentRepository {

    override fun observeAll(): Flow<List<StoredDocument>> = documentDao.observeAll()
        .map { entities -> entities.map { it.toStoredDocument() } }
        .flowOn(ioDispatcher)

    override fun search(query: String): Flow<List<StoredDocument>> = documentDao.search(query)
        .map { entities -> entities.map { it.toStoredDocument() } }
        .flowOn(ioDispatcher)

    // No withContext here: Room runs its own suspend queries on its query executor, so wrapping them
    // would only add a dispatcher hop. The Flows above still need flowOn — they are cold, and the
    // collector decides where they run.
    override suspend fun save(document: StoredDocument) = documentDao.save(document.toEntity())

    override suspend fun delete(id: Long) = documentDao.delete(id)
}
