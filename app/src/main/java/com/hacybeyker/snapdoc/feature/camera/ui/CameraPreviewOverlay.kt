package com.hacybeyker.snapdoc.feature.camera.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer as LayoutSpacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.components.HorizontalSpacer
import com.hacybeyker.snapdoc.core.ui.components.LabelChip
import com.hacybeyker.snapdoc.core.ui.components.Spacer
import com.hacybeyker.snapdoc.core.ui.theme.CameraOnScrim
import com.hacybeyker.snapdoc.core.ui.theme.CameraOnScrimMuted
import com.hacybeyker.snapdoc.core.ui.theme.CameraScrim
import com.hacybeyker.snapdoc.core.ui.theme.spacing
import com.hacybeyker.snapdoc.feature.camera.domain.LiveTextHint

/**
 * The way out. The viewfinder is the one screen with no header, so without this the only way back was
 * the system gesture — and on the states where the camera never appears there was nothing at all.
 * It carries its own scrim because it sits on the feed, where a bare white glyph over a lit page is
 * invisible.
 */
@Composable
internal fun CameraBackButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    FilledIconButton(
        onClick = onClick,
        colors = IconButtonDefaults.filledIconButtonColors(
            containerColor = CameraScrim,
            contentColor = CameraOnScrim
        ),
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = stringResource(R.string.action_back)
        )
    }
}

/**
 * The live readout, kept at the top and away from the controls. It used to sit inline with a
 * "Turn off" button, which made a status message look like a setting.
 */
@Composable
internal fun LiveTextBar(
    uiState: CameraPreviewUiState.Ready,
    onBackClick: () -> Unit,
    onToggleClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hint = uiState.liveTextHint
    val seesText = hint is LiveTextHint.TextVisible
    val label = when {
        !uiState.isLiveAnalysisEnabled -> stringResource(R.string.camera_live_off)
        hint is LiveTextHint.NoTextVisible -> stringResource(R.string.camera_live_no_text)
        hint is LiveTextHint.TextVisible ->
            pluralStringResource(R.plurals.camera_live_text_visible, hint.blockCount, hint.blockCount)

        else -> stringResource(R.string.camera_live_searching)
    }
    Row(
        modifier = modifier
            .background(Brush.verticalGradient(listOf(CameraScrim, Color.Transparent)))
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Top))
            .padding(MaterialTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CameraBackButton(onClick = onBackClick)
        HorizontalSpacer(MaterialTheme.spacing.sm)
        LabelChip(
            text = label,
            icon = if (seesText) Icons.Filled.CheckCircle else Icons.Filled.Search,
            container = CameraScrim,
            content = CameraOnScrim,
            // The label swaps between four lengths as the recognizer reports; without this the chip
            // snaps to a new width on every frame.
            modifier = Modifier.animateContentSize()
        )
        LayoutSpacer(Modifier.weight(1f))
        LiveTextToggle(isEnabled = uiState.isLiveAnalysisEnabled, onToggleClick = onToggleClick)
    }
}

/**
 * A toggle that looks like one. This was a bare `Info` glyph whose only "off" signal was a colour
 * change on a white icon over a white page — unreadable, and it read as help rather than a switch.
 * Filled when on, scrim-backed when off, and a real toggle for the accessibility tree.
 */
@Composable
private fun LiveTextToggle(isEnabled: Boolean, onToggleClick: () -> Unit) {
    FilledIconToggleButton(
        checked = isEnabled,
        onCheckedChange = { onToggleClick() },
        colors = IconButtonDefaults.filledIconToggleButtonColors(
            containerColor = CameraScrim,
            contentColor = CameraOnScrimMuted,
            checkedContainerColor = MaterialTheme.colorScheme.primary,
            checkedContentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Icon(
            imageVector = Icons.Filled.Search,
            contentDescription = stringResource(
                if (isEnabled) R.string.camera_live_turn_off else R.string.camera_live_turn_on
            )
        )
    }
}

/**
 * The two ways of getting a page, told apart by weight. The guided scanner is almost always the right
 * answer, so it is the only filled button; the caption sits above it because between the two buttons
 * it was ambiguous which one it described.
 *
 * There is no "read the pages" action here any more: a finished capture leaves for the reader on its
 * own, so an action offering to do what just happened would never be seen.
 */
@Composable
internal fun CameraControls(
    uiState: CameraPreviewUiState.Ready,
    onCaptureClick: () -> Unit,
    onScanClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBusy = uiState.isScanning || uiState.isCapturing
    Column(
        modifier = modifier
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    CONTROLS_FADE_STOP to CameraScrim,
                    1f to CameraScrim
                )
            )
            // The panel grows and shrinks as errors come and go.
            .animateContentSize()
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal + WindowInsetsSides.Bottom))
            .padding(horizontal = MaterialTheme.spacing.lg)
            .padding(top = MaterialTheme.spacing.xl, bottom = MaterialTheme.spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        uiState.captureError?.let { error -> CaptureError(error = error) }
        Text(
            text = stringResource(R.string.camera_scan_caption),
            style = MaterialTheme.typography.bodySmall,
            color = CameraOnScrimMuted,
            textAlign = TextAlign.Center
        )
        Spacer(MaterialTheme.spacing.sm)
        Button(
            onClick = onScanClick,
            enabled = !isBusy,
            colors = ButtonDefaults.buttonColors(
                // The default disabled grey is nearly invisible over a bright page, and the button is
                // disabled for as long as the scanner is open.
                disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = DISABLED_ALPHA),
                disabledContentColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = DISABLED_ALPHA)
            ),
            modifier = Modifier.fillMaxWidth().height(PRIMARY_HEIGHT)
        ) {
            if (uiState.isScanning) {
                BusyIndicator(color = MaterialTheme.colorScheme.onPrimary)
                HorizontalSpacer(MaterialTheme.spacing.sm)
            }
            Text(text = stringResource(R.string.camera_scan_action), style = MaterialTheme.typography.titleMedium)
        }
        Spacer(MaterialTheme.spacing.xs)
        TextButton(
            onClick = onCaptureClick,
            enabled = !isBusy,
            colors = ButtonDefaults.textButtonColors(
                contentColor = CameraOnScrim,
                disabledContentColor = CameraOnScrimMuted
            )
        ) {
            if (uiState.isCapturing) {
                BusyIndicator(color = CameraOnScrim)
                HorizontalSpacer(MaterialTheme.spacing.sm)
            }
            Text(text = stringResource(R.string.camera_photo_action))
        }
    }
}

/** Both capture paths take a visible moment, and a button that only greys out reads as broken. */
@Composable
private fun BusyIndicator(color: Color) {
    CircularProgressIndicator(
        color = color,
        strokeWidth = BUSY_STROKE,
        modifier = Modifier.size(BUSY_INDICATOR)
    )
}

/**
 * On its own container, because white body text on the scrim did not read as a failure at all. This
 * is the one thing left on the panel that outlives a capture: everything that succeeds leaves the
 * screen, so what stays behind is only ever what went wrong.
 */
@Composable
private fun CaptureError(error: CameraPreviewUiState.CaptureError) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(MaterialTheme.spacing.md),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = Icons.Filled.Warning, contentDescription = null)
            HorizontalSpacer(MaterialTheme.spacing.sm)
            Text(
                text = stringResource(
                    when (error) {
                        CameraPreviewUiState.CaptureError.Camera -> R.string.camera_preview_capture_failed
                        CameraPreviewUiState.CaptureError.Storage -> R.string.camera_preview_save_failed
                        CameraPreviewUiState.CaptureError.Scanner -> R.string.camera_preview_scanner_unavailable
                    }
                ),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
    Spacer(MaterialTheme.spacing.md)
}

private val PRIMARY_HEIGHT = 56.dp

private val BUSY_INDICATOR = 20.dp

private val BUSY_STROKE = 2.dp

private const val DISABLED_ALPHA = 0.7f

private const val CONTROLS_FADE_STOP = 0.45f
