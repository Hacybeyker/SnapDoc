package com.hacybeyker.snapdoc.feature.library.domain

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryUseCasesTest {

    private fun document(id: Long, paths: List<String>, text: String) = StoredDocument(
        id = id,
        imagePaths = paths,
        createdAtEpochMillis = 1_787_250_612_345,
        text = text,
        insight = DocumentInsight(DocumentKind.Receipt, "Hardware Store", "2026-08-20", "16.30", InsightSource.Rules)
    )

    private fun observe(repository: FakeDocumentRepository) = ObserveLibraryUseCase(repository, BuildFtsQueryUseCase())

    @Test
    fun `an empty query lists the whole archive without touching search`() = runTest {
        val repository = FakeDocumentRepository(
            listOf(document(1, listOf("/a.jpg"), "hardware"), document(2, listOf("/b.jpg"), "plumber"))
        )

        val documents = observe(repository)("").first()

        assertEquals(2, documents.size)
        assertEquals(emptyList<String>(), repository.searchedQueries)
    }

    @Test
    fun `a query searches the recognized text`() = runTest {
        val repository = FakeDocumentRepository(
            listOf(document(1, listOf("/a.jpg"), "HARDWARE STORE"), document(2, listOf("/b.jpg"), "call the plumber"))
        )

        val documents = observe(repository)("plumb").first()

        assertEquals(listOf(2L), documents.map { it.id })
        assertEquals(listOf("plumb*"), repository.searchedQueries)
    }

    @Test
    fun `a query that sanitizes down to nothing lists everything instead of failing`() = runTest {
        val repository = FakeDocumentRepository(listOf(document(1, listOf("/a.jpg"), "hardware")))

        val documents = observe(repository)("\"\" ---").first()

        assertEquals(1, documents.size)
        assertEquals(emptyList<String>(), repository.searchedQueries)
    }

    @Test
    fun `the archive emits again when a document is saved`() = runTest {
        val repository = FakeDocumentRepository()
        val useCase = SaveScannedDocumentUseCase(repository)

        repository.observeAll().test {
            assertEquals(emptyList<StoredDocument>(), awaitItem())

            useCase(listOf("/a.jpg"), "HARDWARE STORE", DocumentInsight.empty(InsightSource.Rules), 0)

            assertEquals(1, awaitItem().size)
        }
    }

    @Test
    fun `saving the same pages twice replaces the entry instead of duplicating the scan`() = runTest {
        val repository = FakeDocumentRepository()
        val useCase = SaveScannedDocumentUseCase(repository)
        val insightFromRules = DocumentInsight.empty(InsightSource.Rules)
        val insightFromModel =
            DocumentInsight(DocumentKind.Receipt, "Hardware Store", null, "16.30", InsightSource.OnDeviceModel)

        useCase(listOf("/a.jpg"), "HARDWARE STORE", insightFromRules, 0)
        useCase(listOf("/a.jpg"), "HARDWARE STORE", insightFromModel, 0)

        val documents = repository.observeAll().first()
        assertEquals(1, documents.size)
        assertEquals(InsightSource.OnDeviceModel, documents.single().insight.source)
    }

    @Test
    fun `a scan with no text yet is still archived, because the user did take it`() = runTest {
        val repository = FakeDocumentRepository()

        SaveScannedDocumentUseCase(repository)(listOf("/a.jpg"), "", DocumentInsight.empty(InsightSource.Rules), 0)

        val stored = repository.observeAll().first().single()
        assertEquals(false, stored.hasBeenRead)
    }

    @Test
    fun `reading an archived scan keeps its id and the time it was taken`() = runTest {
        val repository = FakeDocumentRepository()
        val useCase = SaveScannedDocumentUseCase(repository)
        useCase(listOf("/a.jpg"), "", DocumentInsight.empty(InsightSource.Rules), CAPTURED_AT)

        val idAfterCapture = repository.observeAll().first().single().id
        useCase(listOf("/a.jpg"), "HARDWARE STORE", DocumentInsight.empty(InsightSource.Rules), CAPTURED_AT + 90_000)

        val stored = repository.observeAll().first().single()
        assertEquals(idAfterCapture, stored.id)
        assertEquals(CAPTURED_AT, stored.createdAtEpochMillis)
        assertEquals(true, stored.hasBeenRead)
    }

    @Test
    fun `a scan without pages is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                SaveScannedDocumentUseCase(FakeDocumentRepository())(
                    imagePaths = emptyList(),
                    text = "something",
                    insight = DocumentInsight.empty(InsightSource.Rules),
                    createdAtEpochMillis = 0
                )
            }
        }
    }

    @Test
    fun `deleting removes it from the archive`() = runTest {
        val repository = FakeDocumentRepository(listOf(document(1, listOf("/a.jpg"), "hardware")))

        DeleteDocumentUseCase(repository)(1)

        assertTrue(repository.observeAll().first().isEmpty())
    }

    private companion object {
        const val CAPTURED_AT = 1_787_250_612_345
    }
}
