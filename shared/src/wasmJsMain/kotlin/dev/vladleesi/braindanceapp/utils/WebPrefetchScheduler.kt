// SPDX-License-Identifier: MPL-2.0

@file:OptIn(InternalComposeUiApi::class, ExperimentalWasmJsInterop::class)

package dev.vladleesi.braindanceapp.utils

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformPrefetchRequest
import androidx.compose.ui.platform.PlatformPrefetchRequestScope
import androidx.compose.ui.platform.PlatformPrefetchScheduler
import kotlinx.browser.window
import kotlin.math.max
import kotlin.math.roundToLong

// Frames are 1000/60 ms on most displays and 1000/120 ms or less on fast ones; anything outside is a missed frame or
// a timer clamp, not the display's rate.
private const val DEFAULT_FRAME_MILLIS = 1000.0 / 60
private const val MIN_FRAME_MILLIS = 4.0
private const val MAX_FRAME_MILLIS = 1000.0 / 30
private const val SLOWER_RATE_WEIGHT = 8

// Left free before the next frame, so prefetching never delays it.
private const val FRAME_SAFETY_MILLIS = 1.0
private const val NANOS_PER_MILLI = 1_000_000

/**
 * Runs lazy lists' and grids' prefetch work in the time left over after each frame, as Android does. Compose's Web
 * host has no scheduler of its own and drops the work, so a lazy layout composes items only when they scroll into view,
 * inside the scroll event, which delays the frame and the next wheel event. With this scheduler, items are composed
 * ahead of time between frames, and a cache window (`rememberCachedLazyListState`) composes a viewport ahead.
 *
 * A request runs only while the current frame has time left, high priority first; Compose's requests measure their
 * own steps against that time and pause composition that does not fit. The defaults use the browser's animation frames
 * and timers; tests pass their own [now], [requestFrame], and [postTask].
 */
internal class WebPrefetchScheduler(
    private val now: () -> Double = ::performanceNow,
    private val requestFrame: ((frameTimeMillis: Double) -> Unit) -> Unit = { callback ->
        window.requestAnimationFrame { time -> callback(time) }
    },
    private val postTask: (() -> Unit) -> Unit = { task ->
        window.setTimeout(
            {
                task()
                null
            },
            0,
        )
    },
) : PlatformPrefetchScheduler {
    private val highPriority = ArrayDeque<PlatformPrefetchRequest>()
    private val lowPriority = ArrayDeque<PlatformPrefetchRequest>()
    private var isFrameRequested = false
    private var lastFrameTime = Double.NaN
    private var frameMillis = DEFAULT_FRAME_MILLIS
    private var deadline = 0.0

    private val scope =
        object : PlatformPrefetchRequestScope {
            override fun availableTimeNanos(): Long = max(0.0, (deadline - now()) * NANOS_PER_MILLI).roundToLong()
        }

    override fun scheduleHighPriorityPrefetch(request: PlatformPrefetchRequest) {
        highPriority.addLast(request)
        requestFrameIfNeeded()
    }

    override fun scheduleLowPriorityPrefetch(request: PlatformPrefetchRequest) {
        lowPriority.addLast(request)
        requestFrameIfNeeded()
    }

    private fun requestFrameIfNeeded() {
        if (isFrameRequested || (highPriority.isEmpty() && lowPriority.isEmpty())) return
        isFrameRequested = true
        requestFrame(::onFrame)
    }

    private fun onFrame(frameTime: Double) {
        isFrameRequested = false
        val interval = frameTime - lastFrameTime
        // Consecutive frames show the display's rate. A shorter interval is taken at once; a longer one, which may be a
        // single slow frame, only gradually, so one dropped frame never makes the next budget too large.
        if (interval in MIN_FRAME_MILLIS..MAX_FRAME_MILLIS) {
            frameMillis =
                if (interval < frameMillis) interval else frameMillis + (interval - frameMillis) / SLOWER_RATE_WEIGHT
        }
        lastFrameTime = frameTime
        // After this frame's rendering, which runs in the same animation frame, use what is left of it.
        postTask { runUntil(frameTime + frameMillis - FRAME_SAFETY_MILLIS) }
    }

    private fun runUntil(frameDeadline: Double) {
        deadline = frameDeadline
        while (scope.availableTimeNanos() > 0) {
            val queue = highPriority.ifEmpty { lowPriority }
            val request = queue.firstOrNull() ?: break
            val hasMoreWork = with(request) { scope.execute() }
            // A request with more work ran out of time; it continues after the next frame.
            if (hasMoreWork) break
            queue.removeFirst()
        }
        requestFrameIfNeeded()
    }
}

private fun performanceNow(): Double = js("performance.now()")
