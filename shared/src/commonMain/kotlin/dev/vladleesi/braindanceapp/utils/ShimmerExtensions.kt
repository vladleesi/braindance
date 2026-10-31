// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.shimmer

private const val SHIMMER_DURATION = 1500
private const val SHIMMER_START_WIDTH_FRACTION = -2f
private const val SHIMMER_END_WIDTH_FRACTION = 2f

private val ShimmerColors = listOf(secondary, shimmer, secondary)

// The animated offset is read only while drawing, so each frame redraws the shimmer without recomposition.
fun Modifier.shimmerEffect(): Modifier =
    composed {
        val transition = rememberInfiniteTransition()
        val widthFraction =
            transition.animateFloat(
                initialValue = SHIMMER_START_WIDTH_FRACTION,
                targetValue = SHIMMER_END_WIDTH_FRACTION,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(SHIMMER_DURATION),
                    ),
            )

        drawBehind {
            val startOffsetX = widthFraction.value * size.width
            drawRect(
                brush =
                    Brush.linearGradient(
                        colors = ShimmerColors,
                        start = Offset(startOffsetX, 0f),
                        end = Offset(startOffsetX + size.width, size.height),
                    ),
            )
        }
    }
