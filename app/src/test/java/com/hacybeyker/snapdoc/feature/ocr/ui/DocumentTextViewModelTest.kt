package com.hacybeyker.snapdoc.feature.ocr.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.ocr.domain.FakeDocumentTextRecognizer
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizeDocumentTextUseCase
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
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

    private fun viewModel(recognizer: FakeDocumentTextRecognizer = this.recognizer) =
        DocumentTextViewModel(RecognizeDocumentTextUseCase(recognizer))

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

    private companion object {
        const val RECEIPT = "/scans/receipt_p1.jpg"
    }
}
