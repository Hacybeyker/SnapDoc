package com.hacybeyker.snapdoc.feature.ocr.data

import com.hacybeyker.snapdoc.feature.ocr.domain.DocumentTextRecognizer
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
interface OcrModule {

    @Binds
    fun bindDocumentTextRecognizer(recognizer: MlKitDocumentTextRecognizer): DocumentTextRecognizer
}
