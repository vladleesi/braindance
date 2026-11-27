// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.poppins_medium
import dev.vladleesi.braindanceapp.resources.poppins_regular
import dev.vladleesi.braindanceapp.resources.poppins_semibold
import org.jetbrains.compose.resources.Font
import org.jetbrains.compose.resources.FontResource

@Composable
fun BraindanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    // The dark palette is the only one so far. A light palette goes here, chosen by [darkTheme], and every component
    // follows it, since they all read the roles in Colors.kt.
    val colors = DarkAppColors
    val shapes =
        Shapes(
            // Material buttons and chips use the small shape; cards, sheets, and dialogs use medium and large.
            small = CornerShapes.pill,
            medium = CornerShapes.large,
            large = CornerShapes.large,
        )
    val fonts = FontFamily(AppFonts.map { font -> Font(font.resource, font.weight) })
    val typography = remember(fonts, colors) { buildTypography(fonts, colors) }

    MaterialTheme(
        colors = remember(colors) { colors.toMaterialColors() },
        typography = typography,
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            values =
                arrayOf(
                    LocalAppColors provides colors,
                    // Text and icons without a color of their own take the theme's.
                    LocalContentColor provides colors.foreground,
                    LocalTextSelectionColors provides remember(colors) { colors.toTextSelectionColors() },
                    // Compose Web focuses a component on every click, so the ripple's focus highlight would stay on
                    // whatever was clicked last. It shows only for keyboard focus instead, like the focus ring.
                    LocalIndication provides AppIndication,
                ),
            content = content,
        )
    }
}

private val AppIndication = FocusVisibleIndication(ripple())

/** A bundled font file and the weight the typography uses it for. */
internal class AppFont(
    val resource: FontResource,
    val weight: FontWeight,
)

/**
 * Only the weights the typography uses: the Web app downloads every listed font before it shows the first screen, and
 * waits for exactly these (see `WebAppHost`).
 */
internal val AppFonts =
    listOf(
        AppFont(Res.font.poppins_regular, FontWeight.Normal),
        AppFont(Res.font.poppins_medium, FontWeight.Medium),
        AppFont(Res.font.poppins_semibold, FontWeight.SemiBold),
    )

/**
 * Compact headings and readable body styles, using only the three bundled Poppins weights. Headings are set tight,
 * like the landing site's.
 */
private fun buildTypography(
    fonts: FontFamily,
    colors: AppColors,
): Typography {
    val heading = TextStyle(color = colors.foreground, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val body = TextStyle(color = colors.foreground, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val label = TextStyle(color = colors.secondaryText, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)

    return Typography(
        defaultFontFamily = fonts,
        h1 = heading.copy(fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-1).sp),
        h2 = heading.copy(fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.7).sp),
        h3 = heading.copy(fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.5).sp),
        h4 = heading.copy(fontSize = 18.sp, lineHeight = 24.sp, letterSpacing = (-0.4).sp),
        h5 = heading.copy(fontSize = 16.sp, lineHeight = 24.sp),
        h6 = heading.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        body1 = body.copy(fontSize = 18.sp, lineHeight = 28.sp),
        body2 = body.copy(fontSize = 16.sp, lineHeight = 24.sp),
        subtitle1 = heading.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        subtitle2 = label.copy(fontSize = 14.sp, lineHeight = 20.sp),
        caption = label.copy(fontSize = 12.sp, lineHeight = 16.sp),
        button = heading.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        // The smallest label, for footnotes such as the desktop sidebar's links.
        overline = label.copy(fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.2.sp),
    )
}
