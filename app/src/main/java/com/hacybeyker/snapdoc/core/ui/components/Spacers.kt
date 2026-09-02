package com.hacybeyker.snapdoc.core.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

/**
 * `Spacer(MaterialTheme.spacing.md)` instead of `Spacer(Modifier.height(MaterialTheme.spacing.md))`.
 * The layouts here are mostly stacks of card, gap, card, and the longer form was drowning them.
 */
@Composable
fun Spacer(height: Dp) = Spacer(Modifier.height(height))

@Composable
fun HorizontalSpacer(width: Dp) = Spacer(Modifier.width(width))
