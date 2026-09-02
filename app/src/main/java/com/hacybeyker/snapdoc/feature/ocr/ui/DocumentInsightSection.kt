package com.hacybeyker.snapdoc.feature.ocr.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.ui.components.DocumentKindBadge
import com.hacybeyker.snapdoc.core.ui.components.HorizontalSpacer
import com.hacybeyker.snapdoc.core.ui.components.LabelChip
import com.hacybeyker.snapdoc.core.ui.components.Spacer
import com.hacybeyker.snapdoc.core.ui.components.labelRes
import com.hacybeyker.snapdoc.core.ui.theme.spacing

/*
 * What the app understood about the document. Kept out of DocumentTextScreen.kt for the reason
 * detekt's TooManyFunctions surfaced: this is a self-contained section with its own vocabulary.
 */

/** Null while the analysis runs: the text is already useful, so it is not held back waiting on this. */
@Composable
internal fun InsightCard(insight: DocumentInsight?, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            if (insight == null) {
                AnalyzingRow()
                return@Card
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                DocumentKindBadge(kind = insight.kind)
                HorizontalSpacer(MaterialTheme.spacing.md)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(insight.kind.labelRes),
                        style = MaterialTheme.typography.titleMedium
                    )
                    // Naming the engine is the honest part: the rules never fill in a merchant, and
                    // hiding that makes a blank field look like the document had no shop name.
                    SourceChip(source = insight.source)
                }
            }
            if (insight.hasFields) {
                Spacer(MaterialTheme.spacing.md)
                InsightField(Icons.Filled.ShoppingCart, R.string.document_insight_merchant, insight.merchant)
                InsightField(Icons.Filled.DateRange, R.string.document_insight_date, insight.date)
                InsightField(Icons.Filled.Star, R.string.document_insight_total, insight.total)
            } else {
                Spacer(MaterialTheme.spacing.sm)
                Text(
                    text = stringResource(R.string.document_insight_no_fields),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AnalyzingRow() {
    Text(
        text = stringResource(R.string.document_insight_analyzing),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Spacer(MaterialTheme.spacing.sm)
    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
}

/** Amber for the model, neutral for the rules — the palette reserves that colour for exactly this. */
@Composable
private fun SourceChip(source: InsightSource) {
    val fromModel = source == InsightSource.OnDeviceModel
    Spacer(MaterialTheme.spacing.xs)
    LabelChip(
        text = stringResource(
            if (fromModel) R.string.document_insight_by_model else R.string.document_insight_by_rules
        ),
        icon = if (fromModel) Icons.Filled.Star else Icons.Filled.Info,
        container = if (fromModel) {
            MaterialTheme.colorScheme.tertiaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        content = if (fromModel) {
            MaterialTheme.colorScheme.onTertiaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    )
}

@Composable
private fun InsightField(icon: ImageVector, labelRes: Int, value: String?) {
    if (value == null) return
    Row(
        modifier = Modifier.padding(vertical = MaterialTheme.spacing.xxs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = MaterialTheme.spacing.sm)
        )
        Text(
            text = stringResource(labelRes),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        HorizontalSpacer(MaterialTheme.spacing.xs)
        Text(text = value, style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * Only shown when there is something the user can act on. A device that simply cannot run the model
 * gets no row at all: an apology for hardware nobody can change is noise.
 */
@Composable
internal fun ModelStatusRow(
    modelStatus: DocumentTextUiState.ModelStatus,
    onEnableModelClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (modelStatus == DocumentTextUiState.ModelStatus.Unavailable ||
        modelStatus == DocumentTextUiState.ModelStatus.Ready
    ) {
        return
    }
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (modelStatus) {
            DocumentTextUiState.ModelStatus.Downloadable -> TextButton(onClick = onEnableModelClick) {
                Icon(imageVector = Icons.Filled.Star, contentDescription = null)
                HorizontalSpacer(MaterialTheme.spacing.xs)
                Text(text = stringResource(R.string.document_insight_enable_model))
            }

            is DocumentTextUiState.ModelStatus.Downloading -> Text(
                text = stringResource(
                    R.string.document_insight_downloading,
                    modelStatus.bytesDownloaded / BYTES_PER_MEGABYTE
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            DocumentTextUiState.ModelStatus.DownloadFailed -> TextButton(onClick = onEnableModelClick) {
                Icon(imageVector = Icons.Filled.Refresh, contentDescription = null)
                HorizontalSpacer(MaterialTheme.spacing.xs)
                Text(text = stringResource(R.string.document_insight_download_failed))
            }

            else -> Unit
        }
    }
}

private const val BYTES_PER_MEGABYTE = 1_048_576
