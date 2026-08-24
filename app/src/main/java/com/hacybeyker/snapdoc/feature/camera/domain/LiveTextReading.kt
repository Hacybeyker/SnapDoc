package com.hacybeyker.snapdoc.feature.camera.domain

/**
 * What a single analyzed frame saw. Only the count survives the analyzer: the frame itself is a
 * CameraX buffer that must be released immediately, and the live hint never shows the text anyway —
 * reading it is [ScannedDocument]'s job, after the shot.
 */
data class LiveTextReading(val blockCount: Int) {

    val hasText: Boolean get() = blockCount > 0
}
