package com.hacybeyker.snapdoc.feature.ocr.domain

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Stands in for Gemini Nano, which no JVM test and no CI machine can ever run. Every failure mode the
 * degradation policy has to survive is expressible here: absent, failing to answer, or unreachable
 * even for a status check.
 */
class FakeOnDeviceDocumentAnalyzer(
    availability: ModelAvailability = ModelAvailability.Available,
    private val availabilityFailure: Throwable? = null,
    private val analysisFailure: Throwable? = null,
    private val insight: DocumentInsight = MODEL_INSIGHT,
    private val downloadSteps: List<ModelDownload> = listOf(ModelDownload.Completed)
) : OnDeviceDocumentAnalyzer {

    /** A finished download makes the model available — the real platform reports it, so this must too. */
    private var currentAvailability = availability

    var analyzedTexts = mutableListOf<String>()
        private set

    override suspend fun availability(): ModelAvailability {
        availabilityFailure?.let { throw it }
        return currentAvailability
    }

    override fun download(): Flow<ModelDownload> = flow {
        downloadSteps.forEach { step ->
            if (step == ModelDownload.Completed) currentAvailability = ModelAvailability.Available
            emit(step)
        }
    }

    override suspend fun analyze(text: String): DocumentInsight {
        analysisFailure?.let { throw it }
        analyzedTexts += text
        return insight
    }

    companion object {
        val MODEL_INSIGHT = DocumentInsight(
            kind = DocumentKind.Receipt,
            merchant = "Hardware Store",
            date = "2026-08-20",
            total = "16.30",
            source = InsightSource.OnDeviceModel
        )
    }
}
