package com.hacybeyker.snapdoc.feature.library.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.ui.components.AppTopBar
import com.hacybeyker.snapdoc.core.ui.components.DocumentKindBadge
import com.hacybeyker.snapdoc.core.ui.components.EmptyState
import com.hacybeyker.snapdoc.core.ui.components.HorizontalSpacer
import com.hacybeyker.snapdoc.core.ui.components.LabelChip
import com.hacybeyker.snapdoc.core.ui.components.Spacer
import com.hacybeyker.snapdoc.core.ui.components.labelRes
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import java.io.File

@Composable
fun LibraryScreen(
    onOpenDocument: (List<String>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LibraryEffects(viewModel = viewModel, snackbarHostState = snackbarHostState)

    LibraryContent(
        uiState = uiState,
        actions = LibraryActions(
            onQueryChange = { viewModel.onIntent(LibraryIntent.QueryChanged(it)) },
            onDeleteClick = { viewModel.onIntent(LibraryIntent.DeleteDocument(it)) },
            onShareClick = { viewModel.onIntent(LibraryIntent.ExportDocument(it)) },
            onOpenDocument = onOpenDocument,
            onBack = onBack
        ),
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

@Composable
private fun LibraryEffects(viewModel: LibraryViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.library_share_title)
    val failedMessage = stringResource(R.string.library_export_failed)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is LibraryEffect.SharePdf -> {
                    // The URI comes from FileProvider, never from the raw path: a file:// URI would
                    // be rejected outright since Android 7, and the temporary read grant on this one
                    // is what lets the other app open the PDF without any access to the rest.
                    //
                    // The authority must match the manifest's "${applicationId}.fileprovider", which
                    // resolves at build time to the same value packageName returns at runtime.
                    val uri = FileProvider.getUriForFile(
                        context,
                        "${context.packageName}$FILE_PROVIDER_SUFFIX",
                        File(effect.filePath)
                    )
                    val share = Intent(Intent.ACTION_SEND).apply {
                        type = PDF_MIME_TYPE
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                    context.startActivity(Intent.createChooser(share, chooserTitle))
                }

                LibraryEffect.ExportFailed -> snackbarHostState.showSnackbar(failedMessage)
            }
        }
    }
}

private const val PDF_MIME_TYPE = "application/pdf"

/** Kept next to the intent that uses it, so the manifest's authority has one obvious counterpart. */
private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

/**
 * The five things this screen can ask for. They are grouped because they always travel together —
 * the screen, its list and every preview pass the same set — and because a composable with eight
 * parameters is one where the call site stops being readable.
 */
internal data class LibraryActions(
    val onQueryChange: (String) -> Unit = {},
    val onDeleteClick: (Long) -> Unit = {},
    val onShareClick: (Long) -> Unit = {},
    val onOpenDocument: (List<String>) -> Unit = {},
    val onBack: () -> Unit = {}
)

@Composable
internal fun LibraryContent(
    uiState: LibraryUiState,
    actions: LibraryActions,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AppTopBar(
                title = stringResource(R.string.library_title),
                subtitle = stringResource(R.string.library_subtitle),
                onBack = actions.onBack
            )
            SearchField(
                query = uiState.query,
                onQueryChange = actions.onQueryChange,
                modifier = Modifier.padding(horizontal = MaterialTheme.spacing.md)
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> CircularProgressIndicator()

                    uiState.isEmptyArchive -> EmptyState(
                        icon = Icons.AutoMirrored.Filled.List,
                        title = stringResource(R.string.library_empty_title),
                        message = stringResource(R.string.library_empty)
                    )

                    uiState.hasNoMatches -> EmptyState(
                        icon = Icons.Filled.Search,
                        title = stringResource(R.string.library_no_matches_title),
                        message = stringResource(R.string.library_no_matches),
                        actionLabel = stringResource(R.string.library_clear_search),
                        onAction = { actions.onQueryChange("") }
                    )

                    else -> DocumentList(
                        documents = uiState.documents,
                        onDeleteClick = actions.onDeleteClick,
                        onShareClick = actions.onShareClick,
                        onOpenDocument = actions.onOpenDocument
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        placeholder = { Text(text = stringResource(R.string.library_search_label)) },
        leadingIcon = { Icon(imageVector = Icons.Filled.Search, contentDescription = null) },
        trailingIcon = {
            // Only offered when there is something to clear, so the field is not permanently cluttered.
            if (query.isNotEmpty()) {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = stringResource(R.string.library_clear_search)
                    )
                }
            }
        },
        singleLine = true,
        shape = MaterialTheme.shapes.large,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun DocumentList(
    documents: List<StoredDocument>,
    onDeleteClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onOpenDocument: (List<String>) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.sm),
        contentPadding = PaddingValues(MaterialTheme.spacing.md)
    ) {
        // Keyed by id so deleting one row does not recompose every row below it.
        items(items = documents, key = { it.id }) { document ->
            DocumentRow(
                document = document,
                onDeleteClick = { onDeleteClick(document.id) },
                onShareClick = { onShareClick(document.id) },
                onClick = { onOpenDocument(document.imagePaths) }
            )
        }
    }
}

@Composable
private fun DocumentRow(
    document: StoredDocument,
    onDeleteClick: () -> Unit,
    onShareClick: () -> Unit,
    onClick: () -> Unit
) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.Top
        ) {
            DocumentKindBadge(kind = document.insight.kind)
            HorizontalSpacer(MaterialTheme.spacing.md)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = document.insight.merchant ?: stringResource(document.insight.kind.labelRes),
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(MaterialTheme.spacing.xs)
                DocumentMeta(document = document)
            }
            // Icon buttons, not text: two labelled buttons per row turned the list into a wall of words.
            IconButton(onClick = onShareClick) {
                Icon(
                    imageVector = Icons.Filled.Share,
                    contentDescription = stringResource(R.string.library_share)
                )
            }
            IconButton(onClick = onDeleteClick) {
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = stringResource(R.string.library_delete),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun DocumentMeta(document: StoredDocument) {
    Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.xs)) {
        document.insight.total?.let {
            LabelChip(
                text = it,
                container = MaterialTheme.colorScheme.primaryContainer,
                content = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        document.insight.date?.let { LabelChip(text = it) }
        LabelChip(text = pluralStringResource(R.plurals.library_page_count, document.pageCount, document.pageCount))
    }
    if (!document.hasBeenRead) {
        Spacer(MaterialTheme.spacing.xs)
        Text(
            text = stringResource(R.string.library_not_read),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private val previewDocuments = listOf(
    StoredDocument(
        id = 1,
        imagePaths = listOf("/scans/a_p1.jpg", "/scans/a_p2.jpg"),
        createdAtEpochMillis = 1_787_250_612_345,
        text = "HARDWARE STORE\nTOTAL 16.30",
        insight = DocumentInsight(
            kind = DocumentKind.Receipt,
            merchant = "Hardware Store",
            date = "2026-08-20",
            total = "16.30",
            source = InsightSource.OnDeviceModel
        )
    ),
    StoredDocument(
        id = 2,
        imagePaths = listOf("/scans/b_p1.jpg"),
        createdAtEpochMillis = 1_787_150_612_345,
        text = "",
        insight = DocumentInsight.empty(InsightSource.Rules)
    )
)

@Preview(showBackground = true)
@Composable
private fun LibraryContentPreview() {
    SnapDocTheme {
        LibraryContent(
            uiState = LibraryUiState(documents = previewDocuments, isLoading = false),
            actions = LibraryActions()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentEmptyPreview() {
    SnapDocTheme {
        LibraryContent(
            uiState = LibraryUiState(isLoading = false),
            actions = LibraryActions()
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentNoMatchesPreview() {
    SnapDocTheme {
        LibraryContent(
            uiState = LibraryUiState(query = "plumber", isLoading = false),
            actions = LibraryActions()
        )
    }
}
