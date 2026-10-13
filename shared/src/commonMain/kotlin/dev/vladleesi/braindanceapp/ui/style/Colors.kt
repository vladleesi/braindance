// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val white = Color(0xFFEFEFEF)
val overlayWhiteWith40Alpha = Color(0x66FFFFFF)
val overlayWhiteWith45Alpha = Color(0x73FFFFFF)
val overlayWhiteWith50Alpha = Color(0x80FFFFFF)
val overlayWhiteWith60Alpha = Color(0x99FFFFFF)
val overlayWhiteWith70Alpha = Color(0xB2FFFFFF)
val overlayWhiteWith80Alpha = Color(0xCCFFFFFF)
val overlayWhiteWith90Alpha = Color(0xE6FFFFFF)
val overlayWhiteWith95Alpha = Color(0xF2FFFFFF)
val overlayBlackWith50Alpha = Color(0x80000000)
val overlayBlackWith60Alpha = Color(0x99000000)
val overlayBlackWith70Alpha = Color(0xB2000000)
val overlayBlackWith80Alpha = Color(0xCC000000)
val overlayBlackWith90Alpha = Color(0xE6000000)
val overlayBlackWith95Alpha = Color(0xF2000000)
val background = Color(0xFF171717)
val secondaryText = Color(0xFF969696)
val hintText = Color(0xFF363638)
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

// TODO: Needs to implement?
val LightColors = lightColors()
