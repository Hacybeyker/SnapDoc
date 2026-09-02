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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.components.AppTopBar
import com.hacybeyker.snapdoc.core.ui.components.EmptyState
import com.hacybeyker.snapdoc.core.ui.theme.SnapDocTheme

@Composable
fun CameraPermissionScreen(
    modifier: Modifier = Modifier,
    onBack: () -> Unit = {},
    onPagesReady: (List<String>) -> Unit = {},
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
        onBack = onBack,
        grantedContent = { CameraPreviewScreen(onBack = onBack, onPagesReady = onPagesReady) },
        modifier = modifier
    )
}

@Composable
private fun CameraPermissionContent(
    uiState: CameraPermissionUiState,
    onRequestPermission: () -> Unit,
    onOpenSettings: () -> Unit,
    onBack: () -> Unit,
    grantedContent: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    when (uiState) {
        // A slot, so this Content stays stateless and previewable while the real screen plugs in the
        // live camera. It is also the only state that runs edge to edge: the viewfinder fills the
        // screen and insets its own controls, back button included.
        CameraPermissionUiState.Granted -> grantedContent()

        // Short-lived, but it still gets the header: without it the screen jumped from nothing to a
        // titled page as soon as the system answered.
        CameraPermissionUiState.Checking -> PermissionScreen(onBack = onBack, modifier = modifier) {
            CircularProgressIndicator()
        }

        CameraPermissionUiState.RationaleRequired -> PermissionScreen(onBack = onBack, modifier = modifier) {
            EmptyState(
                icon = Icons.Filled.Lock,
                title = stringResource(R.string.camera_permission_rationale_title),
                message = stringResource(R.string.camera_permission_rationale_message),
                actionLabel = stringResource(R.string.camera_permission_allow),
                onAction = onRequestPermission
            )
        }

        CameraPermissionUiState.PermanentlyDenied -> PermissionScreen(onBack = onBack, modifier = modifier) {
            EmptyState(
                icon = Icons.Filled.Settings,
                title = stringResource(R.string.camera_permission_denied_title),
                message = stringResource(R.string.camera_permission_denied_message),
                actionLabel = stringResource(R.string.camera_permission_open_settings),
                onAction = onOpenSettings
            )
        }
    }
}

/**
 * The states with no viewfinder are ordinary screens, so they are built like every other one: a
 * `Scaffold` for the insets and the app header on top. Denying the permission used to leave a
 * sentence and a button in the middle of the screen with no way out except the system gesture, which
 * is the same trap as a modal with no close button.
 */
@Composable
private fun PermissionScreen(onBack: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Scaffold(modifier = modifier) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            AppTopBar(title = stringResource(R.string.camera_title), onBack = onBack)
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { content() }
        }
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
            onBack = {},
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
            onBack = {},
            grantedContent = {}
        )
    }
}
