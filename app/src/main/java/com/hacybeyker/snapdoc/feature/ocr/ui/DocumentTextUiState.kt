package com.hacybeyker.snapdoc.feature.ocr.ui

import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizedDocument

sealed interface DocumentTextUiState {

    data object Recognizing : DocumentTextUiState

    data class Content(val document: RecognizedDocument) : DocumentTextUiState

    /**
     * Apart from [Content] because a page that simply has no text is not a failure: retrying it
     * would produce the same empty result, so the screen must not offer a retry here.
     */
    data object Empty : DocumentTextUiState

    data object Error : DocumentTextUiState
}
