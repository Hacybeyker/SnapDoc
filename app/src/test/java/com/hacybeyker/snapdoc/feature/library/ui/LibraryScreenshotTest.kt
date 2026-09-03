package com.hacybeyker.snapdoc.feature.library.ui

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.test.ScreenshotTest
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import org.junit.Test

class LibraryScreenshotTest : ScreenshotTest() {

    private val documents = listOf(
        StoredDocument(
            id = 1,
            imagePaths = listOf("/scans/a_p1.jpg"),
            createdAtEpochMillis = CREATED_AT,
            text = "HARDWARE STORE\nTOTAL 16.30",
            insight = DocumentInsight(
                kind = DocumentKind.Receipt,
                merchant = "Hardware Store",
                date = "2026-08-20",
                total = "16.30",
                source = InsightSource.OnDeviceModel
            )
        ),
        StoredDocument(
            id = 2,
            imagePaths = listOf("/scans/b_p1.jpg", "/scans/b_p2.jpg"),
            createdAtEpochMillis = CREATED_AT,
            text = "",
            insight = DocumentInsight.empty(InsightSource.Rules)
        )
    )

    private fun library(uiState: LibraryUiState) = capture {
        LibraryContent(
            uiState = uiState,
            onQueryChange = {},
            onDeleteClick = {},
            onShareClick = {},
            onOpenDocument = {},
            onBack = {}
        )
    }

    @Test
    fun library_withDocuments() = library(LibraryUiState(documents = documents, isLoading = false))

    @Test
    fun library_emptyArchive() = library(LibraryUiState(isLoading = false))

    /** The other empty state: it must not read as "you have no documents". */
    @Test
    fun library_noMatches() = library(LibraryUiState(query = "plumber", isLoading = false))

    private companion object {
        const val CREATED_AT = 1_787_250_612_345
    }
}
