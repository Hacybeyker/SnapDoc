package com.hacybeyker.snapdoc.feature.camera.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.camera.domain.BuildScanFileNameUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.EvaluateLiveTextHintUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.FakePhotoStorageRepository
import com.hacybeyker.snapdoc.feature.camera.domain.FakeScannedPageReader
import com.hacybeyker.snapdoc.feature.camera.domain.ImportScannedPagesUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextHint
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextReading
import com.hacybeyker.snapdoc.feature.camera.domain.SaveCapturedPhotoUseCase
import java.io.IOException
import java.time.ZoneId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraPreviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(storageFailure: Throwable? = null): CameraPreviewViewModel {
        val storage = FakePhotoStorageRepository(storageFailure)
        val saveCapturedPhotoUseCase = SaveCapturedPhotoUseCase(
            photoStorageRepository = storage,
            buildScanFileNameUseCase = BuildScanFileNameUseCase(ZoneId.of("America/Lima"))
        )
        return CameraPreviewViewModel(
            saveCapturedPhotoUseCase = saveCapturedPhotoUseCase,
            importScannedPagesUseCase = ImportScannedPagesUseCase(
                scannedPageReader = FakeScannedPageReader(),
                saveCapturedPhotoUseCase = saveCapturedPhotoUseCase,
                photoStorageRepository = storage
            ),
            evaluateLiveTextHintUseCase = EvaluateLiveTextHintUseCase()
        )
    }

    private fun CameraPreviewViewModel.analyze(vararg blockCounts: Int) = blockCounts.forEach {
        onIntent(CameraPreviewIntent.FrameAnalyzed(LiveTextReading(it)))
    }

    @Test
    fun `starts while the camera is being bound`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        assertEquals(CameraPreviewUiState.Starting, sut.uiState.value)
    }

    @Test
    fun `becomes Ready once the viewfinder has a surface`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        sut.onIntent(CameraPreviewIntent.ViewfinderReady)

        assertEquals(CameraPreviewUiState.Ready(), sut.uiState.value)
    }

    @Test
    fun `a camera that cannot be bound reports Unavailable`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        sut.onIntent(CameraPreviewIntent.CameraUnavailable)

        assertEquals(CameraPreviewUiState.Unavailable, sut.uiState.value)
    }

    @Test
    fun `capturing asks the screen to take the picture and blocks the shutter meanwhile`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()
            sut.onIntent(CameraPreviewIntent.ViewfinderReady)

            sut.onIntent(CameraPreviewIntent.CapturePhoto)

            sut.effects.test { assertEquals(CameraPreviewEffect.TakePicture, awaitItem()) }
            assertEquals(CameraPreviewUiState.Ready(isCapturing = true), sut.uiState.value)
        }

    @Test
    fun `a second tap while capturing is ignored`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.CapturePhoto)

        sut.onIntent(CameraPreviewIntent.CapturePhoto)

        sut.effects.test {
            assertEquals(CameraPreviewEffect.TakePicture, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun `a captured photo is saved and surfaced as the last one`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.CapturePhoto)

        sut.onIntent(
            CameraPreviewIntent.PhotoCaptured(
                jpegBytes = byteArrayOf(1, 2, 3),
                capturedAtEpochMillis = 1_787_250_612_345
            )
        )
        advanceUntilIdle()

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals("scan_20260820_133012_345.jpg", state.lastPhoto?.fileName)
        assertEquals(false, state.isCapturing)
        assertNull(state.captureError)
    }

    @Test
    fun `a storage failure releases the shutter and reports the failure`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel(storageFailure = IOException("disk full"))
            sut.onIntent(CameraPreviewIntent.ViewfinderReady)
            sut.onIntent(CameraPreviewIntent.CapturePhoto)

            sut.onIntent(
                CameraPreviewIntent.PhotoCaptured(jpegBytes = byteArrayOf(1), capturedAtEpochMillis = 0)
            )
            advanceUntilIdle()

            val state = sut.uiState.value as CameraPreviewUiState.Ready
            assertEquals(CameraPreviewUiState.CaptureError.Storage, state.captureError)
            assertEquals(false, state.isCapturing)
            assertNull(state.lastPhoto)
        }

    @Test
    fun `a CameraX capture error releases the shutter`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.CapturePhoto)

        sut.onIntent(CameraPreviewIntent.CaptureFailed)

        val expected = CameraPreviewUiState.Ready(
            isCapturing = false,
            captureError = CameraPreviewUiState.CaptureError.Camera
        )
        assertEquals(expected, sut.uiState.value)
    }

    @Test
    fun `scanning asks the screen to open the guided scanner and blocks the button meanwhile`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()
            sut.onIntent(CameraPreviewIntent.ViewfinderReady)

            sut.onIntent(CameraPreviewIntent.ScanDocument)

            sut.effects.test { assertEquals(CameraPreviewEffect.LaunchDocumentScanner, awaitItem()) }
            assertEquals(CameraPreviewUiState.Ready(isScanning = true), sut.uiState.value)
        }

    @Test
    fun `scanned pages are imported and reported`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.ScanDocument)

        sut.onIntent(
            CameraPreviewIntent.PagesScanned(
                pageUris = listOf("content://scan/1", "content://scan/2"),
                scannedAtEpochMillis = 1_787_250_612_345
            )
        )
        advanceUntilIdle()

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(2, state.lastScan?.pageCount)
        assertEquals(false, state.isScanning)
        assertNull(state.captureError)
    }

    @Test
    fun `dismissing the scanner leaves no error behind`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.ScanDocument)

        sut.onIntent(CameraPreviewIntent.ScanDismissed)

        assertEquals(CameraPreviewUiState.Ready(), sut.uiState.value)
    }

    @Test
    fun `a scanner that cannot start is reported as such`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.ScanDocument)

        sut.onIntent(CameraPreviewIntent.ScanFailed)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(CameraPreviewUiState.CaptureError.Scanner, state.captureError)
        assertEquals(false, state.isScanning)
    }

    @Test
    fun `an imported scan offers every page to the text extractor`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)

        sut.onIntent(
            CameraPreviewIntent.PagesScanned(
                pageUris = listOf("content://scan/1", "content://scan/2"),
                scannedAtEpochMillis = 1_787_250_612_345
            )
        )
        advanceUntilIdle()

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(
            listOf("/fake/scans/scan_20260820_133012_345_p1.jpg", "/fake/scans/scan_20260820_133012_345_p2.jpg"),
            state.lastImagePaths
        )
    }

    @Test
    fun `capturing after a scan replaces it, so the text extractor reads the newer image`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()
            sut.onIntent(CameraPreviewIntent.ViewfinderReady)
            sut.onIntent(CameraPreviewIntent.PagesScanned(listOf("content://scan/1"), 1_787_250_612_345))
            advanceUntilIdle()

            sut.onIntent(CameraPreviewIntent.PhotoCaptured(byteArrayOf(1), 1_787_250_700_000))
            advanceUntilIdle()

            val state = sut.uiState.value as CameraPreviewUiState.Ready
            assertNull(state.lastScan)
            assertEquals(listOf("/fake/scans/scan_20260820_133140_000.jpg"), state.lastImagePaths)
        }

    @Test
    fun `nothing captured yet means nothing to extract text from`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        sut.onIntent(CameraPreviewIntent.ViewfinderReady)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(emptyList<String>(), state.lastImagePaths)
    }

    @Test
    fun `live analysis says nothing until enough frames agree`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)

        sut.analyze(3, 3)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(LiveTextHint.Searching, state.liveTextHint)
    }

    @Test
    fun `frames that agree on seeing text report it in the viewfinder`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)

        sut.analyze(2, 3, 4)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(LiveTextHint.TextVisible(4), state.liveTextHint)
    }

    @Test
    fun `a hand crossing the frame does not wipe the hint`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.analyze(2, 3, 4)

        sut.analyze(0)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(LiveTextHint.TextVisible(4), state.liveTextHint)
    }

    @Test
    fun `the window only remembers the newest frames, so the hint can recover`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val sut = viewModel()
            sut.onIntent(CameraPreviewIntent.ViewfinderReady)
            sut.analyze(5, 5, 5)

            sut.analyze(0, 0, 0)

            val state = sut.uiState.value as CameraPreviewUiState.Ready
            assertEquals(LiveTextHint.NoTextVisible, state.liveTextHint)
        }

    @Test
    fun `turning live analysis off forgets what it had seen`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.analyze(2, 3, 4)

        sut.onIntent(CameraPreviewIntent.ToggleLiveAnalysis)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(false, state.isLiveAnalysisEnabled)
        assertEquals(LiveTextHint.Searching, state.liveTextHint)
    }

    @Test
    fun `frames arriving while live analysis is off are ignored`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.onIntent(CameraPreviewIntent.ToggleLiveAnalysis)

        sut.analyze(2, 3, 4)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(LiveTextHint.Searching, state.liveTextHint)
    }

    @Test
    fun `turning it back on starts the window from scratch`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()
        sut.onIntent(CameraPreviewIntent.ViewfinderReady)
        sut.analyze(2, 3, 4)
        sut.onIntent(CameraPreviewIntent.ToggleLiveAnalysis)

        sut.onIntent(CameraPreviewIntent.ToggleLiveAnalysis)
        sut.analyze(6, 6)

        val state = sut.uiState.value as CameraPreviewUiState.Ready
        assertEquals(true, state.isLiveAnalysisEnabled)
        assertEquals(LiveTextHint.Searching, state.liveTextHint)
    }

    @Test
    fun `frames analyzed before the viewfinder is ready are dropped`() = runTest(mainDispatcherRule.testDispatcher) {
        val sut = viewModel()

        sut.analyze(2, 3, 4)

        assertEquals(CameraPreviewUiState.Starting, sut.uiState.value)
    }
}
