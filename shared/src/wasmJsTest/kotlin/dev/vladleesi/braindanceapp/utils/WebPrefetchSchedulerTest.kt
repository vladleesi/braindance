// SPDX-License-Identifier: MPL-2.0

@file:OptIn(InternalComposeUiApi::class)

package dev.vladleesi.braindanceapp.utils

import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.PlatformPrefetchRequest
import androidx.compose.ui.platform.PlatformPrefetchRequestScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private const val NANOS_PER_MILLI = 1_000_000

class WebPrefetchSchedulerTest {
    private var now = 0.0
    private val frames = ArrayDeque<(Double) -> Unit>()
    private val tasks = ArrayDeque<() -> Unit>()
    private val executed = mutableListOf<String>()
    private val scheduler =
        WebPrefetchScheduler(
            now = { now },
            requestFrame = { callback -> frames.addLast(callback) },
            postTask = { task -> tasks.addLast(task) },
        )

    /** A request that needs [steps] steps of [stepMillis] each and does one only while that much time is left. */
    private inner class Request(
        private val name: String,
        private var steps: Int = 1,
        private val stepMillis: Double = 2.0,
    ) : PlatformPrefetchRequest {
        override fun PlatformPrefetchRequestScope.execute(): Boolean {
            while (steps > 0) {
                if (availableTimeNanos() < stepMillis * NANOS_PER_MILLI) return true
                now += stepMillis
                steps--
                executed += name
            }
            return false
        }
    }

    /** Runs one animation frame at [frameTime], with the frame's own work taking [renderMillis], then the idle task. */
    private fun frame(
        frameTime: Double,
        renderMillis: Double = 0.0,
    ) {
        now = frameTime
        frames.removeFirst()(frameTime)
        now += renderMillis
        tasks.removeFirst()()
    }

    @Test
    fun runsNothingAndRequestsNoFramesWithoutWork() {
        assertTrue(frames.isEmpty())
    }

    @Test
    fun highPriorityRequestsRunBeforeLowPriorityOnes() {
        scheduler.scheduleLowPriorityPrefetch(Request("low"))
        scheduler.scheduleHighPriorityPrefetch(Request("high"))
        assertEquals(1, frames.size, "one frame for both requests")

        frame(frameTime = 0.0)

        assertEquals(listOf("high", "low"), executed)
        assertTrue(frames.isEmpty(), "no frame is requested once the work is done")
    }

    @Test
    fun workStopsWhenTheFrameRunsOutOfTimeAndContinuesAfterTheNextOne() {
        // A 60 Hz frame leaves about 15 ms after a 1 ms safety margin; five 4 ms steps do not fit in one.
        scheduler.scheduleHighPriorityPrefetch(Request("item", steps = 5, stepMillis = 4.0))

        frame(frameTime = 0.0)
        assertEquals(3, executed.size)

        frame(frameTime = 1000.0 / 60)
        assertEquals(5, executed.size)
        assertTrue(frames.isEmpty())
    }

    @Test
    fun aFrameThatTakesItsWholeBudgetLeavesThePrefetchingForLater() {
        scheduler.scheduleHighPriorityPrefetch(Request("item"))

        frame(frameTime = 0.0, renderMillis = 16.0)
        assertTrue(executed.isEmpty())
        assertEquals(1, frames.size, "the request waits for the next frame")

        frame(frameTime = 1000.0 / 60)
        assertEquals(listOf("item"), executed)
    }

    @Test
    fun aFasterDisplayGetsAShorterBudget() {
        // Two consecutive frames 8.3 ms apart show a 120 Hz display; 4 ms steps then fit only once per frame.
        scheduler.scheduleHighPriorityPrefetch(Request("item", steps = 3, stepMillis = 4.0))
        frame(frameTime = 0.0, renderMillis = 12.0)
        assertTrue(executed.isEmpty())

        frame(frameTime = 1000.0 / 120, renderMillis = 3.0)
        assertEquals(1, executed.size)
    }
}
