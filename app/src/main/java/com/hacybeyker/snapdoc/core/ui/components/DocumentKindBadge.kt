package com.hacybeyker.snapdoc.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hacybeyker.snapdoc.R
import com.hacybeyker.snapdoc.core.document.DocumentKind

/**
 * The visual anchor of every document row. A list of cards whose only difference is their wording
 * has to be read; the same list with a tinted glyph per kind can be scanned, which is the point of
 * an archive.
 *
 * Colour never carries the meaning on its own — the badge always sits beside the kind's written
 * label, so it stays legible to anyone who cannot tell the tints apart.
 */
@Composable
fun DocumentKindBadge(kind: DocumentKind, modifier: Modifier = Modifier, size: Dp = DEFAULT_SIZE) {
    val scheme = MaterialTheme.colorScheme
    val (container, content) = when (kind) {
        DocumentKind.Receipt -> scheme.secondaryContainer to scheme.onSecondaryContainer
        DocumentKind.Invoice -> scheme.primaryContainer to scheme.onPrimaryContainer
        DocumentKind.IdDocument -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        DocumentKind.Note, DocumentKind.Unknown -> scheme.surfaceVariant to scheme.onSurfaceVariant
    }
    Box(
        modifier = modifier
            .size(size)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = kind.icon,
            contentDescription = null,
            tint = content,
            modifier = Modifier.size(size / 2)
        )
    }
}

/**
 * Only metaphors that actually hold: a cart for a shop receipt, line items for an invoice, a person
 * for an identity document, a pen for a note. Anything unrecognized gets a neutral mark rather than
 * a forced one — a wrong icon is a confident lie about what the document is.
 */
private val DocumentKind.icon: ImageVector
    get() = when (this) {
        DocumentKind.Receipt -> Icons.Filled.ShoppingCart
        DocumentKind.Invoice -> Icons.AutoMirrored.Filled.List
        DocumentKind.IdDocument -> Icons.Filled.Person
        DocumentKind.Note -> Icons.Filled.Create
        DocumentKind.Unknown -> Icons.Filled.Info
    }

private val DEFAULT_SIZE = 44.dp

/**
 * Lives beside the badge because it is the same job — turning a [DocumentKind] into something a
 * person sees. Three screens were each keeping their own copy of this `when`.
 */
val DocumentKind.labelRes: Int
    get() = when (this) {
        DocumentKind.Receipt -> R.string.document_kind_receipt
        DocumentKind.Invoice -> R.string.document_kind_invoice
        DocumentKind.IdDocument -> R.string.document_kind_id
        DocumentKind.Note -> R.string.document_kind_note
        DocumentKind.Unknown -> R.string.document_kind_unknown
    }
