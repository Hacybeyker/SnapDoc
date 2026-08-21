package com.hacybeyker.snapdoc.feature.camera.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.camera.domain.BuildScanFileNameUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.FakePhotoStorageRepository
import com.hacybeyker.snapdoc.feature.camera.domain.SaveCapturedPhotoUseCase
import java.io.IOException
import java.time.ZoneId
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

class CameraPreviewViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(storageFailure: Throwable? = null) = CameraPreviewViewModel(
        saveCapturedPhotoUseCase = SaveCapturedPhotoUseCase(
            photoStorageRepository = FakePhotoStorageRepository(storageFailure),
            buildScanFileNameUseCase = BuildScanFileNameUseCase(ZoneId.of("America/Lima"))
        )
    )

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
}
