package com.hacybeyker.snapdoc.feature.library.data

import com.hacybeyker.snapdoc.feature.library.domain.DocumentRepository
import com.hacybeyker.snapdoc.feature.library.domain.PdfExporter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface LibraryModule {

    @Binds
    fun bindDocumentRepository(repository: RoomDocumentRepository): DocumentRepository

    @Binds
    fun bindPdfExporter(exporter: PdfDocumentExporter): PdfExporter
}
