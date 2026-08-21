package com.hacybeyker.snapdoc.feature.camera.ui

import app.cash.turbine.test
import com.hacybeyker.snapdoc.core.test.MainDispatcherRule
import com.hacybeyker.snapdoc.feature.camera.domain.EvaluateCameraPermissionUseCase
import com.hacybeyker.snapdoc.feature.camera.domain.FakeCameraPermissionRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class CameraPermissionViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(granted: Boolean) = CameraPermissionViewModel(
        cameraPermissionRepository = FakeCameraPermissionRepository(granted),
        evaluateCameraPermissionUseCase = EvaluateCameraPermissionUseCase()
    )

    @Test
    fun `permission already granted exposes Granted and requests nothing`() = runTest {
        val sut = viewModel(granted = true)

        assertEquals(CameraPermissionUiState.Granted, sut.uiState.value)
    }

    @Test
    fun `permission not granted on first load requests it automatically`() = runTest {
        val sut = viewModel(granted = false)

        sut.effects.test {
            assertEquals(CameraPermissionEffect.LaunchPermissionRequest, awaitItem())
        }
    }

    @Test
    fun `denying once moves to RationaleRequired`() = runTest {
        val sut = viewModel(granted = false)
        sut.effects.test { awaitItem() } // consume the automatic initial request

        sut.onIntent(
            CameraPermissionIntent.PermissionResultReceived(isGranted = false, shouldShowRationale = true)
        )

        assertEquals(CameraPermissionUiState.RationaleRequired, sut.uiState.value)
    }

    @Test
    fun `denying with dont-ask-again after the initial request moves to PermanentlyDenied`() = runTest {
        val sut = viewModel(granted = false)
        sut.effects.test { awaitItem() } // consume the automatic initial request

        sut.onIntent(
            CameraPermissionIntent.PermissionResultReceived(isGranted = false, shouldShowRationale = false)
        )

        assertEquals(CameraPermissionUiState.PermanentlyDenied, sut.uiState.value)
    }

    @Test
    fun `tapping allow after a rationale re-requests the permission`() = runTest {
        val sut = viewModel(granted = false)
        sut.effects.test { awaitItem() } // consume the automatic initial request
        sut.onIntent(
            CameraPermissionIntent.PermissionResultReceived(isGranted = false, shouldShowRationale = true)
        )

        sut.onIntent(CameraPermissionIntent.RequestPermission)

        sut.effects.test {
            assertEquals(CameraPermissionEffect.LaunchPermissionRequest, awaitItem())
        }
    }
}
