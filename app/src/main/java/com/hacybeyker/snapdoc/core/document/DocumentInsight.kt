package com.hacybeyker.snapdoc.core.document

/*
 * Promoted out of feature/ocr the moment a second slice needed it: the OCR slice produces an
 * insight and the library slice stores one, which is exactly the threshold AGENTS.md sets for core/.
 */

/** What the document turned out to be. [Unknown] is a real answer, not a failure. */
enum class DocumentKind { Receipt, Invoice, IdDocument, Note, Unknown }

/**
 * Which engine produced an insight. The UI shows it because the two are not equally good and saying
 * so is more useful than pretending: the rules never fill in a merchant, and hiding that would make
 * a device without the model look like one where the document simply had no shop name on it.
 */
enum class InsightSource { OnDeviceModel, Rules }

/**
 * What the app understood from a scanned document. Every field is nullable because "not found" is
 * the normal outcome for most documents — a handwritten note has no total, an ID card has no merchant.
 */
data class DocumentInsight(
    val kind: DocumentKind,
    val merchant: String?,
    val date: String?,
    val total: String?,
    val source: InsightSource
) {

    val hasFields: Boolean get() = merchant != null || date != null || total != null

    companion object {
        fun empty(source: InsightSource) = DocumentInsight(
            kind = DocumentKind.Unknown,
            merchant = null,
            date = null,
            total = null,
            source = source
        )
    }
}
