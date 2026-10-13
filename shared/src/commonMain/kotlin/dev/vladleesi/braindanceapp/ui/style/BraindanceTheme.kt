// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Shapes
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.poppins_bold
import dev.vladleesi.braindanceapp.resources.poppins_light
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
            small = RoundedCornerShape(Dimens.tiny),
            medium = RoundedCornerShape(Dimens.small),
            large = RoundedCornerShape(Dimens.compact),
        )
    val fonts =
        FontFamily(
            Font(Res.font.poppins_light, FontWeight.Light),
            Font(Res.font.poppins_regular, FontWeight.Normal),
            Font(Res.font.poppins_medium, FontWeight.Medium),
            Font(Res.font.poppins_semibold, FontWeight.SemiBold),
            Font(Res.font.poppins_bold, FontWeight.Bold),
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
                ),
            content = content,
        )
    }
}

@Composable
private fun buildTypography(fonts: FontFamily): Typography =
    Typography(
        h1 =
            TextStyle(
                color = white,
                fontFamily = fonts,
                fontWeight = FontWeight.SemiBold,
                fontSize = 32.sp,
            ),
        h2 =
            TextStyle(
                color = white,
                fontFamily = fonts,
                fontWeight = FontWeight.SemiBold,
                fontSize = 22.sp,
            ),
        h3 =
            TextStyle(
                color = white,
                fontFamily = fonts,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
            ),
        body1 =
            TextStyle(
                color = white,
                fontFamily = fonts,
                fontWeight = FontWeight.Normal,
                fontSize = 18.sp,
            ),
        body2 =
            TextStyle(
                color = secondaryText,
                fontFamily = fonts,
                fontWeight = FontWeight.Medium,
                fontSize = 16.sp,
            ),
        subtitle1 =
            TextStyle(
                color = white,
                fontFamily = fonts,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
        subtitle2 =
            TextStyle(
                color = secondaryText,
                fontFamily = fonts,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            ),
        caption =
            TextStyle(
                color = secondaryText,
                fontFamily = fonts,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            ),
        button =
            TextStyle(
                color = white,
                fontFamily = fonts,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            ),
    )
