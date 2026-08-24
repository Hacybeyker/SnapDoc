package com.hacybeyker.snapdoc.feature.ocr.ui

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizedDocument

sealed interface DocumentTextUiState {

    data object Recognizing : DocumentTextUiState

    data class Content(
        val document: RecognizedDocument,
        /** Null only while the first analysis is still running; the rules always answer eventually. */
        val insight: DocumentInsight? = null,
        val modelStatus: ModelStatus = ModelStatus.Unavailable
    ) : DocumentTextUiState

    /**
     * What the screen can say and offer about the generative model. Kept apart from [DocumentInsight]
     * because it describes the device, not the document: the same phone gives the same status for
     * every scan, and only [Downloadable] is something the user can act on.
     */
    sealed interface ModelStatus {
        data object Unavailable : ModelStatus
        data object Downloadable : ModelStatus
        data class Downloading(val bytesDownloaded: Long) : ModelStatus
        data object Ready : ModelStatus
        data object DownloadFailed : ModelStatus
    }

    /**
     * Apart from [Content] because a page that simply has no text is not a failure: retrying it
     * would produce the same empty result, so the screen must not offer a retry here.
     */
    data object Empty : DocumentTextUiState

    data object Error : DocumentTextUiState
}
