// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

internal val SidebarMoon: ImageVector by lazy {
    sidebarUtilityIcon("SidebarMoon") {
        moveTo(12f, 3f)
        curveTo(7f, 3f, 3f, 7f, 3f, 12f)
        curveTo(3f, 17f, 7f, 21f, 12f, 21f)
        curveTo(16.4f, 21f, 20f, 17.9f, 20.8f, 13.8f)
        curveTo(14.3f, 16.2f, 8f, 9.6f, 12f, 3f)
        close()
    }
}

internal val SidebarHelp: ImageVector by lazy {
    sidebarUtilityIcon("SidebarHelp") {
        moveTo(12f, 3f)
        arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 21f)
        arcTo(9f, 9f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 3f)
        close()
        moveTo(9.5f, 9.3f)
        curveTo(10.1f, 6.3f, 15.1f, 6.5f, 15.1f, 9.4f)
        curveTo(15.1f, 11.5f, 12.2f, 11.4f, 12.2f, 13.1f)
        moveTo(12.2f, 16.3f)
        lineTo(12.2f, 16.5f)
    }
}

private fun sidebarUtilityIcon(
    name: String,
    outline: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(name, defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .apply {
            path(
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = outline,
            )
        }.build()
