// SPDX-License-Identifier: MPL-2.0

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val Star: ImageVector
    get() {
        star?.let { return it }
        val imageVector =
            ImageVector
                .Builder(
                    name = "Star",
                    defaultWidth = 20.dp,
                    defaultHeight = 20.dp,
                    viewportWidth = 20f,
                    viewportHeight = 20f,
                ).apply {
                    path(
                        fill = null,
                        stroke = SolidColor(Color.Black),
                        strokeLineWidth = 2f,
                        strokeLineJoin = StrokeJoin.Round,
                    ) {
                        moveTo(10f, 1f)
                        lineTo(12.645034f, 6.3594236f)
                        lineTo(18.55951f, 7.218847f)
                        lineTo(14.279755f, 11.390576f)
                        lineTo(15.290068f, 17.281153f)
                        lineTo(10f, 14.5f)
                        lineTo(4.709933f, 17.281153f)
                        lineTo(5.7202454f, 11.390576f)
                        lineTo(1.4404917f, 7.218847f)
                        lineTo(7.354966f, 6.3594236f)
                        close()
                    }
                }.build()
        star = imageVector
        return imageVector
    }

private var star: ImageVector? = null
