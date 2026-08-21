package com.hacybeyker.snapdoc.feature.camera.domain

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * The timestamp is formatted in the device's zone on purpose: the file name is something the user
 * reads, so "the moment I took it" beats UTC correctness here. The zone is a constructor parameter
 * so tests can pin it instead of depending on the machine running them (Hilt provides the
 * device's zone; Dagger cannot read Kotlin default arguments, hence the explicit binding).
 */
class BuildScanFileNameUseCase @Inject constructor(private val zoneId: ZoneId) {

    operator fun invoke(capturedAtEpochMillis: Long): String {
        val timestamp = FORMATTER.format(Instant.ofEpochMilli(capturedAtEpochMillis).atZone(zoneId))
        return "scan_$timestamp.jpg"
    }

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")
    }
}
