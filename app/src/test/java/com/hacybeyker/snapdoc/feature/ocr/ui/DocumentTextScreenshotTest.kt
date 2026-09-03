package com.hacybeyker.snapdoc.feature.ocr.ui

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.test.ScreenshotTest
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizedDocument
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizedPage
import org.junit.Test

class DocumentTextScreenshotTest : ScreenshotTest() {

    private val document = RecognizedDocument(
        pages = listOf(
            RecognizedPage(
                pageNumber = 1,
                blocks = listOf("HARDWARE STORE", "Av. Arequipa 1234", "2 x Paint roller  9.80", "TOTAL 16.30")
            )
        )
    )

    private fun reader(uiState: DocumentTextUiState) = capture {
        DocumentTextContent(
            uiState = uiState,
            onCopyClick = {},
            onRetryClick = {},
            onBackClick = {},
            onEnableModelClick = {}
        )
    }

    /** What the model produced, next to the text it produced it from. */
    @Test
    fun reader_readByTheModel() = reader(
        DocumentTextUiState.Content(
            document = document,
            insight = DocumentInsight(
                kind = DocumentKind.Receipt,
                merchant = "Hardware Store",
                date = "2026-08-20",
                total = "16.30",
                source = InsightSource.OnDeviceModel
            ),
            modelStatus = DocumentTextUiState.ModelStatus.Ready
        )
    )

    /** The degraded answer, which has to look like an answer and not like a failure. */
    @Test
    fun reader_readByTheRules_modelOffered() = reader(
        DocumentTextUiState.Content(
            document = document,
            insight = DocumentInsight(
                kind = DocumentKind.Receipt,
                merchant = null,
                date = "2026-08-20",
                total = "16.30",
                source = InsightSource.Rules
            ),
            modelStatus = DocumentTextUiState.ModelStatus.Downloadable
        )
    )

    @Test
    fun reader_nothingReadable() = reader(DocumentTextUiState.Empty)

    @Test
    fun reader_failed() = reader(DocumentTextUiState.Error)
}
