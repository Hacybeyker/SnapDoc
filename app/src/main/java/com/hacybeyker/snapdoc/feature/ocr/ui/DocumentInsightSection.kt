package com.hacybeyker.snapdoc.feature.ocr.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.ocr.domain.DocumentInsight
import com.hacybeyker.snapdoc.feature.ocr.domain.DocumentKind
import com.hacybeyker.snapdoc.feature.ocr.domain.InsightSource

/*
 * What the app understood about the document, and what it can say about the model that read it.
 * Kept out of DocumentTextScreen.kt for the reason detekt's TooManyFunctions surfaced: this is a
 * self-contained section with its own vocabulary, and the screen is easier to read without it.
 */

/** Null while the analysis runs: the text is already useful, so it is not held back waiting on this. */
@Composable
internal fun InsightCard(insight: DocumentInsight?) {
    if (insight == null) {
        Text(
            text = stringResource(R.string.document_insight_analyzing),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            Text(text = stringResource(insight.kind.labelRes), style = MaterialTheme.typography.titleMedium)
            if (insight.hasFields) {
                Spacer(Modifier.height(MaterialTheme.spacing.sm))
                InsightField(R.string.document_insight_merchant, insight.merchant)
                InsightField(R.string.document_insight_date, insight.date)
                InsightField(R.string.document_insight_total, insight.total)
            } else {
                Spacer(Modifier.height(MaterialTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.document_insight_no_fields),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Spacer(Modifier.height(MaterialTheme.spacing.sm))
            // Saying which engine answered is the honest part: the rules never fill in a merchant,
            // and hiding that would make a blank field look like the document had no shop name.
            Text(
                text = stringResource(
                    if (insight.source == InsightSource.OnDeviceModel) {
                        R.string.document_insight_by_model
                    } else {
                        R.string.document_insight_by_rules
                    }
                ),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightField(labelRes: Int, value: String?) {
    if (value == null) return
    Text(
        text = stringResource(labelRes, value),
        style = MaterialTheme.typography.bodyMedium
    )
}

@Composable
internal fun ModelStatusRow(modelStatus: DocumentTextUiState.ModelStatus, onEnableModelClick: () -> Unit) {
    // Nothing to say when the device cannot run the model: the rules already answered, and an
    // apology for hardware the user cannot change is noise.
    if (modelStatus == DocumentTextUiState.ModelStatus.Unavailable) return
    Spacer(Modifier.height(MaterialTheme.spacing.sm))
    when (modelStatus) {
        DocumentTextUiState.ModelStatus.Downloadable -> TextButton(onClick = onEnableModelClick) {
            Text(text = stringResource(R.string.document_insight_enable_model))
        }

        is DocumentTextUiState.ModelStatus.Downloading -> Text(
            text = stringResource(
                R.string.document_insight_downloading,
                modelStatus.bytesDownloaded / BYTES_PER_MEGABYTE
            ),
            style = MaterialTheme.typography.bodySmall
        )

        DocumentTextUiState.ModelStatus.DownloadFailed -> TextButton(onClick = onEnableModelClick) {
            Text(text = stringResource(R.string.document_insight_download_failed))
        }

        else -> Unit
    }
}

private const val BYTES_PER_MEGABYTE = 1_048_576

private val DocumentKind.labelRes: Int
    get() = when (this) {
        DocumentKind.Receipt -> R.string.document_kind_receipt
        DocumentKind.Invoice -> R.string.document_kind_invoice
        DocumentKind.IdDocument -> R.string.document_kind_id
        DocumentKind.Note -> R.string.document_kind_note
        DocumentKind.Unknown -> R.string.document_kind_unknown
    }
