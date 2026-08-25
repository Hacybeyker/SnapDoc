package com.hacybeyker.snapdoc.feature.library.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.library.domain.StoredDocument
import java.io.File

@Composable
fun LibraryScreen(
    onOpenDocument: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LibraryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LibraryEffects(viewModel = viewModel, snackbarHostState = snackbarHostState)

    LibraryContent(
        uiState = uiState,
        onQueryChange = { viewModel.onIntent(LibraryIntent.QueryChanged(it)) },
        onDeleteClick = { viewModel.onIntent(LibraryIntent.DeleteDocument(it)) },
        onShareClick = { viewModel.onIntent(LibraryIntent.ExportDocument(it)) },
        onOpenDocument = onOpenDocument,
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

@Composable
private fun LibraryContent(
    uiState: LibraryUiState,
    onQueryChange: (String) -> Unit,
    onDeleteClick: (Long) -> Unit,
    onShareClick: (Long) -> Unit,
    onOpenDocument: (List<String>) -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding).padding(MaterialTheme.spacing.md)) {
            OutlinedTextField(
                value = uiState.query,
                onValueChange = onQueryChange,
                label = { Text(text = stringResource(R.string.library_search_label)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when {
                    uiState.isLoading -> CircularProgressIndicator()
                    uiState.isEmptyArchive -> CenteredMessage(stringResource(R.string.library_empty))
                    uiState.hasNoMatches -> CenteredMessage(stringResource(R.string.library_no_matches))
                    else -> DocumentList(
                        documents = uiState.documents,
                        onDeleteClick = onDeleteClick,
                        onShareClick = onShareClick,
                        onOpenDocument = onOpenDocument
                    )
                }
            }
        }
    }
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
        contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = MaterialTheme.spacing.md)
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
        Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
            Text(
                text = stringResource(document.insight.kind.labelRes),
                style = MaterialTheme.typography.titleMedium
            )
            document.insight.merchant?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium)
            }
            if (!document.hasBeenRead) {
                Text(
                    text = stringResource(R.string.library_not_read),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Text(
                text = document.summaryLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onDeleteClick) {
                    Text(text = stringResource(R.string.library_delete))
                }
                TextButton(onClick = onShareClick) {
                    Text(text = stringResource(R.string.library_share))
                }
            }
        }
    }
}

/** Whatever the document actually has: a total and a date are both optional on a real scan. */
@Composable
private fun StoredDocument.summaryLine(): String {
    val pages = pluralStringResource(R.plurals.library_page_count, pageCount, pageCount)
    return listOfNotNull(insight.total, insight.date, pages).joinToString(separator = " · ")
}

@Composable
private fun CenteredMessage(message: String) {
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(MaterialTheme.spacing.lg)
    )
}

private val DocumentKind.labelRes: Int
    get() = when (this) {
        DocumentKind.Receipt -> R.string.document_kind_receipt
        DocumentKind.Invoice -> R.string.document_kind_invoice
        DocumentKind.IdDocument -> R.string.document_kind_id
        DocumentKind.Note -> R.string.document_kind_note
        DocumentKind.Unknown -> R.string.document_kind_unknown
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
        text = "Remember to call the plumber",
        insight = DocumentInsight.empty(InsightSource.Rules)
    )
)

@Preview(showBackground = true)
@Composable
private fun LibraryContentPreview() {
    SnapDocTheme {
        LibraryContent(
            uiState = LibraryUiState(documents = previewDocuments, isLoading = false),
            onQueryChange = {},
            onDeleteClick = {},
            onShareClick = {},
            onOpenDocument = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentEmptyPreview() {
    SnapDocTheme {
        LibraryContent(
            uiState = LibraryUiState(isLoading = false),
            onQueryChange = {},
            onDeleteClick = {},
            onShareClick = {},
            onOpenDocument = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun LibraryContentNoMatchesPreview() {
    SnapDocTheme {
        LibraryContent(
            uiState = LibraryUiState(query = "plumber", isLoading = false),
            onQueryChange = {},
            onDeleteClick = {},
            onShareClick = {},
            onOpenDocument = {}
        )
    }
}
