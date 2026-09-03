package com.hacybeyker.snapdoc.feature.home.ui

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.test.ScreenshotTest
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import org.junit.Test

class HomeScreenshotTest : ScreenshotTest() {

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

    private fun home(uiState: HomeUiState, dark: Boolean = false) = capture(darkTheme = dark) {
        HomeContent(uiState = uiState, onScanClick = {}, onLibraryClick = {}, onOpenDocument = {})
    }

    @Test
    fun home_withRecentDocuments() = home(HomeUiState(recentDocuments = documents, isLoading = false))

    @Test
    fun home_withRecentDocuments_dark() = home(HomeUiState(recentDocuments = documents, isLoading = false), dark = true)

    /** The first screen anyone sees, and the one where an empty state has to invite rather than apologise. */
    @Test
    fun home_empty() = home(HomeUiState(isLoading = false))

    private companion object {
        const val CREATED_AT = 1_787_250_612_345
    }
}
