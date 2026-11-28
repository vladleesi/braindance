// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material.Colors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Brand colors (docs/branding/README.md): the same in any theme, for the logo, its trail, and brand artwork such as
// the score rings and the avatar. The brand is dark only.
val void = Color(0xFF0A0D14)
val neonCyan = Color(0xFF22E4FF)
val hotMagenta = Color(0xFFFF2E88)
val signalYellow = Color(0xFFFFEE0A)
val slate = Color(0xFF202A45)
val ghost = Color(0xFFF4F6FA)

// What sits on photos and video, which stay dark in any theme: white content and the dark scrims and frames under it.
val onMedia = ghost
val black = Color(0xFF000000)
val overlayWhiteWith80Alpha = Color(0xCCFFFFFF)
val overlayBlackWith70Alpha = Color(0xB2000000)
val overlayBlackWith80Alpha = Color(0xCC000000)

private const val SELECTION_BACKGROUND_ALPHA = 0.45f
private val DarkRaisedControl = Color(0xFF262C39)
private val LightSecondaryText = Color(0xFF4A5361)

/**
 * A theme's color roles. Components read them through the role properties below, never as fixed colors, so a light
 * palette can replace the dark one without changes to any component. Only brand artwork and what sits on photos and
 * video use the fixed colors above.
 *
 * The low-chroma neutrals distinguish [navBar] (chrome), [background] (canvas), [surface] (cards and panels),
 * [secondary] (chips, inputs, selected rows), and [secondaryVariant] (tonal buttons). Navy (slate) stays a brand
 * color: tinting surfaces with it gave the
 * whole interface a blue cast that competed with cover art. [foreground] is text and icons on all of them, and
 * [secondaryText] the quieter text. [accent] is the one functional accent, for selection, focus, and links. The
 * status colors tint game status tags and badges.
 */
@Immutable
@Suppress("LongParameterList")
class AppColors(
    val isLight: Boolean,
    val navBar: Color,
    val background: Color,
    val surface: Color,
    val secondary: Color,
    val secondaryVariant: Color,
    val shimmer: Color,
    val divider: Color,
    val foreground: Color,
    val secondaryText: Color,
    val mutedText: Color,
    val disabledText: Color,
    val disabledFill: Color,
    val accent: Color,
    val error: Color,
    val statusYellow: Color,
    val statusCyan: Color,
    val statusMagenta: Color,
    val pinkText: Color,
    val positive: Color,
)

val DarkAppColors =
    AppColors(
        isLight = false,
        navBar = Color(0xFF070A10),
        background = void,
        surface = Color(0xFF12161F),
        secondary = Color(0xFF1B202B),
        secondaryVariant = DarkRaisedControl,
        shimmer = Color(0xFF2C3342),
        divider = DarkRaisedControl,
        foreground = ghost,
        secondaryText = Color(0xFFA9B1C2),
        mutedText = Color(0xFFA9B1C2),
        disabledText = ghost.copy(alpha = 0.38f),
        disabledFill = ghost.copy(alpha = 0.12f),
        accent = neonCyan,
        error = Color(0xFFEA5A4A),
        statusYellow = signalYellow,
        statusCyan = neonCyan,
        // Hot magenta lightened for text on a hot magenta tint, as in the landing site's "Coming soon" tag.
        statusMagenta = Color(0xFFFF6FA8),
        pinkText = Color(0xFFFF6FA8),
        positive = Color(0xFF4ADE80),
    )

val LightAppColors =
    AppColors(
        isLight = true,
        navBar = Color(0xFFEAEBEE),
        background = Color(0xFFF5F5F6),
        surface = Color.White,
        secondary = Color(0xFFE7E8EC),
        secondaryVariant = Color(0xFFD9DBE0),
        shimmer = Color(0xFFE3E4E8),
        divider = Color(0xFFDEDEDF),
        foreground = void,
        secondaryText = LightSecondaryText,
        mutedText = Color(0xFF5F6876),
        disabledText = Color(0xFF8E9093),
        disabledFill = Color(0xFFE2E2E3),
        accent = LightSecondaryText,
        error = Color(0xFFC4372C),
        statusYellow = LightSecondaryText,
        statusCyan = LightSecondaryText,
        statusMagenta = LightSecondaryText,
        pinkText = Color(0xFFC81E67),
        positive = Color(0xFF157A43),
    )

/**
 * The current theme's colors, which [BraindanceTheme] provides. Static, since the theme changes rarely: reading it
 * costs no tracking, and a change recomposes everything anyway. Outside the theme, as in a bare preview, it is dark.
 */
val LocalAppColors = staticCompositionLocalOf { DarkAppColors }

// The roles, read in composition. Code that draws or builds brushes outside composition reads them first and passes
// the colors in, or reads LocalAppColors from a Modifier.Node.
val navBar: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.navBar
val background: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.background
val surface: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.surface
val secondary: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.secondary
val secondaryVariant: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.secondaryVariant
val shimmer: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.shimmer
val divider: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.divider
val foreground: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.foreground
val secondaryText: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.secondaryText
val mutedText: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.mutedText
val disabledText: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.disabledText
val disabledFill: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.disabledFill
val accent: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.accent
val error: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.error
val statusYellow: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.statusYellow
val statusCyan: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.statusCyan
val statusMagenta: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.statusMagenta
val pinkText: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.pinkText
val positive: Color
    @Composable @ReadOnlyComposable
    get() = LocalAppColors.current.positive

/** Material's colors from the roles, for the Material components the app uses. */
internal fun AppColors.toMaterialColors(): Colors =
    Colors(
        primary = background,
        primaryVariant = background,
        secondary = secondary,
        secondaryVariant = secondaryVariant,
        // The canvas every screen shows; NavigationScaffold paints it once. The chrome paints navBar itself.
        background = background,
        surface = surface,
        error = error,
        onPrimary = foreground,
        onSecondary = foreground,
        onBackground = foreground,
        onSurface = foreground,
        onError = surface,
        isLight = isLight,
    )

internal fun AppColors.toTextSelectionColors(): TextSelectionColors =
    TextSelectionColors(
        handleColor = foreground,
        backgroundColor = foreground.copy(alpha = SELECTION_BACKGROUND_ALPHA),
    )
