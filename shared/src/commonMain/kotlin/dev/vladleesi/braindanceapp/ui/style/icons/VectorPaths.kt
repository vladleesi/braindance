// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes

private const val STROKE_WIDTH = 2f

/** Adds an outlined 24-unit path in the Lucide style: a 2-unit stroke with round caps and joins. */
internal fun ImageVector.Builder.outlinePath(pathData: String): ImageVector.Builder =
    addPath(
        pathData = addPathNodes(pathData),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = STROKE_WIDTH,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round,
    )

/** Adds a filled path, for logos drawn as solid shapes. */
internal fun ImageVector.Builder.filledPath(pathData: String): ImageVector.Builder =
    addPath(pathData = addPathNodes(pathData), fill = SolidColor(Color.Black))
