// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.poppins_medium
import dev.vladleesi.braindanceapp.resources.poppins_regular
import dev.vladleesi.braindanceapp.resources.poppins_semibold
import org.jetbrains.compose.resources.Font

@Composable
fun BraindanceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    if (!darkTheme) {
        // ignore by now
    }
    val shapes =
        Shapes(
            small = CornerShapes.tiny,
            medium = CornerShapes.small,
            large = CornerShapes.compact,
        )
    // Only the weights the typography uses: the Web app downloads every listed font before it can show text.
    val fonts =
        FontFamily(
            Font(Res.font.poppins_regular, FontWeight.Normal),
            Font(Res.font.poppins_medium, FontWeight.Medium),
            Font(Res.font.poppins_semibold, FontWeight.SemiBold),
        )

    MaterialTheme(
        colors = DarkColors,
        typography = buildTypography(fonts),
        shapes = shapes,
    ) {
        CompositionLocalProvider(
            values =
                arrayOf(
                    LocalTextSelectionColors provides DarkTextSelectionColors,
                    // Compose Web focuses a component on every click, so the ripple's focus highlight would stay on
                    // whatever was clicked last. It shows only for keyboard focus instead, like the focus ring.
                    LocalIndication provides AppIndication,
                ),
            content = content,
        )
    }
}

private val AppIndication = FocusVisibleIndication(ripple())

/** Compact headings and readable body styles, using only the three bundled Poppins weights. */
private fun buildTypography(fonts: FontFamily): Typography {
    val heading = TextStyle(color = white, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp)
    val body = TextStyle(color = white, fontWeight = FontWeight.Normal, letterSpacing = 0.sp)
    val label = TextStyle(color = secondaryText, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp)

    return Typography(
        defaultFontFamily = fonts,
        h1 = heading.copy(fontSize = 32.sp, lineHeight = 40.sp, letterSpacing = (-0.5).sp),
        h2 = heading.copy(fontSize = 24.sp, lineHeight = 32.sp, letterSpacing = (-0.25).sp),
        h3 = heading.copy(fontSize = 20.sp, lineHeight = 28.sp),
        h4 = heading.copy(fontSize = 18.sp, lineHeight = 24.sp),
        h5 = heading.copy(fontSize = 16.sp, lineHeight = 24.sp),
        h6 = heading.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        body1 = body.copy(fontSize = 18.sp, lineHeight = 28.sp),
        body2 = body.copy(fontSize = 16.sp, lineHeight = 24.sp),
        subtitle1 = heading.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        subtitle2 = label.copy(fontSize = 14.sp, lineHeight = 20.sp),
        caption = label.copy(fontSize = 12.sp, lineHeight = 16.sp),
        button = heading.copy(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
        overline = label.copy(fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    )
}
