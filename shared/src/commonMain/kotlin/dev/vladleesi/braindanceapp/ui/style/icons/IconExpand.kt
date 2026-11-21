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

/** Four corners pointing outward: show larger, such as full screen. */
val Expand: ImageVector by lazy {
    cornersIcon(name = "Expand") {
        moveTo(4f, 9f)
        lineTo(4f, 4f)
        lineTo(9f, 4f)
        moveTo(15f, 4f)
        lineTo(20f, 4f)
        lineTo(20f, 9f)
        moveTo(20f, 15f)
        lineTo(20f, 20f)
        lineTo(15f, 20f)
        moveTo(9f, 20f)
        lineTo(4f, 20f)
        lineTo(4f, 15f)
    }
}

/** Four corners pointing inward: leave full screen. */
val Shrink: ImageVector by lazy {
    cornersIcon(name = "Shrink") {
        moveTo(9f, 4f)
        lineTo(9f, 9f)
        lineTo(4f, 9f)
        moveTo(20f, 9f)
        lineTo(15f, 9f)
        lineTo(15f, 4f)
        moveTo(15f, 20f)
        lineTo(15f, 15f)
        lineTo(20f, 15f)
        moveTo(4f, 15f)
        lineTo(9f, 15f)
        lineTo(9f, 20f)
    }
}

private fun cornersIcon(
    name: String,
    corners: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
                pathBuilder = corners,
            )
        }.build()
