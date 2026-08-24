package com.hacybeyker.snapdoc.feature.camera.domain

/** A single scan run: the guided scanner returns one or more cropped, deskewed pages. */
data class ScannedDocument(val pages: List<CapturedPhoto>) {
    val pageCount: Int get() = pages.size
}
