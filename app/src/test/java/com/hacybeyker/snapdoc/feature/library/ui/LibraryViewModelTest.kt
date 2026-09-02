package com.hacybeyker.snapdoc.feature.library.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.library.domain.BuildFtsQueryUseCase
import com.hacybeyker.snapdoc.feature.library.domain.BuildPdfFileNameUseCase
import com.hacybeyker.snapdoc.feature.library.domain.DeleteDocumentUseCase
import com.hacybeyker.snapdoc.feature.library.domain.ExportDocumentToPdfUseCase
import com.hacybeyker.snapdoc.feature.library.domain.FakeDocumentRepository
import com.hacybeyker.snapdoc.feature.library.domain.ObserveLibraryUseCase
import com.hacybeyker.snapdoc.feature.library.domain.PdfExporter
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import java.io.IOException
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LibraryViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private class FakePdfExporter(private val failure: Throwable? = null) : PdfExporter {
        var exportedPages: List<String> = emptyList()
            private set

        override suspend fun export(fileName: String, imagePaths: List<String>): String {
            failure?.let { throw it }
            exportedPages = imagePaths
            return "/cache/exports/$fileName"
        }
    }

    private fun document(id: Long, text: String, paths: List<String> = listOf("/scans/a_p1.jpg")) = StoredDocument(
        id = id,
        imagePaths = paths,
        createdAtEpochMillis = CREATED_AT,
        text = text,
        insight = DocumentInsight(DocumentKind.Receipt, "Hardware Store", "2026-08-20", "16.30", InsightSource.Rules)
    )

    private fun viewModel(
        repository: FakeDocumentRepository = FakeDocumentRepository(),
        exporter: PdfExporter = FakePdfExporter()
    ) = LibraryViewModel(
        observeLibraryUseCase = ObserveLibraryUseCase(repository, BuildFtsQueryUseCase()),
        deleteDocumentUseCase = DeleteDocumentUseCase(repository),
        exportDocumentToPdfUseCase = ExportDocumentToPdfUseCase(
            pdfExporter = exporter,
            buildPdfFileNameUseCase = BuildPdfFileNameUseCase(ZoneId.of("America/Lima"))
        )
    )

    @Test
    fun `the box follows every keystroke while the search behind it runs once`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeDocumentRepository(listOf(document(1, "HARDWARE STORE")))
            val sut = viewModel(repository)

            sut.uiState.test {
                sut.onIntent(LibraryIntent.QueryChanged("h"))
                sut.onIntent(LibraryIntent.QueryChanged("ha"))
                sut.onIntent(LibraryIntent.QueryChanged("har"))
                advanceUntilIdle()

                assertEquals("har", expectMostRecentItem().query)
                cancelAndIgnoreRemainingEvents()
            }
            // Three keystrokes, one query: what the debounce is for. The typed text is never debounced,
            // which is why the assertion above and this one can hold at the same time.
            assertEquals(listOf("har*"), repository.searchedQueries)
        }

    @Test
    fun `an empty archive and a search with no hits are different empty states`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()

            sut.uiState.test {
                advanceUntilIdle()
                assertTrue(expectMostRecentItem().isEmptyArchive)

                sut.onIntent(LibraryIntent.QueryChanged("plumber"))
                advanceUntilIdle()

                val searched = expectMostRecentItem()
                assertTrue(searched.hasNoMatches)
                assertTrue(!searched.isEmptyArchive)
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `deleting a document takes it out of the archive`() = runTest(mainDispatcherRule.testDispatcher) {
        val repository = FakeDocumentRepository(listOf(document(1, "hardware"), document(2, "plumber")))
        val sut = viewModel(repository)

        sut.uiState.test {
            advanceUntilIdle()
            sut.onIntent(LibraryIntent.DeleteDocument(1))
            advanceUntilIdle()

            assertEquals(listOf(2L), expectMostRecentItem().documents.map { it.id })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `exporting shares the file the exporter wrote`() = runTest(mainDispatcherRule.testDispatcher) {
        val pages = listOf("/scans/a_p1.jpg", "/scans/a_p2.jpg")
        val exporter = FakePdfExporter()
        val sut = viewModel(FakeDocumentRepository(listOf(document(1, "hardware", pages))), exporter)
        // The export takes the document from the live state rather than from the row that was tapped,
        // so it only means anything while the screen is watching — which it always is, since the list
        // it exports from is the one it renders.
        backgroundScope.launch { sut.uiState.collect { } }
        advanceUntilIdle()

        sut.effects.test {
            sut.onIntent(LibraryIntent.ExportDocument(1))
            advanceUntilIdle()

            val effect = awaitItem()
            assertTrue(effect is LibraryEffect.SharePdf)
            assertTrue((effect as LibraryEffect.SharePdf).filePath.startsWith("/cache/exports/"))
        }
        assertEquals(pages, exporter.exportedPages)
    }

    @Test
    fun `an export that fails is an effect and not an error state`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel(
            repository = FakeDocumentRepository(listOf(document(1, "hardware"))),
            exporter = FakePdfExporter(IOException("no space left on device"))
        )
        backgroundScope.launch { sut.uiState.collect { } }
        advanceUntilIdle()

        sut.effects.test {
            sut.onIntent(LibraryIntent.ExportDocument(1))
            advanceUntilIdle()

            assertEquals(LibraryEffect.ExportFailed, awaitItem())
        }
        // The archive is still perfectly usable: one failed share is not a screen-wide error.
        assertEquals(listOf(1L), sut.uiState.value.documents.map { it.id })
    }

    @Test
    fun `exporting a document that is no longer listed reports the failure`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel(FakeDocumentRepository(listOf(document(1, "hardware"))))
            backgroundScope.launch { sut.uiState.collect { } }
            advanceUntilIdle()

            sut.effects.test {
                sut.onIntent(LibraryIntent.ExportDocument(404))
                advanceUntilIdle()

                assertEquals(LibraryEffect.ExportFailed, awaitItem())
            }
        }

    private companion object {
        const val CREATED_AT = 1_787_250_612_345
    }
}
