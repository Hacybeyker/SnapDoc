package com.hacybeyker.snapdoc.feature.home.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.library.domain.BuildFtsQueryUseCase
import com.hacybeyker.snapdoc.feature.library.domain.FakeDocumentRepository
import com.hacybeyker.snapdoc.feature.library.domain.ObserveLibraryUseCase
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun document(id: Long) = StoredDocument(
        id = id,
        imagePaths = listOf("/scans/scan_$id.jpg"),
        createdAtEpochMillis = CREATED_AT,
        text = "HARDWARE STORE",
        insight = DocumentInsight.empty(InsightSource.Rules)
    )

    private fun viewModel(repository: FakeDocumentRepository) =
        HomeViewModel(ObserveLibraryUseCase(repository, BuildFtsQueryUseCase()))

    @Test
    fun `home shows the newest scans and no more than three`() = runTest(mainDispatcherRule.testDispatcher) {
        // The order is the archive's to decide, not Home's — it takes the front of the list it is given.
        val repository = FakeDocumentRepository((1L..5L).map { document(it) })
        val sut = viewModel(repository)

        sut.uiState.test {
            advanceUntilIdle()

            val state = expectMostRecentItem()
            assertEquals(listOf(1L, 2L, 3L), state.recentDocuments.map { it.id })
            assertTrue(state.hasDocuments)
            assertFalse(state.isLoading)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `an archive with nothing in it stops loading instead of spinning forever`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel(FakeDocumentRepository())

            sut.uiState.test {
                advanceUntilIdle()

                val state = expectMostRecentItem()
                assertFalse(state.hasDocuments)
                assertFalse(state.isLoading)
                cancelAndIgnoreRemainingEvents()
            }
        }

    private companion object {
        const val CREATED_AT = 1_787_250_612_345
    }
}
