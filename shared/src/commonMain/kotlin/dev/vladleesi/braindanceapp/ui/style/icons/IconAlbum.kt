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

val Album: ImageVector
    get() {
        if (album != null) {
            return album!!
        }
        album =
            ImageVector
                .Builder(
                    name = "Album",
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
                        moveTo(5f, 3f)
                        horizontalLineTo(19f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 5f)
                        verticalLineTo(19f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 21f)
                        horizontalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 19f)
                        verticalLineTo(5f)
                        arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 3f)
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
                        moveTo(11f, 3f)
                        lineTo(11f, 11f)
                        lineTo(14f, 8f)
                        lineTo(17f, 11f)
                        lineTo(17f, 3f)
                    }
                }.build()
        return album!!
    }

private var album: ImageVector? = null
