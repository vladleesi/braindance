// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.gestures.ScrollScope
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class PageAutoscrollTest {
    @Test
    fun pointerInsideDeadZoneDoesNotScroll() {
        assertEquals(0f, autoscrollVelocity(offset = 12f, deadZone = 12f, density = 1f))
        assertEquals(0f, autoscrollVelocity(offset = -5f, deadZone = 12f, density = 1f))
    }

    @Test
    fun speedGrowsWithDistanceInThePointersDirection() {
        val near = autoscrollVelocity(offset = 22f, deadZone = 12f, density = 1f)
        val far = autoscrollVelocity(offset = 112f, deadZone = 12f, density = 1f)

        assertEquals(80f, near)
        assertEquals(800f, far)
        assertEquals(-800f, autoscrollVelocity(offset = -112f, deadZone = 12f, density = 1f))
    }

    @Test
    fun speedIsLimitedPerDensity() {
        assertEquals(8_000f, autoscrollVelocity(offset = 100_000f, deadZone = 12f, density = 1f))
        assertEquals(-16_000f, autoscrollVelocity(offset = -100_000f, deadZone = 24f, density = 2f))
    }

    @Test
    fun clickKeepsAutoscrollingButDragEndsOnRelease() {
        assertFalse(autoscrollEndsOnRelease(maxTravel = 3f, deadZone = 12f))
        assertTrue(autoscrollEndsOnRelease(maxTravel = 40f, deadZone = 12f))
    }

    @Test
    fun innermostScrollablePageUnderPointerIsChosen() {
        val targets = AutoscrollTargets()
        val page = target(canScroll = true, Rect(0f, 0f, 1000f, 800f))
        val inner = target(canScroll = true, Rect(100f, 100f, 500f, 500f))
        targets.add(page)
        targets.add(inner)

        assertSame(inner, targets.pageAt(Offset(200f, 200f)))
        assertSame(page, targets.pageAt(Offset(800f, 700f)))
        assertNull(targets.pageAt(Offset(1200f, 200f)))
    }

    @Test
    fun pagesThatCannotScrollOrAreGoneAreSkipped() {
        val targets = AutoscrollTargets()
        val fixed = target(canScroll = false, Rect(0f, 0f, 1000f, 800f))
        val removed = target(canScroll = true, Rect(0f, 0f, 1000f, 800f))
        targets.add(fixed)
        targets.add(removed)
        targets.remove(removed)

        assertNull(targets.pageAt(Offset(10f, 10f)))
        assertFalse(removed.attached)
    }

    private fun target(
        canScroll: Boolean,
        bounds: Rect,
    ) = AutoscrollTarget(FixedScrollableState(canScroll)).also { it.bounds = bounds }

    private class FixedScrollableState(
        private val canScroll: Boolean,
    ) : ScrollableState {
        override val isScrollInProgress = false
        override val canScrollForward get() = canScroll
        override val canScrollBackward = false

        override fun dispatchRawDelta(delta: Float) = 0f

        override suspend fun scroll(
            scrollPriority: MutatePriority,
            block: suspend ScrollScope.() -> Unit,
        ) = Unit
    }
}
