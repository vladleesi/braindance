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

val CalendarRange: ImageVector
    get() {
        if (calendarRange != null) {
            return calendarRange!!
        }
        calendarRange =
            ImageVector
                .Builder(
                    name = "CalendarRange",
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
                        moveTo(5f, 4f)
                        horizontalLineTo(19f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 6f)
                        verticalLineTo(20f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 22f)
                        horizontalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 20f)
                        verticalLineTo(6f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 4f)
                        close()
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
                        moveTo(16f, 2f)
                        verticalLineToRelative(4f)
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
                        moveTo(3f, 10f)
                        horizontalLineToRelative(18f)
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
                        moveTo(8f, 2f)
                        verticalLineToRelative(4f)
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
                        moveTo(17f, 14f)
                        horizontalLineToRelative(-6f)
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
                        moveTo(13f, 18f)
                        horizontalLineTo(7f)
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
                        moveTo(7f, 14f)
                        horizontalLineToRelative(0.01f)
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
                        moveTo(17f, 18f)
                        horizontalLineToRelative(0.01f)
                    }
                }.build()
        return calendarRange!!
    }

private var calendarRange: ImageVector? = null
