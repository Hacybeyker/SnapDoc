package com.hacybeyker.snapdoc.feature.library.data

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A stored scan. [imagePaths] carries the pages joined by [PATH_SEPARATOR] and is uniquely indexed,
 * which is what gives a document a stable identity: a document *is* the pages it was made of. Saving
 * the same pages again therefore replaces the row instead of duplicating it — and that is not just
 * deduplication, it is how an insight improves: read the same scan again once the model is available
 * and the better answer overwrites the one the rules produced.
 */
@Entity(tableName = "documents", indices = [Index(value = ["imagePaths"], unique = true)])
data class DocumentEntity(
    // Room needs an Int primary key here: it doubles as the rowid the FTS table joins against.
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val imagePaths: String,
    val createdAtEpochMillis: Long,
    val pageCount: Int,
    val text: String,
    val kind: String,
    val merchant: String?,
    val date: String?,
    val total: String?,
    val insightSource: String
)

/**
 * External-content FTS index over [DocumentEntity.text]. "External content" means the index does not
 * keep its own copy of the OCR text, which for scanned pages is the bulk of the row; Room generates
 * the triggers that keep the two in sync, so every write still goes to `documents` and never here.
 */
@Fts4(contentEntity = DocumentEntity::class)
@Entity(tableName = "documents_fts")
data class DocumentFtsEntity(val text: String)

internal const val PATH_SEPARATOR = "\n"
