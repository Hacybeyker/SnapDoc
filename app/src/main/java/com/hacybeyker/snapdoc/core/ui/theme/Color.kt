package com.hacybeyker.snapdoc.core.ui.theme

import androidx.compose.ui.graphics.Color

/*
 * SnapDoc's own palette, replacing the scaffolding purple.
 *
 * Blue carries the product: it is the color documents and paperwork are filed under, and it stays
 * out of the way of the thing that actually matters on screen — a photograph of a page.
 *
 * Amber is reserved for one job only: anything the on-device model produced. Because the app has to
 * be honest about which engine answered (the rules never fill in a merchant), that distinction earns
 * a color of its own instead of a footnote nobody reads.
 *
 * Green marks a page that has been read and filed. Both are paired with text colors that keep
 * contrast, since meaning is never carried by color alone — every one of them sits next to a label.
 */

// Light
val BluePrimary = Color(0xFF2B5CE0)
val BlueOnPrimary = Color(0xFFFFFFFF)
val BlueContainer = Color(0xFFDCE4FF)
val BlueOnContainer = Color(0xFF001945)

val TealSecondary = Color(0xFF356A6A)
val TealOnSecondary = Color(0xFFFFFFFF)
val TealContainer = Color(0xFFBCEBEA)
val TealOnContainer = Color(0xFF00201F)

val AmberTertiary = Color(0xFF7A5900)
val AmberOnTertiary = Color(0xFFFFFFFF)
val AmberContainer = Color(0xFFFFDEA5)
val AmberOnTertiaryContainer = Color(0xFF261A00)

val SurfaceLight = Color(0xFFFDFBFF)
val OnSurfaceLight = Color(0xFF1A1B21)
val SurfaceVariantLight = Color(0xFFE1E2EC)
val OnSurfaceVariantLight = Color(0xFF44464F)
val OutlineLight = Color(0xFF757780)
val ErrorLight = Color(0xFFBA1A1A)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

// Dark
val BluePrimaryDark = Color(0xFFB3C5FF)
val BlueOnPrimaryDark = Color(0xFF002C71)
val BlueContainerDark = Color(0xFF0F429F)
val BlueOnContainerDark = Color(0xFFDCE4FF)

val TealSecondaryDark = Color(0xFFA0CFCE)
val TealOnSecondaryDark = Color(0xFF003736)
val TealContainerDark = Color(0xFF1C4E4E)
val TealOnContainerDark = Color(0xFFBCEBEA)

val AmberTertiaryDark = Color(0xFFF0C048)
val AmberOnTertiaryDark = Color(0xFF412D00)
val AmberContainerDark = Color(0xFF5D4200)
val AmberOnTertiaryContainerDark = Color(0xFFFFDEA5)

val SurfaceDark = Color(0xFF121318)
val OnSurfaceDark = Color(0xFFE3E1E9)
val SurfaceVariantDark = Color(0xFF44464F)
val OnSurfaceVariantDark = Color(0xFFC5C6D0)
val OutlineDark = Color(0xFF8F909A)
val ErrorDark = Color(0xFFFFB4AB)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

/** Drawn over the camera feed so white controls stay legible against a bright page. */
val CameraScrim = Color(0xB3000000)
val CameraOnScrim = Color(0xFFFFFFFF)

/** Captions and inactive controls over the feed: quieter than the actions, still legible on a page. */
val CameraOnScrimMuted = Color(0xB3FFFFFF)
