// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.shimmer

private const val SHIMMER_DURATION = 1500

// TODO: Refactoring?
fun Modifier.shimmerEffect(): Modifier =
    composed {
        var size by remember {
            mutableStateOf(IntSize.Zero)
        }
        val transition = rememberInfiniteTransition()
        val startOffsetX by transition.animateFloat(
            initialValue = -2 * size.width.toFloat(),
            targetValue = 2 * size.width.toFloat(),
            animationSpec =
                infiniteRepeatable(
                    animation = tween(SHIMMER_DURATION),
                ),
        )

        background(
            brush =
                Brush.linearGradient(
                    colors =
                        listOf(
                            secondary,
                            shimmer,
                            secondary,
                        ),
                    start = Offset(startOffsetX, 0f),
                    end = Offset(startOffsetX + size.width.toFloat(), size.height.toFloat()),
                ),
        ).onGloballyPositioned {
            size = it.size
        }
    }
