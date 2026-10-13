// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Newspaper: ImageVector
    get() {
        if (newspaper != null) {
            return newspaper!!
        }
        newspaper =
            ImageVector
                .Builder(
                    name = "Newspaper",
                    defaultWidth = 24.dp,
                    defaultHeight = 24.dp,
                    viewportWidth = 24f,
                    viewportHeight = 24f,
                ).apply {
                    path(
                        fill = null,
                        fillAlpha = 1.0f,
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeAlpha = 1.0f,
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(4f, 22f)
                        horizontalLineToRelative(16f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, -2f)
                        verticalLineTo(4f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, -2f)
                        horizontalLineTo(8f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = false, -2f, 2f)
                        verticalLineToRelative(16f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, 2f)
                        close()
                        moveToRelative(0f, 0f)
                        arcToRelative(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, -2f, -2f)
                        verticalLineToRelative(-9f)
                        curveToRelative(0f, -1.1f, 0.9f, -2f, 2f, -2f)
                        horizontalLineToRelative(2f)
                    }
                    path(
                        fill = null,
                        fillAlpha = 1.0f,
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeAlpha = 1.0f,
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(18f, 14f)
                        horizontalLineToRelative(-8f)
                    }
                    path(
                        fill = null,
                        fillAlpha = 1.0f,
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeAlpha = 1.0f,
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(15f, 18f)
                        horizontalLineToRelative(-5f)
                    }
                    path(
                        fill = null,
                        fillAlpha = 1.0f,
                        stroke = SolidColor(Color(0xFF000000)),
                        strokeAlpha = 1.0f,
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                        strokeLineJoin = StrokeJoin.Round,
                        strokeLineMiter = 1.0f,
                        pathFillType = PathFillType.NonZero,
                    ) {
                        moveTo(10f, 6f)
                        horizontalLineToRelative(8f)
                        verticalLineToRelative(4f)
                        horizontalLineToRelative(-8f)
                        verticalLineTo(6f)
                        close()
                    }
                }.build()
        return newspaper!!
    }

private var newspaper: ImageVector? = null
