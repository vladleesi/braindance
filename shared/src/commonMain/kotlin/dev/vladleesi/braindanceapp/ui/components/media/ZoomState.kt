// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroid
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min

// At most four times the fitted size, or the fill scale when that is larger, as for a tall image on a wide screen.
private const val MAX_ZOOM = 4f

// Double-tap zooms in this far, about where a screenshot's interface text becomes readable on a phone.
private const val DOUBLE_TAP_ZOOM = 2.5f

// One wheel notch is about 100 pixels in browsers; this makes it zoom by about a fifth.
private const val WHEEL_ZOOM_PER_PIXEL = 0.002f

// Keyboard zoom steps.
internal const val KEY_ZOOM_STEP = 1.25f
private const val SCALE_EPSILON = 0.01f

// Filling is offered when it enlarges the image by at least this much; below it, fit and fill look the same.
private const val MIN_FILL_SCALE = 1.05f

/**
 * The zoom and pan of an image fitted into a container. [scale] 1 is the fitted image; [offset] moves it from the
 * center. The image never moves past its edges, and at scale 1 it stays centered, so the whole image is always one
 * zoom-out away. "Fill" is the scale at which the image covers the container, cropping the rest out of view but not
 * out of reach: it can still be panned.
 */
@Stable
internal class ZoomState {
    var scale by mutableFloatStateOf(1f)
        private set

    var offset by mutableStateOf(Offset.Zero)
        private set

    /** The container's size in pixels, set by its layout. */
    var containerSize by mutableStateOf(Size.Zero)
        internal set

    /** The image's own size, or [Size.Unspecified] until it loads. */
    var imageSize by mutableStateOf(Size.Unspecified)
        internal set

    val isZoomed: Boolean get() = scale > 1f + SCALE_EPSILON

    /** The scale at which the image covers the container, or 1 when it already does. */
    val fillScale: Float get() = ZoomMath.fillScale(imageSize, containerSize)

    /** Whether filling the container would visibly change the fitted image. */
    val canFill: Boolean get() = fillScale >= MIN_FILL_SCALE

    /** Whether the image is shown at [fillScale], covering the container. */
    val isFilled: Boolean get() = fillScale > 1f + SCALE_EPSILON && abs(scale - fillScale) < SCALE_EPSILON

    private val maxScale: Float get() = max(MAX_ZOOM, fillScale)

    /** Zooms by [factor] keeping the point at [focus], measured from the container's center, in place. */
    fun zoomBy(
        factor: Float,
        focus: Offset = Offset.Zero,
    ) {
        if (!factor.isFinite() || factor <= 0f) return
        val newScale = (scale * factor).coerceIn(1f, maxScale)
        val newOffset = ZoomMath.offsetKeepingFocus(offset, scale, newScale, focus)
        apply(newScale, newOffset)
    }

    fun panBy(delta: Offset) = apply(scale, offset + delta)

    /** Double-tap: zooms in at [focus], or back to the whole image. */
    fun toggleZoom(focus: Offset) {
        if (isZoomed) reset() else zoomBy(DOUBLE_TAP_ZOOM, focus)
    }

    /** Switches between the whole image and the image covering the container. */
    fun toggleFill() {
        if (isZoomed) reset() else apply(fillScale, Offset.Zero)
    }

    fun reset() = apply(1f, Offset.Zero)

    private fun apply(
        newScale: Float,
        newOffset: Offset,
    ) {
        scale = newScale
        offset = ZoomMath.clampOffset(newOffset, newScale, ZoomMath.fittedSize(imageSize, containerSize), containerSize)
    }
}

/** Zoom and pan arithmetic, without state. Sizes are in pixels; offsets are from the container's center. */
internal object ZoomMath {
    /** [image]'s size when fitted into [container], keeping its aspect ratio; the container's size when unknown. */
    fun fittedSize(
        image: Size,
        container: Size,
    ): Size {
        if (!image.isSpecified || image.width <= 0f || image.height <= 0f) return container
        val factor = min(container.width / image.width, container.height / image.height)
        return Size(image.width * factor, image.height * factor)
    }

    /** How much larger than fitted [image] must be to cover [container]; 1 when the shapes match or are unknown. */
    fun fillScale(
        image: Size,
        container: Size,
    ): Float {
        val fitted = fittedSize(image, container)
        if (fitted.width <= 0f || fitted.height <= 0f) return 1f
        return max(container.width / fitted.width, container.height / fitted.height).coerceAtLeast(1f)
    }

    /** The offset that keeps the content under [focus] in place when the scale changes from [from] to [to]. */
    fun offsetKeepingFocus(
        offset: Offset,
        from: Float,
        to: Float,
        focus: Offset,
    ): Offset = focus - (focus - offset) * (to / from)

    /** [offset] limited so the [fitted] image, scaled by [scale], never shows space beyond its edges. */
    fun clampOffset(
        offset: Offset,
        scale: Float,
        fitted: Size,
        container: Size,
    ): Offset {
        val maxX = ((fitted.width * scale - container.width) / 2f).coerceAtLeast(0f)
        val maxY = ((fitted.height * scale - container.height) / 2f).coerceAtLeast(0f)
        return Offset(offset.x.coerceIn(-maxX, maxX), offset.y.coerceIn(-maxY, maxY))
    }
}

/** Draws this content at [state]'s zoom and pan. */
internal fun Modifier.zoomed(state: ZoomState): Modifier =
    graphicsLayer {
        scaleX = state.scale
        scaleY = state.scale
        translationX = state.offset.x
        translationY = state.offset.y
    }

/**
 * Zoom gestures for [state]: pinch and two-finger pan, a one-finger or mouse drag while zoomed, double-tap or
 * double-click to zoom, and Ctrl or ⌘ with the wheel, which is also how browsers report a trackpad pinch. An
 * unzoomed one-finger drag is left to the parent, so a swipe still moves to the next item.
 */
internal fun Modifier.zoomGestures(state: ZoomState): Modifier =
    pointerInput(state) {
        detectTapGestures(
            onDoubleTap = { position -> state.toggleZoom(position - Offset(size.width / 2f, size.height / 2f)) },
        )
    }.pointerInput(state) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            do {
                val event = awaitPointerEvent()
                val pressed = event.changes.count { it.pressed }
                if (pressed >= 2 || (pressed == 1 && state.isZoomed)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val zoom = event.calculateZoom()
                    if (zoom != 1f) state.zoomBy(zoom, event.calculateCentroid(useCurrent = false) - center)
                    state.panBy(event.calculatePan())
                    event.changes.forEach { change -> if (change.positionChanged()) change.consume() }
                }
            } while (event.changes.any { it.pressed })
        }
    }.pointerInput(state) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent()
                val modifiers = event.keyboardModifiers
                if (event.type == PointerEventType.Scroll && (modifiers.isCtrlPressed || modifiers.isMetaPressed)) {
                    val change = event.changes.first()
                    val center = Offset(size.width / 2f, size.height / 2f)
                    state.zoomBy(exp(-change.scrollDelta.y * WHEEL_ZOOM_PER_PIXEL), change.position - center)
                    event.changes.forEach { it.consume() }
                }
            }
        }
    }
