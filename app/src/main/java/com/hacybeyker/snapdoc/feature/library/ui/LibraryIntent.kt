package com.hacybeyker.snapdoc.feature.library.ui

sealed interface LibraryIntent {
    data class QueryChanged(val query: String) : LibraryIntent
    data class DeleteDocument(val id: Long) : LibraryIntent
    data class ExportDocument(val id: Long) : LibraryIntent
}
