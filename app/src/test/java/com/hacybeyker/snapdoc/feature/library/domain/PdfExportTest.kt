package com.hacybeyker.snapdoc.feature.library.domain

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import java.io.FileNotFoundException
import java.time.ZoneId
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class PdfExportTest {

    private class FakePdfExporter(private val failure: Throwable? = null) : PdfExporter {
        var exportedName: String? = null
            private set
        var exportedPages: List<String> = emptyList()
            private set

        override suspend fun export(fileName: String, imagePaths: List<String>): String {
            failure?.let { throw it }
            exportedName = fileName
            exportedPages = imagePaths
            return "/cache/exports/$fileName"
        }
    }

    private val buildFileName = BuildPdfFileNameUseCase(ZoneId.of("America/Lima"))

    private fun document(
        kind: DocumentKind = DocumentKind.Receipt,
        merchant: String? = "Hardware Store",
        paths: List<String> = listOf("/scans/a_p1.jpg", "/scans/a_p2.jpg")
    ) = StoredDocument(
        id = 1,
        imagePaths = paths,
        createdAtEpochMillis = CREATED_AT,
        text = "HARDWARE STORE",
        insight = DocumentInsight(kind, merchant, "2026-08-20", "16.30", InsightSource.OnDeviceModel)
    )

    @Test
    fun `the file is named after what the document is and when it was taken`() {
        assertEquals(
            "receipt_hardware-store_20260820_1330.pdf",
            buildFileName(DocumentKind.Receipt, "Hardware Store", CREATED_AT)
        )
    }

    @Test
    fun `a merchant name that would break a file system is sanitized`() {
        assertEquals(
            "invoice_acme-co-ltd_20260820_1330.pdf",
            buildFileName(DocumentKind.Invoice, "ACME/Co: Ltd", CREATED_AT)
        )
    }

    @Test
    fun `a long merchant name is trimmed so the timestamp stays visible`() {
        assertEquals(
            "receipt_one-two-three_20260820_1330.pdf",
            buildFileName(DocumentKind.Receipt, "One Two Three Four Five", CREATED_AT)
        )
    }

    @Test
    fun `without a merchant the name is still meaningful`() {
        assertEquals("note_20260820_1330.pdf", buildFileName(DocumentKind.Note, null, CREATED_AT))
    }

    @Test
    fun `every page of the document reaches the exporter, in order`() = runTest {
        val exporter = FakePdfExporter()

        ExportDocumentToPdfUseCase(exporter, buildFileName)(document())

        assertEquals(listOf("/scans/a_p1.jpg", "/scans/a_p2.jpg"), exporter.exportedPages)
        assertEquals("receipt_hardware-store_20260820_1330.pdf", exporter.exportedName)
    }

    @Test
    fun `the caller gets back where the PDF landed`() = runTest {
        val path = ExportDocumentToPdfUseCase(FakePdfExporter(), buildFileName)(document())

        assertEquals("/cache/exports/receipt_hardware-store_20260820_1330.pdf", path)
    }

    @Test
    fun `a page that is no longer on disk fails the export instead of writing half a PDF`() {
        val exporter = FakePdfExporter(FileNotFoundException("gone"))

        assertThrows(FileNotFoundException::class.java) {
            runBlocking { ExportDocumentToPdfUseCase(exporter, buildFileName)(document()) }
        }
    }

    @Test
    fun `a document with no pages is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                ExportDocumentToPdfUseCase(FakePdfExporter(), buildFileName)(document(paths = emptyList()))
            }
        }
    }

    private companion object {
        const val CREATED_AT = 1_787_250_612_345
    }
}
