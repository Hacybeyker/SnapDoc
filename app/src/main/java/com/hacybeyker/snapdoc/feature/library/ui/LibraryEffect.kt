package com.hacybeyker.snapdoc.feature.library.ui

/** One-shot side effects: only the Screen turns a path into a shareable URI and opens the chooser. */
sealed interface LibraryEffect {
    data class SharePdf(val filePath: String) : LibraryEffect
    data object ExportFailed : LibraryEffect
}
