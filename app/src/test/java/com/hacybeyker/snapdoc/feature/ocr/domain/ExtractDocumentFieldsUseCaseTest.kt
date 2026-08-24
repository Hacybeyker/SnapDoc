package com.hacybeyker.snapdoc.feature.ocr.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExtractDocumentFieldsUseCaseTest {

    private val useCase = ExtractDocumentFieldsUseCase()

    @Test
    fun `a labelled total wins over a bigger number elsewhere on the page`() {
        val receipt = """
            SUPERMARKET
            Cash tendered 50.00
            TOTAL 16.30
        """.trimIndent()

        assertEquals("16.30", useCase(receipt).total)
    }

    @Test
    fun `the last amount on the total line is the total, not a code before it`() {
        assertEquals("16.30", useCase("TOTAL 4 items 16.30").total)
    }

    @Test
    fun `an amount keeps its currency symbol`() {
        assertEquals("€ 16.30", useCase("TOTAL € 16.30").total)
    }

    @Test
    fun `a document with no labelled total reports none`() {
        assertNull(useCase("Just some handwritten notes 12.90").total)
    }

    @Test
    fun `dates are read in the common formats`() {
        assertEquals("2026-08-20", useCase("Issued 2026-08-20").date)
        assertEquals("20/08/2026", useCase("Fecha 20/08/2026").date)
        assertEquals("20-08-26", useCase("Date 20-08-26").date)
    }

    @Test
    fun `an invoice is told apart from a receipt`() {
        assertEquals(DocumentKind.Invoice, useCase("TAX INVOICE\nTOTAL 10.00").kind)
        assertEquals(DocumentKind.Receipt, useCase("RECEIPT\nSUBTOTAL 10.00").kind)
        assertEquals(DocumentKind.IdDocument, useCase("PASSPORT\nName: Someone").kind)
    }

    @Test
    fun `text with no telling keyword is just a note`() {
        assertEquals(DocumentKind.Note, useCase("Remember to call the plumber").kind)
    }

    @Test
    fun `the merchant is deliberately left empty rather than guessed`() {
        val receipt = """
            HARDWARE STORE
            TOTAL 16.30
        """.trimIndent()

        assertNull(useCase(receipt).merchant)
    }

    @Test
    fun `every answer says it came from the rules`() {
        assertEquals(InsightSource.Rules, useCase("TOTAL 16.30").source)
    }

    @Test
    fun `a blank document yields an empty insight`() {
        val insight = useCase("   ")

        assertEquals(DocumentKind.Unknown, insight.kind)
        assertEquals(false, insight.hasFields)
    }
}
