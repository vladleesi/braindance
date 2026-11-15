// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

// Brand colors (docs/branding/README.md) are available independently of the app theme. The brand is dark only.
val void = Color(0xFF0A0D14)
val neonCyan = Color(0xFF22E4FF)
val hotMagenta = Color(0xFFFF2E88)

// Hot magenta lightened for text on a hot magenta tint, as in the landing site's "Coming soon" tag.
val hotMagentaLight = Color(0xFFFF6FA8)
val signalYellow = Color(0xFFFFEE0A)
val statusGreen = Color(0xFF4ADE80)
val slate = Color(0xFF202A45)
val ghost = Color(0xFFF4F6FA)

val white = Color(0xFFF4F6FA)
val black = Color(0xFF000000)
val overlayWhiteWith45Alpha = Color(0x73FFFFFF)
val overlayWhiteWith80Alpha = Color(0xCCFFFFFF)
val overlayBlackWith70Alpha = Color(0xB2000000)
val overlayBlackWith80Alpha = Color(0xCC000000)

// Functional neutrals: a low-chroma ramp built from Ink, one step per role. Navy (slate) stays a brand color for
// background symbols; tinting surfaces with it gave the whole interface a blue cast that competed with cover art.
// navBar (chrome: sidebar, bottom bar) < background (canvas) < surface (cards, panels, tiles)
// < secondary (controls: chips, inputs, selected rows) < secondaryVariant (tonal buttons, outlines).
val navBar = Color(0xFF070A10)
val background = void
val surface = Color(0xFF12161F)
val secondary = Color(0xFF1B202B)
val secondaryVariant = Color(0xFF262C39)
val shimmer = Color(0xFF2C3342)
val secondaryText = Color(0xFFA9B1C2)
val error = Color(0xFFEA5A4A)

// The one functional accent: selection, focus, and links. Magenta and yellow join it only in the Echo trail, and
// magenta alone marks the development status.
val accent = neonCyan

val DarkColors =
    darkColors(
        primary = background,
        secondary = secondary,
        secondaryVariant = secondaryVariant,
        background = navBar,
        surface = surface,
        error = error,
    )

val DarkTextSelectionColors =
    TextSelectionColors(
        handleColor = white,
        backgroundColor = overlayWhiteWith45Alpha,
    )
