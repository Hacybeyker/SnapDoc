package com.hacybeyker.snapdoc.feature.camera.ui

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme
import com.hacybeyker.snapdoc.core.ui.theme.spacing

@Composable
fun CameraPermissionScreen(
    modifier: Modifier = Modifier,
    onExtractText: (List<String>) -> Unit = {},
    viewModel: CameraPermissionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        val shouldShowRationale =
            activity?.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA) == true
        viewModel.onIntent(CameraPermissionIntent.PermissionResultReceived(isGranted, shouldShowRationale))
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.onIntent(CameraPermissionIntent.ScreenResumed)
    }

    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CameraPermissionEffect.LaunchPermissionRequest ->
                    permissionLauncher.launch(Manifest.permission.CAMERA)

                CameraPermissionEffect.OpenAppSettings -> {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                }
            }
        }
    }

    CameraPermissionContent(
        uiState = uiState,
        onRequestPermission = { viewModel.onIntent(CameraPermissionIntent.RequestPermission) },
        onOpenSettings = { viewModel.onIntent(CameraPermissionIntent.OpenAppSettings) },
        grantedContent = { CameraPreviewScreen(onExtractText = onExtractText) },
        modifier = modifier
    )
}

@Composable
private fun CameraPermissionContent(
    uiState: CameraPermissionUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    grantedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when (uiState) {
            CameraPermissionUiState.Checking ->
                CircularProgressIndicator()

            // A slot, so this Content stays stateless and previewable while the real screen
            // plugs in the live camera.
            CameraPermissionUiState.Granted -> grantedContent()

            CameraPermissionUiState.RationaleRequired ->
                PermissionMessage(
                    title = stringResource(R.string.camera_permission_rationale_title),
                    message = stringResource(R.string.camera_permission_rationale_message),
                    buttonLabel = stringResource(R.string.camera_permission_allow),
                    onButtonClick = onRequestPermission
                )

            CameraPermissionUiState.PermanentlyDenied ->
                PermissionMessage(
                    title = stringResource(R.string.camera_permission_denied_title),
                    message = stringResource(R.string.camera_permission_denied_message),
                    buttonLabel = stringResource(R.string.camera_permission_open_settings),
                    onButtonClick = onOpenSettings
                )
        }
    }
}

@Composable
private fun PermissionMessage(title: String, message: String, buttonLabel: String, onButtonClick: () -> Unit) {
    Column(
        modifier = Modifier.padding(MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        Spacer(Modifier.height(MaterialTheme.spacing.sm))
        Text(text = message, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(MaterialTheme.spacing.md))
        Button(onClick = onButtonClick) { Text(buttonLabel) }
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPermissionContentRationalePreview() {
    SnapDocTheme {
        CameraPermissionContent(
            uiState = CameraPermissionUiState.RationaleRequired,
            onRequestPermission = {},
            onOpenSettings = {},
            grantedContent = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CameraPermissionContentPermanentlyDeniedPreview() {
    SnapDocTheme {
        CameraPermissionContent(
            uiState = CameraPermissionUiState.PermanentlyDenied,
            onRequestPermission = {},
            onOpenSettings = {},
            grantedContent = {}
        )
    }
}
