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

val CircleUserRound: ImageVector
    get() {
        if (circleUserRound != null) {
            return circleUserRound!!
        }
        circleUserRound =
            ImageVector
                .Builder(
                    name = "CircleUserRound",
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
                        moveTo(18f, 20f)
                        arcToRelative(6f, 6f, 0f, isMoreThanHalf = false, isPositiveArc = false, -12f, 0f)
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
                        moveTo(16f, 10f)
                        arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 14f)
                        arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 8f, 10f)
                        arcTo(4f, 4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 16f, 10f)
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
                        moveTo(22f, 12f)
                        arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 12f, 22f)
                        arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 2f, 12f)
                        arcTo(10f, 10f, 0f, isMoreThanHalf = false, isPositiveArc = true, 22f, 12f)
                        close()
                    }
                }.build()
        return circleUserRound!!
    }

private var circleUserRound: ImageVector? = null
