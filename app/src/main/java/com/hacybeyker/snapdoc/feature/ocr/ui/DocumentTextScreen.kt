package com.hacybeyker.snapdoc.feature.ocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.document.DocumentInsight
import com.hacybeyker.snapdoc.core.document.DocumentKind
import com.hacybeyker.snapdoc.core.document.InsightSource
import com.hacybeyker.snapdoc.core.ui.components.AppTopBar
import com.hacybeyker.snapdoc.core.ui.components.EmptyState
import com.hacybeyker.snapdoc.core.ui.components.HorizontalSpacer
import com.hacybeyker.snapdoc.core.ui.components.Spacer
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizedDocument
import com.hacybeyker.snapdoc.feature.ocr.domain.RecognizedPage

@Composable
fun DocumentTextScreen(
    imagePaths: List<String>,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DocumentTextViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    // The pages travel in the navigation key, which Navigation 3 gives to the Screen, not to the
    // ViewModel; the ViewModel ignores a repeat of the same pages so a restart re-reads nothing.
    LaunchedEffect(imagePaths) {
        viewModel.onIntent(DocumentTextIntent.RecognizeText(imagePaths))
    }

    DocumentTextEffects(viewModel = viewModel, snackbarHostState = snackbarHostState)

    DocumentTextContent(
        uiState = uiState,
        onCopyClick = { viewModel.onIntent(DocumentTextIntent.CopyText) },
        onRetryClick = { viewModel.onIntent(DocumentTextIntent.Retry) },
        onBackClick = onBack,
        onEnableModelClick = { viewModel.onIntent(DocumentTextIntent.EnableOnDeviceModel) },
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

@Composable
private fun DocumentTextEffects(viewModel: DocumentTextViewModel, snackbarHostState: SnackbarHostState) {
    val context = LocalContext.current
    val clipLabel = stringResource(R.string.document_text_clip_label)
    val copiedMessage = stringResource(R.string.document_text_copied)

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is DocumentTextEffect.CopyToClipboard -> {
                    context.getSystemService(ClipboardManager::class.java)
                        ?.setPrimaryClip(ClipData.newPlainText(clipLabel, effect.text))
                    // Android 13 onwards the system shows its own "copied" popup; ours would double it.
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                        snackbarHostState.showSnackbar(copiedMessage)
                    }
                }
            }
        }
    }
}

@Composable
private fun DocumentTextContent(
    uiState: DocumentTextUiState,
    onCopyClick: () -> Unit,
    onRetryClick: () -> Unit,
    onBackClick: () -> Unit,
    onEnableModelClick: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    val pageCount = (uiState as? DocumentTextUiState.Content)?.document?.pages?.size
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AppTopBar(
                title = stringResource(R.string.document_text_title),
                subtitle = pageCount?.let { pluralStringResource(R.plurals.library_page_count, it, it) },
                onBack = onBackClick
            )
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                when (uiState) {
                    DocumentTextUiState.Recognizing -> CircularProgressIndicator()

                    is DocumentTextUiState.Content -> RecognizedTextPages(
                        uiState = uiState,
                        onCopyClick = onCopyClick,
                        onEnableModelClick = onEnableModelClick
                    )

                    DocumentTextUiState.Empty -> EmptyState(
                        icon = Icons.Filled.Info,
                        title = stringResource(R.string.document_text_empty_title),
                        message = stringResource(R.string.document_text_empty),
                        actionLabel = stringResource(R.string.document_text_back),
                        onAction = onBackClick
                    )

                    DocumentTextUiState.Error -> EmptyState(
                        icon = Icons.Filled.Warning,
                        title = stringResource(R.string.document_text_failed_title),
                        message = stringResource(R.string.document_text_failed),
                        actionLabel = stringResource(R.string.document_text_retry),
                        onAction = onRetryClick
                    )
                }
            }
        }
    }
}

@Composable
private fun RecognizedTextPages(
    uiState: DocumentTextUiState.Content,
    onCopyClick: () -> Unit,
    onEnableModelClick: () -> Unit
) {
    val document = uiState.document
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = MaterialTheme.spacing.md)) {
        Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            InsightCard(insight = uiState.insight)
            ModelStatusRow(modelStatus = uiState.modelStatus, onEnableModelClick = onEnableModelClick)
            Spacer(MaterialTheme.spacing.md)
            Card(modifier = Modifier.fillMaxWidth()) {
                // Selectable so the user can lift a single line out without copying the whole document.
                SelectionContainer {
                    Column(modifier = Modifier.padding(MaterialTheme.spacing.md)) {
                        document.pages.filterNot { it.isEmpty }.forEach { page ->
                            PageText(page = page, showHeader = document.pages.size > 1)
                        }
                    }
                }
            }
            Spacer(MaterialTheme.spacing.md)
        }
        Button(
            onClick = onCopyClick,
            modifier = Modifier.fillMaxWidth().padding(bottom = MaterialTheme.spacing.md)
        ) {
            Icon(imageVector = Icons.Filled.Send, contentDescription = null)
            HorizontalSpacer(MaterialTheme.spacing.sm)
            Text(text = stringResource(R.string.document_text_copy))
        }
    }
}

@Composable
private fun PageText(page: RecognizedPage, showHeader: Boolean) {
    if (showHeader) {
        Text(
            text = stringResource(R.string.document_text_page, page.pageNumber),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(MaterialTheme.spacing.xs)
    }
    Text(text = page.text, style = MaterialTheme.typography.bodyMedium)
    Spacer(MaterialTheme.spacing.md)
}

private val previewDocument = RecognizedDocument(
    pages = listOf(
        RecognizedPage(pageNumber = 1, blocks = listOf("HARDWARE STORE", "Hammer  12.90\nNails   3.40", "TOTAL 16.30"))
    )
)

private val previewInsight = DocumentInsight(
    kind = DocumentKind.Receipt,
    merchant = "Hardware Store",
    date = "2026-08-20",
    total = "16.30",
    source = InsightSource.OnDeviceModel
)

@Preview(showBackground = true)
@Composable
private fun DocumentTextContentPreview() {
    SnapDocTheme {
        DocumentTextContent(
            uiState = DocumentTextUiState.Content(previewDocument, previewInsight),
            onCopyClick = {},
            onRetryClick = {},
            onBackClick = {},
            onEnableModelClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentTextContentEmptyPreview() {
    SnapDocTheme {
        DocumentTextContent(
            uiState = DocumentTextUiState.Empty,
            onCopyClick = {},
            onRetryClick = {},
            onBackClick = {},
            onEnableModelClick = {}
        )
    }
}
