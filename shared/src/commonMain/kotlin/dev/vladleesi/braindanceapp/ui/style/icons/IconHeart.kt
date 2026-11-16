// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Lucide's heart outline. */
val Heart: ImageVector by lazy { heart(name = "Heart", filled = false) }

/** The same heart with its inside filled, for a selected state. */
val HeartFilled: ImageVector by lazy { heart(name = "HeartFilled", filled = true) }

private fun heart(
    name: String,
    filled: Boolean,
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
                fill = if (filled) SolidColor(Color.Black) else null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19f, 14f)
                curveToRelative(1.49f, -1.46f, 3f, -3.21f, 3f, -5.5f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 16.5f, 3f)
                curveToRelative(-1.76f, 0f, -3f, 0.5f, -4.5f, 2f)
                curveToRelative(-1.5f, -1.5f, -2.74f, -2f, -4.5f, -2f)
                arcTo(5.5f, 5.5f, 0f, isMoreThanHalf = false, isPositiveArc = false, 2f, 8.5f)
                curveToRelative(0f, 2.3f, 1.5f, 4.05f, 3f, 5.5f)
                lineToRelative(7f, 7f)
                close()
            }
        }.build()
