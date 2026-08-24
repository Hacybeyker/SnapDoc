package com.hacybeyker.snapdoc.feature.library.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    /**
     * Two statements rather than one `REPLACE`, because a scan is stored twice in its life: once when
     * the camera produces it, and again when it has been read. REPLACE would delete and re-insert,
     * which resets the row's `createdAtEpochMillis` to the moment it was read — reordering the
     * archive — and hands it a **new id**, invalidating the key the list is drawn with. Inserting only
     * when new and then updating the reading keeps both the original time and the identity intact.
     */
    @Transaction
    suspend fun save(document: DocumentEntity) {
        insertIfNew(document)
        updateReading(
            imagePaths = document.imagePaths,
            text = document.text,
            kind = document.kind,
            merchant = document.merchant,
            date = document.date,
            total = document.total,
            insightSource = document.insightSource
        )
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfNew(document: DocumentEntity)

    @Query(
        """
        UPDATE documents SET
            text = :text,
            kind = :kind,
            merchant = :merchant,
            date = :date,
            total = :total,
            insightSource = :insightSource
        WHERE imagePaths = :imagePaths
        """
    )
    suspend fun updateReading(
        imagePaths: String,
        text: String,
        kind: String,
        merchant: String?,
        date: String?,
        total: String?,
        insightSource: String
    )

    @Query("SELECT * FROM documents ORDER BY createdAtEpochMillis DESC")
    fun observeAll(): Flow<List<DocumentEntity>>

    /**
     * Joins through the FTS table by rowid. Ordering stays on the document's own timestamp rather
     * than on relevance: for a scan archive "the one I took most recently" is a better answer than
     * "the one that repeats the word most", which is what FTS ranking would reward.
     */
    @Query(
        """
        SELECT documents.* FROM documents
        JOIN documents_fts ON documents.id = documents_fts.rowid
        WHERE documents_fts MATCH :query
        ORDER BY documents.createdAtEpochMillis DESC
        """
    )
    fun search(query: String): Flow<List<DocumentEntity>>

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: Long)
}
