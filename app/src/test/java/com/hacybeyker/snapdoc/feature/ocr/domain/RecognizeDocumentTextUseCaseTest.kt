package com.hacybeyker.snapdoc.feature.ocr.domain

import java.io.IOException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class RecognizeDocumentTextUseCaseTest {

    @Test
    fun `numbers the pages in the order they were scanned`() = runTest {
        val recognizer = FakeDocumentTextRecognizer(
            blocksByPath = mapOf("/scans/p1.jpg" to listOf("first"), "/scans/p2.jpg" to listOf("second"))
        )

        val document = RecognizeDocumentTextUseCase(recognizer)(listOf("/scans/p1.jpg", "/scans/p2.jpg"))

        assertEquals(listOf(1, 2), document.pages.map { it.pageNumber })
        assertEquals(listOf("/scans/p1.jpg", "/scans/p2.jpg"), recognizer.readPaths)
    }

    @Test
    fun `joins the pages into one text, blocks first and pages after`() = runTest {
        val recognizer = FakeDocumentTextRecognizer(
            blocksByPath = mapOf(
                "/scans/p1.jpg" to listOf("HARDWARE STORE", "TOTAL 16.30"),
                "/scans/p2.jpg" to listOf("Thank you")
            )
        )

        val document = RecognizeDocumentTextUseCase(recognizer)(listOf("/scans/p1.jpg", "/scans/p2.jpg"))

        assertEquals("HARDWARE STORE\nTOTAL 16.30\n\nThank you", document.fullText)
        assertFalse(document.isEmpty)
    }

    @Test
    fun `a page with no text is skipped when joining but still counted as a page`() = runTest {
        val recognizer = FakeDocumentTextRecognizer(blocksByPath = mapOf("/scans/p2.jpg" to listOf("only me")))

        val document = RecognizeDocumentTextUseCase(recognizer)(listOf("/scans/p1.jpg", "/scans/p2.jpg"))

        assertEquals(2, document.pages.size)
        assertEquals("only me", document.fullText)
    }

    @Test
    fun `a document nobody can read text from is empty, not failed`() = runTest {
        val document = RecognizeDocumentTextUseCase(FakeDocumentTextRecognizer())(listOf("/scans/blank.jpg"))

        assertTrue(document.isEmpty)
        assertEquals("", document.fullText)
    }

    @Test
    fun `a scan without pages is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { RecognizeDocumentTextUseCase(FakeDocumentTextRecognizer())(emptyList()) }
        }
    }

    @Test
    fun `a recognizer that fails takes the whole document down`() {
        val broken = FakeDocumentTextRecognizer(failure = IOException("model unavailable"))

        assertThrows(IOException::class.java) {
            runBlocking { RecognizeDocumentTextUseCase(broken)(listOf("/scans/p1.jpg")) }
        }
    }
}
