package com.hacybeyker.snapdoc.feature.ocr.domain

import javax.inject.Inject

/**
 * Decides which engine gets to read the document, and is therefore where the whole degradation
 * policy lives — one place, pure, testable without a device that supports Gemini Nano.
 *
 * The policy: the model is preferred when it is actually ready, and every other outcome falls back
 * to rules rather than surfacing an error. That includes the model being present but failing
 * mid-inference, which is the case worth designing for: on-device generation can fail because the
 * model was evicted, the device is thermally throttled or the text is longer than its context, and
 * none of those are reasons to leave the user staring at nothing when the rules can still answer.
 */
class GenerateDocumentInsightUseCase @Inject constructor(
    private val onDeviceDocumentAnalyzer: OnDeviceDocumentAnalyzer,
    private val extractDocumentFieldsUseCase: ExtractDocumentFieldsUseCase
) {

    suspend operator fun invoke(text: String): DocumentInsight = when {
        text.isBlank() -> DocumentInsight.empty(InsightSource.Rules)

        modelIsReady() -> runCatching { onDeviceDocumentAnalyzer.analyze(text) }
            .getOrElse { extractDocumentFieldsUseCase(text) }

        else -> extractDocumentFieldsUseCase(text)
    }

    /** A status check that throws counts as no model: the point is to answer, not to explain why. */
    private suspend fun modelIsReady(): Boolean = runCatching { onDeviceDocumentAnalyzer.availability() }
        .getOrDefault(ModelAvailability.Unavailable) == ModelAvailability.Available
}
