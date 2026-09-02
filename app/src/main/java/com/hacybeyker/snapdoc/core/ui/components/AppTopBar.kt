package com.hacybeyker.snapdoc.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.ui.theme.spacing

/**
 * The header every screen was missing. Without one there was no back affordance and no sense of
 * where you were, which is most why the app read as a pile of loose controls.
 *
 * Hand-rolled rather than Material's `TopAppBar` because these screens want an optional subtitle
 * underneath the title — the camera and the reader both have something short to say about state, and
 * squeezing it into a one-line bar is what produced the unexplained text the redesign is fixing.
 */
@Composable
fun AppTopBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    onBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = MIN_HEIGHT)
            .padding(horizontal = MaterialTheme.spacing.xs, vertical = MaterialTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.action_back)
                )
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = if (onBack == null) MaterialTheme.spacing.md else MaterialTheme.spacing.xs)
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        actions()
    }
}

/** Matches Material's own bar height, so the layout still feels standard even though this is not one. */
private val MIN_HEIGHT = 56.dp
