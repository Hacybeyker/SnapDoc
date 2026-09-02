package com.hacybeyker.snapdoc.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Four corner brackets over the camera feed — the one visual that makes a viewfinder read as a
 * document scanner rather than a plain camera, and the cheapest possible way to tell someone where
 * to put the page. Drawn rather than shipped as an icon: it is eight straight lines.
 *
 * It doubles as the live-analysis readout. The brackets turn from white to the accent colour when
 * the recognizer is steadily seeing text, so the feedback that used to be a sentence nobody could
 * parse is now attached to the thing it describes — the frame you are aiming.
 */
@Composable
fun ScannerFrame(isTextDetected: Boolean, modifier: Modifier = Modifier, accent: Color) {
    val color by animateColorAsState(
        targetValue = if (isTextDetected) accent else Color.White.copy(alpha = INACTIVE_ALPHA),
        label = "scannerFrameColor"
    )
    Canvas(modifier = modifier) {
        val inset = size.minDimension * INSET_FRACTION
        val left = inset
        val top = size.height * VERTICAL_INSET_FRACTION
        val right = size.width - inset
        val bottom = size.height - top
        val arm = size.minDimension * ARM_FRACTION
        val stroke = Stroke(width = STROKE.toPx(), cap = StrokeCap.Round)

        // Each corner is two strokes: one running in from the side, one running down from the top.
        listOf(
            Triple(left, top, 1f to 1f),
            Triple(right, top, -1f to 1f),
            Triple(left, bottom, 1f to -1f),
            Triple(right, bottom, -1f to -1f)
        ).forEach { (x, y, direction) ->
            val (dx, dy) = direction
            drawLine(color, Offset(x, y), Offset(x + arm * dx, y), stroke.width, stroke.cap)
            drawLine(color, Offset(x, y), Offset(x, y + arm * dy), stroke.width, stroke.cap)
        }
    }
}

private const val INACTIVE_ALPHA = 0.75f
private const val INSET_FRACTION = 0.09f

// Small on purpose: the caller sizes this to the camera area left free by the bars, so the
// brackets are already clear of the status chip and the controls panel.
private const val VERTICAL_INSET_FRACTION = 0.07f

private const val ARM_FRACTION = 0.09f
private val STROKE = 3.dp
