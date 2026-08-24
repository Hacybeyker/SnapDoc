package com.hacybeyker.snapdoc.feature.ocr.data

import com.google.mlkit.genai.common.DownloadStatus
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.prompt.Generation
import com.hacybeyker.snapdoc.core.coroutines.IoDispatcher
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.feature.ocr.domain.ModelAvailability
import com.hacybeyker.snapdoc.feature.ocr.domain.ModelDownload
import com.hacybeyker.snapdoc.feature.ocr.domain.OnDeviceDocumentAnalyzer
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Gemini Nano through ML Kit's Prompt API. The whole point is that the document's text never leaves
 * the phone: inference happens on the device, offline, with no per-request cost.
 *
 * The client is a singleton because it owns the loaded model, and it is never closed for the same
 * reason the text recognizer is not — it lives as long as the process, and closing it would only
 * strand the next call.
 */
@Singleton
class GeminiNanoDocumentAnalyzer @Inject constructor(@IoDispatcher private val ioDispatcher: CoroutineDispatcher) :
    OnDeviceDocumentAnalyzer {

    private val generativeModel by lazy { Generation.getClient() }

    override suspend fun availability(): ModelAvailability = withContext(ioDispatcher) {
        // checkStatus returns a plain Int: FeatureStatus is an annotation holding constants, not an enum.
        when (generativeModel.checkStatus()) {
            FeatureStatus.AVAILABLE -> ModelAvailability.Available
            FeatureStatus.DOWNLOADABLE -> ModelAvailability.Downloadable
            FeatureStatus.DOWNLOADING -> ModelAvailability.Downloading
            else -> ModelAvailability.Unavailable
        }
    }

    override fun download(): Flow<ModelDownload> = generativeModel.download()
        .map { status ->
            when (status) {
                is DownloadStatus.DownloadStarted -> ModelDownload.InProgress(bytesDownloaded = 0)
                is DownloadStatus.DownloadProgress -> ModelDownload.InProgress(status.totalBytesDownloaded)
                DownloadStatus.DownloadCompleted -> ModelDownload.Completed
                is DownloadStatus.DownloadFailed -> ModelDownload.Failed(status.e)
                else -> ModelDownload.InProgress(bytesDownloaded = 0)
            }
        }
        .flowOn(ioDispatcher)

    override suspend fun analyze(text: String): DocumentInsight = withContext(ioDispatcher) {
        val response = generativeModel.generateContent(buildPrompt(text))
        response.candidates.firstOrNull()?.text.orEmpty().toDocumentInsight()
    }

    /**
     * The text is truncated because Gemini Nano has a small context window and a long scan would be
     * rejected outright. The head of a receipt or invoice is where the merchant, date and total live,
     * so cutting the tail loses far less than failing the whole request would.
     */
    private fun buildPrompt(text: String): String {
        val document = text.take(MAX_PROMPT_CHARACTERS)
        return """
            You are labelling a scanned document. Reply with exactly these four lines and nothing else.
            Do not explain. Do not use markdown.
            KIND: one of RECEIPT, INVOICE, ID, NOTE, UNKNOWN
            MERCHANT: the business or issuer name, or NONE
            DATE: the document date exactly as it appears, or NONE
            TOTAL: the total amount including its currency, or NONE

            Document:
            $document
        """.trimIndent()
    }

    private companion object {
        const val MAX_PROMPT_CHARACTERS = 4000
    }
}
