// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ScrollUnlessInterruptedTest {
    @Test
    fun anInterruptedScrollEndsWithoutEndingTheCaller() =
        runTest {
            var followed = 0
            listOf(1, 2, 3).forEach { _ ->
                // Compose interrupts a scroll by cancelling it, as a gesture or a focus scroll does.
                scrollUnlessInterrupted { throw CancellationException("another scroll took over") }
                followed += 1
            }

            assertEquals(3, followed, "the caller keeps following the selection")
        }

    @Test
    fun cancellingTheCallerStillCancelsIt() =
        runTest {
            var finished = false
            val follower =
                launch {
                    scrollUnlessInterrupted { awaitCancellation() }
                    finished = true
                }
            runCurrent()

            follower.cancel()
            runCurrent()

            assertTrue(follower.isCancelled)
            assertTrue(!finished, "a cancelled caller does not carry on")
        }
}
