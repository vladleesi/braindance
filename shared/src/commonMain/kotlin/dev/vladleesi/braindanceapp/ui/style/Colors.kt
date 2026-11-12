// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.darkColors
import androidx.compose.ui.graphics.Color

// Brand colors are available independently of the app theme.
val void = Color(0xFF0B0E14)
val neonCyan = Color(0xFF22E4FF)
val hotMagenta = Color(0xFFFF2E88)
val signalYellow = Color(0xFFFCEE0A)
val statusGreen = Color(0xFF4ADE80)
val slate = Color(0xFF2E3650)
val ghost = Color(0xFFF4F6FA)

val white = Color(0xFFEFEFEF)
val black = Color(0xFF000000)
val overlayWhiteWith45Alpha = Color(0x73FFFFFF)
val overlayWhiteWith80Alpha = Color(0xCCFFFFFF)
val overlayBlackWith70Alpha = Color(0xB2000000)
val overlayBlackWith80Alpha = Color(0xCC000000)
val background = Color(0xFF171717)
val secondaryText = Color(0xFFA3A3A3)
val secondary = Color(0xFF2F2F2F)
val secondaryVariant = Color(0xFF2D3037)
val error = Color(0xFFEA5A4A)
val accent = Color(0xFF00BFFF) // TODO: Change color
val shimmer = Color(0xFF454545)
val navBar = Color(0xFF101010)

// TODO: Needs to check all colors
val DarkColors =
    darkColors(
        primary = background,
        secondary = secondary,
        secondaryVariant = secondaryVariant,
        background = navBar,
        error = error,
    )

val DarkTextSelectionColors =
    TextSelectionColors(
        handleColor = white,
        backgroundColor = overlayWhiteWith45Alpha,
    )
