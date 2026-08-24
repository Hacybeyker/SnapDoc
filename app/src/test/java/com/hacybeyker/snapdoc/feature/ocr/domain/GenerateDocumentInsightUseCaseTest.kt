package com.hacybeyker.snapdoc.feature.ocr.domain

import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import java.io.IOException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class GenerateDocumentInsightUseCaseTest {

    private fun useCase(analyzer: FakeOnDeviceDocumentAnalyzer) =
        GenerateDocumentInsightUseCase(analyzer, ExtractDocumentFieldsUseCase())

    @Test
    fun `an available model reads the document`() = runTest {
        val analyzer = FakeOnDeviceDocumentAnalyzer(availability = ModelAvailability.Available)

        val insight = useCase(analyzer)(RECEIPT)

        assertEquals(InsightSource.OnDeviceModel, insight.source)
        assertEquals("Hardware Store", insight.merchant)
    }

    @Test
    fun `a device without the model still gets an answer from the rules`() = runTest {
        val analyzer = FakeOnDeviceDocumentAnalyzer(availability = ModelAvailability.Unavailable)

        val insight = useCase(analyzer)(RECEIPT)

        assertEquals(InsightSource.Rules, insight.source)
        assertEquals(DocumentKind.Receipt, insight.kind)
        assertEquals("16.30", insight.total)
    }

    @Test
    fun `a model that is only downloadable does not block the answer`() = runTest {
        val analyzer = FakeOnDeviceDocumentAnalyzer(availability = ModelAvailability.Downloadable)

        val insight = useCase(analyzer)(RECEIPT)

        assertEquals(InsightSource.Rules, insight.source)
        assertEquals(emptyList<String>(), analyzer.analyzedTexts)
    }

    @Test
    fun `a model that is present but fails mid-inference falls back to the rules`() = runTest {
        val analyzer = FakeOnDeviceDocumentAnalyzer(
            availability = ModelAvailability.Available,
            analysisFailure = IOException("model evicted")
        )

        val insight = useCase(analyzer)(RECEIPT)

        assertEquals(InsightSource.Rules, insight.source)
        assertEquals("16.30", insight.total)
    }

    @Test
    fun `a status check that throws is treated as no model at all`() = runTest {
        val analyzer = FakeOnDeviceDocumentAnalyzer(availabilityFailure = IllegalStateException("AICore missing"))

        val insight = useCase(analyzer)(RECEIPT)

        assertEquals(InsightSource.Rules, insight.source)
    }

    @Test
    fun `a blank document is not sent to any engine`() = runTest {
        val analyzer = FakeOnDeviceDocumentAnalyzer()

        val insight = useCase(analyzer)("   ")

        assertEquals(DocumentKind.Unknown, insight.kind)
        assertEquals(emptyList<String>(), analyzer.analyzedTexts)
    }

    private companion object {
        val RECEIPT = """
            HARDWARE STORE
            Hammer 12.90
            Nails 3.40
            TOTAL 16.30
            2026-08-20
        """.trimIndent()
    }
}
