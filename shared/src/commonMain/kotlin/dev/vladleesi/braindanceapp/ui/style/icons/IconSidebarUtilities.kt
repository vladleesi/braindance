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

internal val SidebarSun: ImageVector by lazy {
    sidebarUtilityIcon("SidebarSun") {
        moveTo(12f, 8f)
        arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 16f)
        arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 12f, y1 = 8f)
        close()
        moveTo(12f, 2f)
        lineTo(12f, 4f)
        moveTo(12f, 20f)
        lineTo(12f, 22f)
        moveTo(2f, 12f)
        lineTo(4f, 12f)
        moveTo(20f, 12f)
        lineTo(22f, 12f)
        moveTo(5f, 5f)
        lineTo(6.5f, 6.5f)
        moveTo(17.5f, 17.5f)
        lineTo(19f, 19f)
        moveTo(5f, 19f)
        lineTo(6.5f, 17.5f)
        moveTo(17.5f, 6.5f)
        lineTo(19f, 5f)
    }
}

internal val SidebarMonitor: ImageVector by lazy {
    sidebarUtilityIcon("SidebarMonitor") {
        moveTo(4f, 4f)
        horizontalLineTo(20f)
        quadTo(22f, 4f, 22f, 6f)
        verticalLineTo(15f)
        quadTo(22f, 17f, 20f, 17f)
        horizontalLineTo(4f)
        quadTo(2f, 17f, 2f, 15f)
        verticalLineTo(6f)
        quadTo(2f, 4f, 4f, 4f)
        close()
        moveTo(12f, 17f)
        verticalLineTo(21f)
        moveTo(8f, 21f)
        horizontalLineTo(16f)
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
