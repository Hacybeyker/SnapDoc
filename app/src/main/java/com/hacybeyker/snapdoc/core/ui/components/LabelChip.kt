package com.hacybeyker.snapdoc.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.hacybeyker.snapdoc.core.ui.theme.spacing

/**
 * A small, self-contained statement of fact — which engine read a page, how much text is in view,
 * whether a scan has been filed.
 *
 * These were plain sentences before, indistinguishable from the content around them. A chip reads as
 * metadata at a glance, which is exactly what they all are.
 */
@Composable
fun LabelChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    content: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Surface(modifier = modifier, color = container, shape = RoundedCornerShape(CORNER)) {
        Row(
            modifier = Modifier.padding(
                horizontal = MaterialTheme.spacing.sm,
                vertical = MaterialTheme.spacing.xs
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = content,
                    modifier = Modifier.size(ICON)
                )
                HorizontalSpacer(MaterialTheme.spacing.xs)
            }
            Text(text = text, style = MaterialTheme.typography.labelMedium, color = content)
        }
    }
}

private val CORNER = 8.dp
private val ICON = 14.dp
