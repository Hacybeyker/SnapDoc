package com.hacybeyker.snapdoc.feature.camera.domain

import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Test

class BuildScanFileNameUseCaseTest {

    private val sut = BuildScanFileNameUseCase(ZoneId.of("America/Lima"))

    @Test
    fun `formats the file name from the capture instant in the given zone`() {
        // 2026-08-20T18:30:12.345Z is 13:30:12.345 in Lima (UTC-5).
        val fileName = sut(capturedAtEpochMillis = 1_787_250_612_345)

        assertEquals("scan_20260820_133012_345.jpg", fileName)
    }

    @Test
    fun `two captures within the same second get different names`() {
        val first = sut(capturedAtEpochMillis = 1_787_250_612_345)
        val second = sut(capturedAtEpochMillis = 1_787_250_612_845)

        assertEquals(false, first == second)
    }
}
