package com.hacybeyker.snapdoc.feature.library.domain

import com.hacybeyker.snapdoc.core.document.DocumentKind
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Names the exported file after what the document is and when it was taken, because this name is the
 * one the recipient sees in their inbox — `receipt_20260820_1330.pdf` says more than `document.pdf`.
 *
 * The merchant is folded in when the model found one, sanitized down to safe characters: a shop name
 * can contain a slash or a colon, and those do not survive a file system or an email attachment.
 */
class BuildPdfFileNameUseCase @Inject constructor(private val zoneId: ZoneId) {

    operator fun invoke(kind: DocumentKind, merchant: String?, createdAtEpochMillis: Long): String {
        val timestamp = FORMATTER.format(Instant.ofEpochMilli(createdAtEpochMillis).atZone(zoneId))
        val label = merchant?.sanitized()?.takeIf { it.isNotEmpty() }
        return listOfNotNull(kind.name.lowercase(), label, timestamp).joinToString("_") + ".pdf"
    }

    private fun String.sanitized(): String = trim()
        .map { if (it.isLetterOrDigit()) it else ' ' }
        .joinToString("")
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(MAX_LABEL_WORDS)
        .joinToString("-")
        .lowercase()

    private companion object {
        val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmm")

        /** A long shop name would push the timestamp past what some mail clients show. */
        const val MAX_LABEL_WORDS = 3
    }
}
