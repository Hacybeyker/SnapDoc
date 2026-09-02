package com.hacybeyker.snapdoc.feature.library.data

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import org.junit.Assert.assertEquals
import org.junit.Test

class DocumentMapperTest {

    private val insight = DocumentInsight(
        kind = DocumentKind.Receipt,
        merchant = "Hardware Store",
        date = "2026-08-20",
        total = "16.30",
        source = InsightSource.OnDeviceModel
    )

    private fun document(paths: List<String> = listOf("/scans/a_p1.jpg")) = StoredDocument(
        id = 7,
        imagePaths = paths,
        createdAtEpochMillis = CREATED_AT,
        text = "HARDWARE STORE\nTOTAL 16.30",
        insight = insight
    )

    private fun row(kind: String, insightSource: String) = DocumentEntity(
        id = 1,
        imagePaths = "/scans/a_p1.jpg",
        createdAtEpochMillis = CREATED_AT,
        pageCount = 1,
        text = "HARDWARE STORE",
        kind = kind,
        merchant = null,
        date = null,
        total = null,
        insightSource = insightSource
    )

    @Test
    fun `a document survives the round trip through the row`() {
        val original = document()

        val restored = original.toEntity().toStoredDocument()

        // Everything but the id: the row is written with id 0 because a saved scan is identified by
        // its pages and Room assigns the number.
        assertEquals(original.copy(id = 0), restored)
    }

    @Test
    fun `the pages travel as one column and come back as a list`() {
        val pages = listOf("/scans/a_p1.jpg", "/scans/a_p2.jpg", "/scans/a_p3.jpg")

        val entity = document(pages).toEntity()

        assertEquals("/scans/a_p1.jpg\n/scans/a_p2.jpg\n/scans/a_p3.jpg", entity.imagePaths)
        assertEquals(3, entity.pageCount)
        assertEquals(pages, entity.toStoredDocument().imagePaths)
    }

    @Test
    fun `a kind written by a newer build reads back as Unknown instead of crashing`() {
        val restored = row(kind = "Passport", insightSource = InsightSource.Rules.name).toStoredDocument()

        assertEquals(DocumentKind.Unknown, restored.insight.kind)
    }

    @Test
    fun `an engine this build has never heard of reads back as the rules`() {
        val restored = row(kind = DocumentKind.Note.name, insightSource = "CloudModel").toStoredDocument()

        assertEquals(InsightSource.Rules, restored.insight.source)
        assertEquals(DocumentKind.Note, restored.insight.kind)
    }

    private companion object {
        const val CREATED_AT = 1_787_250_612_345
    }
}
