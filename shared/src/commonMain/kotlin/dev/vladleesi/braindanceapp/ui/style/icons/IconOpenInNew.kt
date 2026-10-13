// SPDX-License-Identifier: Apache-2.0
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val OpenInNew: ImageVector
    get() {
        if (openInNew != null) {
            return openInNew!!
        }
        openInNew =
            ImageVector
                .Builder(
                    name = "OpenInNew",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 960f,
                    viewportHeight = 960f,
                ).apply {
                    path(
                        fill = SolidColor(Color.Black),
                        fillAlpha = 1.0f,
                        stroke = null,
                        strokeAlpha = 1.0f,
                        strokeLineWidth = 1.0f,
                        strokeLineCap = StrokeCap.Butt,
                        strokeLineJoin = StrokeJoin.Miter,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(200f, 840f)
                        quadToRelative(-33f, 0f, -56.5f, -23.5f)
                        reflectiveQuadTo(120f, 760f)
                        verticalLineToRelative(-560f)
                        quadToRelative(0f, -33f, 23.5f, -56.5f)
                        reflectiveQuadTo(200f, 120f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(80f)
                        horizontalLineTo(200f)
                        verticalLineToRelative(560f)
                        horizontalLineToRelative(560f)
                        verticalLineToRelative(-280f)
                        horizontalLineToRelative(80f)
                        verticalLineToRelative(280f)
                        quadToRelative(0f, 33f, -23.5f, 56.5f)
                        reflectiveQuadTo(760f, 840f)
                        close()
                        moveToRelative(188f, -212f)
                        lineToRelative(-56f, -56f)
                        lineToRelative(372f, -372f)
                        horizontalLineTo(560f)
                        verticalLineToRelative(-80f)
                        horizontalLineToRelative(280f)
                        verticalLineToRelative(280f)
                        horizontalLineToRelative(-80f)
                        verticalLineToRelative(-144f)
                        close()
                    }
                }.build()
        return openInNew!!
    }

private var openInNew: ImageVector? = null
