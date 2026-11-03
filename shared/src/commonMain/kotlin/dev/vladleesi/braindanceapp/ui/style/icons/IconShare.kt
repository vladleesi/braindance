// SPDX-License-Identifier: MPL-2.0

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Share: ImageVector
    get() {
        share?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "Share",
                    defaultWidth = 20.dp,
                    defaultHeight = 20.dp,
                    viewportWidth = 20f,
                    viewportHeight = 20f,
                ).apply {
                    path(
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineCap = StrokeCap.Round,
                    ) {
                        moveTo(6.8f, 8.8f)
                        lineTo(13.2f, 5.6f)
                        moveTo(6.8f, 11.2f)
                        lineTo(13.2f, 14.4f)
                    }
                    path(
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                    ) {
                        moveTo(7.4f, 10f)
                        arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -4.8f, 0f)
                        arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4.8f, 0f)
                        close()
                        moveTo(17.4f, 4.4f)
                        arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -4.8f, 0f)
                        arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4.8f, 0f)
                        close()
                        moveTo(17.4f, 15.6f)
                        arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, -4.8f, 0f)
                        arcToRelative(2.4f, 2.4f, 0f, isMoreThanHalf = false, isPositiveArc = true, 4.8f, 0f)
                        close()
                    }
                }.build()
        share = imageVector
        return imageVector
    }

private var share: ImageVector? = null
