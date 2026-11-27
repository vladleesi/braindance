// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.text
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.shimmer

private const val SHIMMER_DURATION = 1500
private const val SHIMMER_START_WIDTH_FRACTION = -2f
private const val SHIMMER_END_WIDTH_FRACTION = 2f

// A text placeholder's bar leaves this share of its line's height free above and below, so lines read as lines.
private const val LINE_INSET_FRACTION = 0.2f

@Composable
private fun rememberShimmerProgress(): State<Float> =
    rememberInfiniteTransition().animateFloat(
        initialValue = SHIMMER_START_WIDTH_FRACTION,
        targetValue = SHIMMER_END_WIDTH_FRACTION,
        animationSpec = infiniteRepeatable(animation = tween(SHIMMER_DURATION)),
    )

/** The shimmer's colors, from the theme: a lighter band passing over the control color. */
@Composable
private fun rememberShimmerColors(): List<Color> {
    val base = secondary
    val highlight = shimmer
    return remember(base, highlight) { listOf(base, highlight, base) }
}

private fun shimmerBrush(
    progress: Float,
    size: Size,
    colors: List<Color>,
): Brush {
    val startOffsetX = progress * size.width
    return Brush.linearGradient(
        colors = colors,
        start = Offset(startOffsetX, 0f),
        end = Offset(startOffsetX + size.width, size.height),
    )
}

// The animated offset is read only while drawing, so each frame redraws the shimmer without recomposition.
fun Modifier.shimmerEffect(): Modifier =
    composed {
        val progress = rememberShimmerProgress()
        val colors = rememberShimmerColors()
        drawBehind { drawRect(brush = shimmerBrush(progress.value, size, colors)) }
    }

internal fun Modifier.placeholderSemantics(): Modifier =
    clearAndSetSemantics {
        hideFromAccessibility()
        // Compose Web updates these DOM attributes only when the properties are present.
        text = AnnotatedString("")
        contentDescription = ""
    }

/**
 * While [visible], draws a shimmer in [shape] in place of this element, which keeps its size: a placeholder made from
 * the real component lines up with it exactly, so nothing moves when the content arrives. The content is neither drawn
 * nor read by screen readers. Use it on elements that do not respond to input, as the content stays laid out.
 */
fun Modifier.placeholder(
    visible: Boolean,
    shape: Shape,
): Modifier =
    if (visible) {
        placeholderSemantics().clip(shape).shimmerEffect().drawWithContent {}
    } else {
        this
    }

/**
 * While [visible], draws a shimmering bar over each line of a text laid out as [layout] instead of the text, each as
 * long as its line. The text keeps its size, so the placeholder matches its line count and height exactly.
 */
fun Modifier.textPlaceholder(
    visible: Boolean,
    layout: () -> TextLayoutResult?,
): Modifier =
    if (visible) {
        composed {
            val progress = rememberShimmerProgress()
            val colors = rememberShimmerColors()
            placeholderSemantics().drawWithContent {
                val text = layout() ?: return@drawWithContent
                val brush = shimmerBrush(progress.value, size, colors)
                for (line in 0 until text.lineCount) {
                    val top = text.getLineTop(line)
                    val height = text.getLineBottom(line) - top
                    val inset = height * LINE_INSET_FRACTION
                    val left = text.getLineLeft(line)
                    val barHeight = height - 2 * inset
                    drawRoundRect(
                        brush = brush,
                        topLeft = Offset(left, top + inset),
                        size = Size(text.getLineRight(line) - left, barHeight),
                        cornerRadius = CornerRadius(barHeight / 2),
                    )
                }
            }
        }
    } else {
        this
    }
