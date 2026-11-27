// SPDX-License-Identifier: ISC AND MIT
// Adapted icon artwork; see THIRD_PARTY_NOTICES.md and LICENSES/.

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Lucide's `panel-left-close`: a panel with its left column and an arrow pointing into it, to collapse a sidebar. */
val PanelLeftClose: ImageVector by lazy {
    panelLeftIcon(name = "PanelLeftClose") {
        moveTo(16f, 15f)
        lineTo(13f, 12f)
        lineTo(16f, 9f)
    }
}

/** Lucide's `panel-left-open`: the same panel with the arrow pointing out of the column, to expand a sidebar. */
val PanelLeftOpen: ImageVector by lazy {
    panelLeftIcon(name = "PanelLeftOpen") {
        moveTo(14f, 9f)
        lineTo(17f, 12f)
        lineTo(14f, 15f)
    }
}

private fun panelLeftIcon(
    name: String,
    arrow: PathBuilder.() -> Unit,
): ImageVector =
    ImageVector
        .Builder(
            name = name,
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            val stroke: (PathBuilder.() -> Unit) -> Unit = { pathBuilder ->
                path(
                    fill = null,
                    stroke = SolidColor(Color.Black),
                    strokeLineWidth = 2f,
                    strokeLineCap = StrokeCap.Round,
                    strokeLineJoin = StrokeJoin.Round,
                    pathBuilder = pathBuilder,
                )
            }
            // <rect width="18" height="18" x="3" y="3" rx="2" />
            stroke {
                moveTo(5f, 3f)
                horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 21f, y1 = 5f)
                verticalLineTo(19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 19f, y1 = 21f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 3f, y1 = 19f)
                verticalLineTo(5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, x1 = 5f, y1 = 3f)
                close()
            }
            // <path d="M9 3v18" />
            stroke {
                moveTo(9f, 3f)
                verticalLineTo(21f)
            }
            stroke(arrow)
        }.build()
