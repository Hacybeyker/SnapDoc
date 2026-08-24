package com.hacybeyker.snapdoc.feature.library.data

import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.feature.library.domain.DocumentRepository
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    override suspend fun save(document: StoredDocument) = withContext(ioDispatcher) {
        documentDao.save(document.toEntity())
    }

    override suspend fun delete(id: Long) = withContext(ioDispatcher) {
        documentDao.delete(id)
    }
}
