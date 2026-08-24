package com.hacybeyker.snapdoc.feature.ocr.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.library.domain.FakeDocumentRepository
import com.hacybeyker.snapdoc.feature.library.domain.SaveScannedDocumentUseCase
import com.hacybeyker.snapdoc.feature.ocr.domain.ExtractDocumentFieldsUseCase
import com.hacybeyker.snapdoc.feature.ocr.domain.FakeDocumentTextRecognizer
import com.hacybeyker.snapdoc.feature.ocr.domain.FakeOnDeviceDocumentAnalyzer
import com.hacybeyker.snapdoc.feature.ocr.domain.GenerateDocumentInsightUseCase
import com.hacybeyker.snapdoc.feature.ocr.domain.ModelAvailability
import com.hacybeyker.snapdoc.feature.ocr.domain.ModelDownload
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizeDocumentTextUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DocumentTextViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val recognizer = FakeDocumentTextRecognizer(
        blocksByPath = mapOf(RECEIPT to listOf("HARDWARE STORE", "TOTAL 16.30"))
    )

    private fun viewModel(
        recognizer: FakeDocumentTextRecognizer = this.recognizer,
        analyzer: FakeOnDeviceDocumentAnalyzer = FakeOnDeviceDocumentAnalyzer(ModelAvailability.Unavailable),
        archive: FakeDocumentRepository = FakeDocumentRepository()
    ) = DocumentTextViewModel(
        recognizeDocumentTextUseCase = RecognizeDocumentTextUseCase(recognizer),
        generateDocumentInsightUseCase = GenerateDocumentInsightUseCase(analyzer, ExtractDocumentFieldsUseCase()),
        onDeviceDocumentAnalyzer = analyzer,
        saveScannedDocumentUseCase = SaveScannedDocumentUseCase(archive)
    )

    @Test
    fun `starts while the model is reading the pages`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        assertEquals(DocumentTextUiState.Recognizing, sut.uiState.value)
    }

    @Test
    fun `recognized pages become the content`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        val state = sut.uiState.value as DocumentTextUiState.Content
        assertEquals("HARDWARE STORE\nTOTAL 16.30", state.document.fullText)
    }

    @Test
    fun `a document with no readable text is Empty and not an Error`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel(FakeDocumentTextRecognizer())

        sut.onIntent(DocumentTextIntent.RecognizeText(listOf("/scans/blank.jpg")))
        advanceUntilIdle()

        assertEquals(DocumentTextUiState.Empty, sut.uiState.value)
    }

    @Test
    fun `a recognizer that fails is reported as an error`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel(FakeDocumentTextRecognizer(failure = IOException("model unavailable")))

        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        assertEquals(DocumentTextUiState.Error, sut.uiState.value)
    }

    @Test
    fun `navigating in with no pages is an error nothing can retry away`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()

            sut.onIntent(DocumentTextIntent.RecognizeText(emptyList()))
            advanceUntilIdle()

            assertEquals(DocumentTextUiState.Error, sut.uiState.value)
        }

    @Test
    fun `asking for the same pages again does not run the model twice`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        assertEquals(listOf(RECEIPT), recognizer.readPaths)
    }

    @Test
    fun `retrying reads the same pages again`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        sut.onIntent(DocumentTextIntent.Retry)
        advanceUntilIdle()

        assertEquals(listOf(RECEIPT, RECEIPT), recognizer.readPaths)
    }

    @Test
    fun `copying hands the whole text to the screen`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        sut.onIntent(DocumentTextIntent.CopyText)

        sut.effects.test {
            assertEquals(DocumentTextEffect.CopyToClipboard("HARDWARE STORE\nTOTAL 16.30"), awaitItem())
        }
    }

    @Test
    fun `there is nothing to copy before the text is recognized`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        sut.onIntent(DocumentTextIntent.CopyText)

        sut.effects.test { expectNoEvents() }
    }

    @Test
    fun `a device without the model still shows what the rules could read`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()

            sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
            advanceUntilIdle()

            val state = sut.uiState.value as DocumentTextUiState.Content
            assertEquals(InsightSource.Rules, state.insight?.source)
            assertEquals("16.30", state.insight?.total)
            assertEquals(DocumentTextUiState.ModelStatus.Unavailable, state.modelStatus)
        }

    @Test
    fun `an available model reads the document and the screen says so`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel(analyzer = FakeOnDeviceDocumentAnalyzer(ModelAvailability.Available))

        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        val state = sut.uiState.value as DocumentTextUiState.Content
        assertEquals(InsightSource.OnDeviceModel, state.insight?.source)
        assertEquals("Hardware Store", state.insight?.merchant)
        assertEquals(DocumentTextUiState.ModelStatus.Ready, state.modelStatus)
    }

    @Test
    fun `a downloadable model is offered without holding back the rules answer`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel(analyzer = FakeOnDeviceDocumentAnalyzer(ModelAvailability.Downloadable))

            sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
            advanceUntilIdle()

            val state = sut.uiState.value as DocumentTextUiState.Content
            assertEquals(DocumentTextUiState.ModelStatus.Downloadable, state.modelStatus)
            assertEquals(InsightSource.Rules, state.insight?.source)
        }

    @Test
    fun `downloading the model re-reads the document, which is when the answer improves`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val analyzer = FakeOnDeviceDocumentAnalyzer(ModelAvailability.Downloadable)
            val sut = viewModel(analyzer = analyzer)
            sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
            advanceUntilIdle()

            sut.onIntent(DocumentTextIntent.EnableOnDeviceModel)
            advanceUntilIdle()

            val state = sut.uiState.value as DocumentTextUiState.Content
            assertEquals(DocumentTextUiState.ModelStatus.Ready, state.modelStatus)
            assertEquals(InsightSource.OnDeviceModel, state.insight?.source)
        }

    @Test
    fun `a download that fails leaves the rules answer standing`() = runTest(mainDispatcherRule.testDispatcher) {
        val analyzer = FakeOnDeviceDocumentAnalyzer(
            availability = ModelAvailability.Downloadable,
            downloadSteps = listOf(ModelDownload.Failed(IOException("no space")))
        )
        val sut = viewModel(analyzer = analyzer)
        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        sut.onIntent(DocumentTextIntent.EnableOnDeviceModel)
        advanceUntilIdle()

        val state = sut.uiState.value as DocumentTextUiState.Content
        assertEquals(DocumentTextUiState.ModelStatus.DownloadFailed, state.modelStatus)
        assertEquals(InsightSource.Rules, state.insight?.source)
    }

    @Test
    fun `there is nothing to download when the device cannot run the model`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val analyzer = FakeOnDeviceDocumentAnalyzer(ModelAvailability.Unavailable)
            val sut = viewModel(analyzer = analyzer)
            sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
            advanceUntilIdle()

            sut.onIntent(DocumentTextIntent.EnableOnDeviceModel)
            advanceUntilIdle()

            val state = sut.uiState.value as DocumentTextUiState.Content
            assertEquals(DocumentTextUiState.ModelStatus.Unavailable, state.modelStatus)
        }

    @Test
    fun `a read document is archived so it can be found again later`() = runTest(mainDispatcherRule.testDispatcher) {
        val archive = FakeDocumentRepository()
        val sut = viewModel(archive = archive)

        sut.onIntent(DocumentTextIntent.RecognizeText(listOf(RECEIPT)))
        advanceUntilIdle()

        val stored = archive.observeAll().first().single()
        assertEquals(listOf(RECEIPT), stored.imagePaths)
        assertEquals("HARDWARE STORE\nTOTAL 16.30", stored.text)
    }

    @Test
    fun `a document with no readable text leaves nothing in the archive`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val archive = FakeDocumentRepository()
            val sut = viewModel(recognizer = FakeDocumentTextRecognizer(), archive = archive)

            sut.onIntent(DocumentTextIntent.RecognizeText(listOf("/scans/blank.jpg")))
            advanceUntilIdle()

            assertEquals(emptyList<Any>(), archive.observeAll().first())
        }

    private companion object {
        const val RECEIPT = "/scans/receipt_p1.jpg"
    }
}
