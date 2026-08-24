package com.hacybeyker.snapdoc.feature.ocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.os.Build
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
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
    Scaffold(modifier = modifier, snackbarHost = { SnackbarHost(snackbarHostState) }) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize().padding(innerPadding).padding(MaterialTheme.spacing.lg),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                DocumentTextUiState.Recognizing -> CircularProgressIndicator()

                is DocumentTextUiState.Content -> RecognizedTextPages(
                    uiState = uiState,
                    onCopyClick = onCopyClick,
                    onEnableModelClick = onEnableModelClick
                )

                DocumentTextUiState.Empty -> MessageWithAction(
                    message = stringResource(R.string.document_text_empty),
                    actionLabel = stringResource(R.string.document_text_back),
                    onActionClick = onBackClick
                )

                DocumentTextUiState.Error -> MessageWithAction(
                    message = stringResource(R.string.document_text_failed),
                    actionLabel = stringResource(R.string.document_text_retry),
                    onActionClick = onRetryClick
                )
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
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.document_text_title),
            style = MaterialTheme.typography.headlineSmall
        )
        Spacer(Modifier.height(MaterialTheme.spacing.md))
        // Selectable so the user can lift a single line out without copying the whole document.
        SelectionContainer(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            Column {
                InsightCard(insight = uiState.insight)
                ModelStatusRow(modelStatus = uiState.modelStatus, onEnableModelClick = onEnableModelClick)
                Spacer(Modifier.height(MaterialTheme.spacing.md))
                document.pages.filterNot { it.isEmpty }.forEach { page ->
                    PageText(page = page, showHeader = document.pages.size > 1)
                }
            }
        }
        Spacer(Modifier.height(MaterialTheme.spacing.md))
        Button(onClick = onCopyClick, modifier = Modifier.fillMaxWidth()) {
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
        Spacer(Modifier.height(MaterialTheme.spacing.xs))
    }
    Text(text = page.text, style = MaterialTheme.typography.bodyMedium)
    Spacer(Modifier.height(MaterialTheme.spacing.md))
}

@Composable
private fun MessageWithAction(message: String, actionLabel: String, onActionClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(MaterialTheme.spacing.md))
        Button(onClick = onActionClick) { Text(text = actionLabel) }
    }
}

private val previewDocument = RecognizedDocument(
    pages = listOf(
        RecognizedPage(pageNumber = 1, blocks = listOf("HARDWARE STORE", "Hammer  12.90\nNails   3.40", "TOTAL 16.30")),
        RecognizedPage(pageNumber = 2, blocks = listOf("Thank you for your purchase"))
    )
)

@Preview(showBackground = true)
@Composable
private fun DocumentTextContentRecognizingPreview() {
    SnapDocTheme {
        DocumentTextContent(
            uiState = DocumentTextUiState.Recognizing,
            onCopyClick = {},
            onRetryClick = {},
            onBackClick = {},
            onEnableModelClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DocumentTextContentPreview() {
    SnapDocTheme {
        DocumentTextContent(
            uiState = DocumentTextUiState.Content(previewDocument),
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

@Preview(showBackground = true)
@Composable
private fun DocumentTextContentErrorPreview() {
    SnapDocTheme {
        DocumentTextContent(
            uiState = DocumentTextUiState.Error,
            onCopyClick = {},
            onRetryClick = {},
            onBackClick = {},
            onEnableModelClick = {}
        )
    }
}
