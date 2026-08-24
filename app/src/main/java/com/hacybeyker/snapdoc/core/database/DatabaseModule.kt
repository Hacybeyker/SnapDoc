package com.hacybeyker.snapdoc.core.database

import android.content.Context
import androidx.room.Room
import com.hacybeyker.snapdoc.feature.library.data.DocumentDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * No `fallbackToDestructiveMigration`: a scan archive is the user's own data, and silently
     * wiping it on a schema change would be the worst possible way to learn a migration was missing.
     * Version 2 will have to arrive with a real migration.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SnapDocDatabase =
        Room.databaseBuilder(context, SnapDocDatabase::class.java, DATABASE_NAME).build()

    @Provides
    fun provideDocumentDao(database: SnapDocDatabase): DocumentDao = database.documentDao()

    private const val DATABASE_NAME = "snapdoc.db"
}
