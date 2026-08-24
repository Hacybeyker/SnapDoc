package com.hacybeyker.snapdoc.feature.ocr.data

import com.hacybeyker.snapdoc.feature.ocr.domain.DocumentKind
import com.hacybeyker.snapdoc.feature.ocr.domain.InsightSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The model is small and its output is untidy; these are the shapes it actually produces. */
class DocumentInsightMapperTest {

    @Test
    fun `reads the four lines it was asked for`() {
        val insight = """
            KIND: RECEIPT
            MERCHANT: Hardware Store
            DATE: 2026-08-20
            TOTAL: 16.30
        """.trimIndent().toDocumentInsight()

        assertEquals(DocumentKind.Receipt, insight.kind)
        assertEquals("Hardware Store", insight.merchant)
        assertEquals("2026-08-20", insight.date)
        assertEquals("16.30", insight.total)
        assertEquals(InsightSource.OnDeviceModel, insight.source)
    }

    @Test
    fun `NONE and its cousins all mean the field is absent`() {
        val insight = """
            KIND: NOTE
            MERCHANT: NONE
            DATE: N/A
            TOTAL: -
        """.trimIndent().toDocumentInsight()

        assertNull(insight.merchant)
        assertNull(insight.date)
        assertNull(insight.total)
    }

    @Test
    fun `markdown the model was told not to use is stripped anyway`() {
        val insight = """
            **KIND:** INVOICE
            **MERCHANT:** *Acme Ltd*
            TOTAL: "99.00"
        """.trimIndent().toDocumentInsight()

        assertEquals(DocumentKind.Invoice, insight.kind)
        assertEquals("Acme Ltd", insight.merchant)
        assertEquals("99.00", insight.total)
    }

    @Test
    fun `prose around the answer does not stop the fields being read`() {
        val insight = """
            Sure! Here is the information you asked for:
            KIND: RECEIPT
            MERCHANT: Corner Shop
            Let me know if you need anything else.
        """.trimIndent().toDocumentInsight()

        assertEquals(DocumentKind.Receipt, insight.kind)
        assertEquals("Corner Shop", insight.merchant)
    }

    @Test
    fun `one unreadable line costs that field and not the whole insight`() {
        val insight = """
            KIND: RECEIPT
            MERCHANT: Corner Shop
            TOTAL
        """.trimIndent().toDocumentInsight()

        assertEquals("Corner Shop", insight.merchant)
        assertNull(insight.total)
    }

    @Test
    fun `a kind the model invented is Unknown rather than a wrong guess`() {
        assertEquals(DocumentKind.Unknown, "KIND: SHOPPING LIST".toDocumentInsight().kind)
    }

    @Test
    fun `an empty reply is an empty insight`() {
        val insight = "".toDocumentInsight()

        assertEquals(DocumentKind.Unknown, insight.kind)
        assertEquals(false, insight.hasFields)
    }
}
