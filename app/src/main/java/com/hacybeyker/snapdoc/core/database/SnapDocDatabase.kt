package com.hacybeyker.snapdoc.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.hacybeyker.snapdoc.feature.library.data.DocumentDao
import com.hacybeyker.snapdoc.feature.library.data.DocumentEntity
import com.hacybeyker.snapdoc.feature.library.data.DocumentFtsEntity

/**
 * The database is inherently cross-feature — it has to name every entity in the app — so it lives in
 * `core/` while each slice contributes its own `@Entity` and DAO. This is the one place the
 * dependency runs from core into a feature, and it is the reason `AGENTS.md` calls it out explicitly.
 *
 * `exportSchema` stays on: the exported JSON is what makes a future migration reviewable in a diff.
 */
@Database(
    entities = [DocumentEntity::class, DocumentFtsEntity::class],
    version = 1,
    exportSchema = true
)
abstract class SnapDocDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
}
