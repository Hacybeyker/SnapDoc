package com.hacybeyker.snapdoc.feature.ocr.ui

/** One-shot side effects: the clipboard is a system service, so only the Screen touches it. */
sealed interface DocumentTextEffect {
    data class CopyToClipboard(val text: String) : DocumentTextEffect
}
