package com.hacybeyker.snapdoc.feature.home.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.ui.components.DocumentKindBadge
import com.hacybeyker.snapdoc.core.ui.components.HorizontalSpacer
import com.hacybeyker.snapdoc.core.ui.components.LabelChip
import com.hacybeyker.snapdoc.core.ui.components.Spacer
import com.hacybeyker.snapdoc.core.ui.components.labelRes
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onScanClick: () -> Unit = {},
    onLibraryClick: () -> Unit = {},
    onOpenDocument: (List<String>) -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    HomeContent(
        uiState = uiState,
        onScanClick = onScanClick,
        onLibraryClick = onLibraryClick,
        onOpenDocument = onOpenDocument,
        modifier = modifier
    )
}

@Composable
internal fun HomeContent(
    uiState: HomeUiState,
    onScanClick: () -> Unit,
    onLibraryClick: () -> Unit,
    onOpenDocument: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(MaterialTheme.spacing.md)
        ) {
            Spacer(MaterialTheme.spacing.md)
            Text(text = stringResource(R.string.app_name), style = MaterialTheme.typography.displaySmall)
            Spacer(MaterialTheme.spacing.xs)
            Text(
                text = stringResource(R.string.home_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(MaterialTheme.spacing.sm)
            // The privacy claim is the product's whole argument, so it says so on the first screen.
            LabelChip(
                text = stringResource(R.string.home_privacy_note),
                icon = Icons.Filled.Lock,
                container = MaterialTheme.colorScheme.secondaryContainer,
                content = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(MaterialTheme.spacing.lg)
            ScanActionCard(onClick = onScanClick)
            Spacer(MaterialTheme.spacing.lg)
            RecentSection(
                uiState = uiState,
                onLibraryClick = onLibraryClick,
                onOpenDocument = onOpenDocument
            )
        }
    }
}

/**
 * One unmistakable primary action, and it explains itself. "Scan" alone said nothing about what the
 * app would do with the page, which is part of why the two buttons further in felt arbitrary.
 */
@Composable
private fun ScanActionCard(onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = null,
                modifier = Modifier.size(ACTION_ICON)
            )
            HorizontalSpacer(MaterialTheme.spacing.md)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.home_scan_action),
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = stringResource(R.string.home_scan_description),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}

@Composable
private fun RecentSection(uiState: HomeUiState, onLibraryClick: () -> Unit, onOpenDocument: (List<String>) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(R.string.home_recent_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f)
        )
        if (uiState.hasDocuments) {
            TextButton(onClick = onLibraryClick) {
                Text(text = stringResource(R.string.home_see_all))
                Icon(imageVector = Icons.Filled.KeyboardArrowRight, contentDescription = null)
            }
        }
    }
    Spacer(MaterialTheme.spacing.xs)
    if (uiState.hasDocuments) {
        Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm)) {
            uiState.recentDocuments.forEach { document ->
                RecentDocumentRow(document = document, onClick = { onOpenDocument(document.imagePaths) })
            }
        }
    } else {
        HomeEmptyHint(isLoading = uiState.isLoading)
    }
}

/** Deliberately quiet while loading: a spinner on the entry screen makes the app feel slower than it is. */
@Composable
private fun HomeEmptyHint(isLoading: Boolean) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.List,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalSpacer(MaterialTheme.spacing.md)
            Text(
                text = stringResource(if (isLoading) R.string.home_recent_loading else R.string.home_recent_empty),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RecentDocumentRow(document: StoredDocument, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DocumentKindBadge(kind = document.insight.kind)
            HorizontalSpacer(MaterialTheme.spacing.md)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.insight.merchant ?: stringResource(document.insight.kind.labelRes),
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = listOfNotNull(
                        document.insight.total,
                        pluralStringResource(R.plurals.library_page_count, document.pageCount, document.pageCount)
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val ACTION_ICON = 32.dp

private val previewDocuments = listOf(
    StoredDocument(
        id = 1,
        imagePaths = listOf("/scans/a.jpg"),
        createdAtEpochMillis = 1_787_250_612_345,
        text = "HARDWARE STORE",
        insight = DocumentInsight(
            DocumentKind.Receipt,
            "Hardware Store",
            "2026-08-20",
            "16.30",
            InsightSource.OnDeviceModel
        )
    ),
    StoredDocument(
        id = 2,
        imagePaths = listOf("/scans/b.jpg", "/scans/b2.jpg"),
        createdAtEpochMillis = 1_787_150_612_345,
        text = "Notes",
        insight = DocumentInsight.empty(InsightSource.Rules)
    )
)

@Preview(showBackground = true)
@Composable
private fun HomeContentPreview() {
    SnapDocTheme {
        HomeContent(
            uiState = HomeUiState(recentDocuments = previewDocuments, isLoading = false),
            onScanClick = {},
            onLibraryClick = {},
            onOpenDocument = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeContentEmptyPreview() {
    SnapDocTheme {
        HomeContent(
            uiState = HomeUiState(isLoading = false),
            onScanClick = {},
            onLibraryClick = {},
            onOpenDocument = {}
        )
    }
}
