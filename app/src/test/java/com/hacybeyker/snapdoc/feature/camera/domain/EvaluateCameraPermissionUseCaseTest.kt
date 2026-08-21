package com.hacybeyker.snapdoc.feature.camera.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class EvaluateCameraPermissionUseCaseTest {

    private val evaluate = EvaluateCameraPermissionUseCase()

    @Test
    fun `returns Granted when permission is granted`() {
        val status = evaluate(isGranted = true, shouldShowRationale = false, hasRequestedBefore = false)

        assertEquals(CameraPermissionStatus.Granted, status)
    }

    @Test
    fun `returns NotRequested on the first check, before ever asking`() {
        val status = evaluate(isGranted = false, shouldShowRationale = false, hasRequestedBefore = false)

        assertEquals(CameraPermissionStatus.NotRequested, status)
    }

    @Test
    fun `returns RationaleRequired after a single denial`() {
        val status = evaluate(isGranted = false, shouldShowRationale = true, hasRequestedBefore = true)

        assertEquals(CameraPermissionStatus.RationaleRequired, status)
    }

    @Test
    fun `returns PermanentlyDenied when denied with dont-ask-again after a prior request`() {
        val status = evaluate(isGranted = false, shouldShowRationale = false, hasRequestedBefore = true)

        assertEquals(CameraPermissionStatus.PermanentlyDenied, status)
    }
}
