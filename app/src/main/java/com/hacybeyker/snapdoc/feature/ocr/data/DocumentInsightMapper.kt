package com.hacybeyker.snapdoc.feature.ocr.data

import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource

/**
 * Maps the model's reply into the domain.
 *
 * The reply is asked for as four `KEY: value` lines rather than JSON on purpose. Gemini Nano is a
 * small model: it drops closing braces, wraps output in prose or in ``` fences, and one malformed
 * character makes a whole JSON payload unparseable. Line-oriented output degrades a field at a time
 * instead — an unreadable TOTAL line costs the total, not the whole insight — so this parser reads
 * whatever lines it recognizes and treats everything else as absent.
 */
internal fun String.toDocumentInsight(): DocumentInsight {
    val fields = lineSequence()
        .mapNotNull { line ->
            val separator = line.indexOf(':')
            if (separator <= 0) return@mapNotNull null
            // Letters only: it is the one normalization that survives every decoration the model
            // reaches for — `**KIND:**`, `- KIND:`, `# KIND :` all collapse to the same key.
            val key = line.take(separator).uppercase().filter { it.isLetter() }
            key to line.substring(separator + 1).trim()
        }
        .toMap()

    return DocumentInsight(
        kind = fields["KIND"].toDocumentKind(),
        merchant = fields["MERCHANT"].orNull(),
        date = fields["DATE"].orNull(),
        total = fields["TOTAL"].orNull(),
        source = InsightSource.OnDeviceModel
    )
}

/** The model is asked to write NONE, but it also likes "N/A" and empty values; all mean the same. */
private fun String?.orNull(): String? {
    val cleaned = this?.unwrap()
    return cleaned?.takeUnless { it.isEmpty() || it.uppercase() in ABSENT }
}

/**
 * One pass over decoration and whitespace together. Trimming them separately leaves the inner star
 * of `** *Acme Ltd*` behind, because each trim stops at the first character the other owns.
 */
private fun String.unwrap(): String = trim { it == '*' || it == '"' || it == '#' || it.isWhitespace() }

private fun String?.toDocumentKind(): DocumentKind = when (this?.unwrap()?.uppercase()) {
    "RECEIPT" -> DocumentKind.Receipt
    "INVOICE" -> DocumentKind.Invoice
    "ID" -> DocumentKind.IdDocument
    "NOTE" -> DocumentKind.Note
    else -> DocumentKind.Unknown
}

private val ABSENT = setOf("NONE", "N/A", "NA", "NULL", "UNKNOWN", "-")
