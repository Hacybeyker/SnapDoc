package com.hacybeyker.snapdoc.feature.ocr.ui

sealed interface DocumentTextIntent {

    /**
     * The pages arrive from the navigation key, which Navigation 3 hands to the Screen and not to
     * the ViewModel — so the Screen forwards them as the intent that starts the work.
     */
    data class RecognizeText(val imagePaths: List<String>) : DocumentTextIntent

    data object Retry : DocumentTextIntent

    data object CopyText : DocumentTextIntent

    /** Downloading the model is a deliberate choice: it is a large file on the user's connection. */
    data object EnableOnDeviceModel : DocumentTextIntent
}
