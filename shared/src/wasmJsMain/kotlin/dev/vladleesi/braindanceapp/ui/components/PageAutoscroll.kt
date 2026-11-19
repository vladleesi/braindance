// SPDX-License-Identifier: MPL-2.0

@file:OptIn(ExperimentalComposeUiApi::class, ExperimentalWasmJsInterop::class)

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.fromKeyword
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.white
import kotlinx.browser.window
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sign

/** How far the pointer may move from the anchor without scrolling, so a click can start autoscrolling. */
private val AutoscrollDeadZone = 12.dp

/** Scroll speed per pixel of pointer distance beyond the dead zone: 8 px per second. */
private const val AUTOSCROLL_SPEED_PER_PIXEL = 8f

/** The fastest autoscroll, in pixels per second per density unit (8,000 dp per second). */
private const val AUTOSCROLL_MAX_SPEED_DP = 8_000f

private const val MILLIS_PER_SECOND = 1_000f

private val AllScrollIcon = PointerIcon.fromKeyword("all-scroll")

/**
 * The vertical autoscroll speed, in pixels per second, for a pointer [offset] pixels below (positive) or above
 * (negative) the anchor: nothing within [deadZone], then faster the farther the pointer is, up to a limit.
 */
internal fun autoscrollVelocity(
    offset: Float,
    deadZone: Float,
    density: Float,
): Float {
    val beyond = abs(offset) - deadZone
    if (beyond <= 0f) return 0f
    return sign(offset) * min(beyond * AUTOSCROLL_SPEED_PER_PIXEL, AUTOSCROLL_MAX_SPEED_DP * density)
}

/**
 * Whether releasing the middle button ends autoscrolling. As in browsers, a press that moved beyond the [deadZone]
 * scrolls only while held; a click without moving keeps autoscrolling until the next click.
 */
internal fun autoscrollEndsOnRelease(
    maxTravel: Float,
    deadZone: Float,
): Boolean = maxTravel > deadZone

/** A page that autoscrolling can move: its scroll [state] and where it is, in the root's coordinates. */
internal class AutoscrollTarget(
    val state: ScrollableState,
) {
    var bounds: Rect = Rect.Zero
    var attached: Boolean = false
}

/** The pages on screen that autoscrolling can move; [PageScrollbar] adds each page while it is shown. */
internal class AutoscrollTargets {
    private val targets = mutableListOf<AutoscrollTarget>()

    fun add(target: AutoscrollTarget) {
        targets += target
        target.attached = true
    }

    fun remove(target: AutoscrollTarget) {
        targets -= target
        target.attached = false
    }

    /** The innermost page under [position] that has somewhere to scroll, or null. */
    fun pageAt(position: Offset): AutoscrollTarget? =
        targets
            .filter { it.bounds.contains(position) && (it.state.canScrollForward || it.state.canScrollBackward) }
            .minByOrNull { it.bounds.width * it.bounds.height }
}

private val LocalAutoscrollTargets = staticCompositionLocalOf<AutoscrollTargets?> { null }

/** Makes this page, which scrolls with [state], movable by [PageAutoscrollHost]. */
@Composable
internal fun BoxScope.PageAutoscrollTarget(state: ScrollableState) {
    val targets = LocalAutoscrollTargets.current ?: return
    val target = remember(state) { AutoscrollTarget(state) }
    DisposableEffect(targets, target) {
        targets.add(target)
        onDispose { targets.remove(target) }
    }
    // Measures the page without taking part in input: a layout without pointer input is never hit.
    Box(Modifier.matchParentSize().onGloballyPositioned { target.bounds = it.boundsInRoot() })
}

/**
 * Hosts [content] with the middle-click autoscrolling that browsers on Windows give scrollable pages: a middle press
 * that no link takes, over a page that can scroll, marks an anchor; moving the pointer away from it scrolls the page,
 * faster the farther away. A click keeps autoscrolling until the next click, Esc, or a wheel scroll; a press that moves
 * beyond the dead zone scrolls until it is released. Compose lists are not scrollable page elements, so the browser
 * cannot do this itself. Other systems have no such convention (Linux pastes on a middle click), so only Windows gets
 * it.
 */
@Composable
internal fun PageAutoscrollHost(content: @Composable () -> Unit) {
    val targets = remember { AutoscrollTargets() }
    val enabled = remember { isWindowsPlatform() }
    var anchor by remember { mutableStateOf<Offset?>(null) }
    CompositionLocalProvider(LocalAutoscrollTargets provides targets) {
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .then(
                        if (enabled) {
                            Modifier.pointerInput(targets) { handleAutoscroll(targets) { anchor = it } }
                        } else {
                            Modifier
                        },
                    ).then(
                        if (anchor !=
                            null
                        ) {
                            Modifier.pointerHoverIcon(AllScrollIcon, overrideDescendants = true)
                        } else {
                            Modifier
                        },
                    ),
            propagateMinConstraints = true,
        ) {
            content()
            anchor?.let { AutoscrollMarker(it) }
        }
    }
}

private suspend fun PointerInputScope.handleAutoscroll(
    targets: AutoscrollTargets,
    onAnchor: (Offset?) -> Unit,
) = coroutineScope {
    val deadZone = AutoscrollDeadZone.toPx()
    awaitEachGesture {
        // The main pass reaches this root after everything under the pointer, so a link that took the press is known.
        val press = awaitPointerEvent(PointerEventPass.Main)
        val change = press.changes.first()
        val autoscrollPress =
            press.type == PointerEventType.Press &&
                change.type == PointerType.Mouse &&
                press.buttons.isTertiaryPressed &&
                !change.isConsumed
        if (!autoscrollPress) return@awaitEachGesture
        val target = targets.pageAt(change.position) ?: return@awaitEachGesture
        change.consume()

        val anchor = change.position
        var velocity = 0f
        val scrolling = launch { scrollContinuously(target) { velocity } }
        val stopOnEscapeOrBlur = stopListener(scrolling)
        scrolling.invokeOnCompletion {
            stopOnEscapeOrBlur.remove()
            onAnchor(null)
        }
        onAnchor(anchor)
        try {
            var held = true
            var maxTravel = 0f
            while (scrolling.isActive) {
                // The initial pass comes first, so a click that ends autoscrolling never reaches what is under it.
                val event = awaitPointerEvent(PointerEventPass.Initial)
                if (!scrolling.isActive) break
                val pointer = event.changes.first()
                when (event.type) {
                    PointerEventType.Move -> {
                        velocity = autoscrollVelocity(pointer.position.y - anchor.y, deadZone, density)
                        if (held) maxTravel = max(maxTravel, (pointer.position - anchor).getDistance())
                    }
                    PointerEventType.Release ->
                        if (held) {
                            held = false
                            pointer.consume()
                            if (autoscrollEndsOnRelease(maxTravel, deadZone)) break
                        }
                    PointerEventType.Press -> {
                        event.changes.forEach { it.consume() }
                        consumeUntilReleased()
                        break
                    }
                    PointerEventType.Scroll -> break
                    else -> Unit
                }
            }
        } finally {
            scrolling.cancel()
        }
    }
}

/** Scrolls [target] by the current [velocity] on every animation frame, as user input, until cancelled. */
private suspend fun scrollContinuously(
    target: AutoscrollTarget,
    velocity: () -> Float,
) {
    target.state.scroll(MutatePriority.UserInput) {
        var last = awaitAnimationFrame()
        while (target.attached) {
            val now = awaitAnimationFrame()
            scrollBy(velocity() * (now - last).toFloat() / MILLIS_PER_SECOND)
            last = now
        }
    }
}

private suspend fun AwaitPointerEventScope.consumeUntilReleased() {
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        event.changes.forEach { it.consume() }
        if (event.changes.none { it.pressed }) return
    }
}

/** A registration that ends autoscrolling on Esc or when the window loses focus; call [remove] when done. */
private class StopListener(
    private val onKey: (Event) -> Unit,
    private val onBlur: (Event) -> Unit,
) {
    fun remove() {
        window.removeEventListener("keydown", onKey)
        window.removeEventListener("blur", onBlur)
    }
}

private fun stopListener(scrolling: Job): StopListener {
    val onKey: (Event) -> Unit = { event -> if ((event as? KeyboardEvent)?.key == "Escape") scrolling.cancel() }
    val onBlur: (Event) -> Unit = { scrolling.cancel() }
    window.addEventListener("keydown", onKey)
    window.addEventListener("blur", onBlur)
    return StopListener(onKey, onBlur)
}

/** The next animation frame's timestamp, in milliseconds. */
private suspend fun awaitAnimationFrame(): Double =
    suspendCancellableCoroutine { continuation ->
        val handle = window.requestAnimationFrame { timestamp -> continuation.resume(timestamp) }
        continuation.invokeOnCancellation { window.cancelAnimationFrame(handle) }
    }

/** The anchor that autoscrolling measures from, like the browser's: a disc with arrows up and down. */
@Composable
private fun AutoscrollMarker(anchor: Offset) {
    Canvas(
        modifier =
            Modifier
                .wrapContentSize(Alignment.TopStart)
                .offset {
                    val radius = Dimens.large.toPx() / 2
                    IntOffset((anchor.x - radius).roundToInt(), (anchor.y - radius).roundToInt())
                }.size(Dimens.large),
    ) {
        drawCircle(color = surface)
        drawCircle(color = secondaryText, style = Stroke(width = Dimens.one.toPx()))
        drawArrow(pointsUp = true)
        drawArrow(pointsUp = false)
        drawCircle(color = white, radius = Dimens.micro.toPx())
    }
}

private fun DrawScope.drawArrow(pointsUp: Boolean) {
    val halfWidth = Dimens.tiny.toPx()
    val height = Dimens.tiny.toPx()
    val gap = Dimens.tiny.toPx()
    val direction = if (pointsUp) -1f else 1f
    val baseY = center.y + direction * gap
    val tipY = baseY + direction * height
    val arrow =
        Path().apply {
            moveTo(center.x - halfWidth, baseY)
            lineTo(center.x + halfWidth, baseY)
            lineTo(center.x, tipY)
            close()
        }
    drawPath(arrow, color = white)
}

private fun isWindowsPlatform(): Boolean = js("/Win/.test(navigator.platform)")

@Preview
@Composable
internal fun AutoscrollMarkerPreview() {
    BraindanceTheme {
        Box(Modifier.size(Dimens.extraLarge)) {
            AutoscrollMarker(Offset(x = 64f, y = 64f))
        }
    }
}
