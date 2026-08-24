package com.hacybeyker.snapdoc.feature.ocr.domain

import kotlinx.coroutines.flow.Flow

/**
 * Whether the generative model can run here. Not every device can: the model needs hardware support
 * and a download of its own, so "unavailable" is an ordinary state to design for, not an error path.
 */
enum class ModelAvailability { Unavailable, Downloadable, Downloading, Available }

sealed interface ModelDownload {
    data class InProgress(val bytesDownloaded: Long) : ModelDownload
    data object Completed : ModelDownload
    data class Failed(val cause: Throwable) : ModelDownload
}

/**
 * The on-device generative model. Kept behind a contract so the degradation policy can be decided in
 * a use case and tested without a device that happens to support Gemini Nano — which no CI machine
 * and no JVM test ever will.
 */
interface OnDeviceDocumentAnalyzer {

    suspend fun availability(): ModelAvailability

    /** Cold flow: collecting it starts the download and reports how far it got. */
    fun download(): Flow<ModelDownload>

    /** Only meaningful once [availability] is [ModelAvailability.Available]; may still fail. */
    suspend fun analyze(text: String): DocumentInsight
}
