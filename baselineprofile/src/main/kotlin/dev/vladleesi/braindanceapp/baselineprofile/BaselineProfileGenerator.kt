// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Records the code paths of a cold start and of browsing Home, which the release build then precompiles. The journey
 * waits for live data, so run it with the backend reachable.
 */
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val rule = BaselineProfileRule()

    @Test
    fun startupAndBrowseHome() =
        rule.collect(packageName = PACKAGE_NAME, includeInStartupProfile = true) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.hasObject(By.scrollable(true)), CONTENT_TIMEOUT_MILLIS)
            // Vertical scrolling renders every Home carousel and their image cards. The first scrollable is the Home
            // list; it is looked up again before each fling, because a reference to a Compose accessibility node can
            // go stale once the list has scrolled.
            for (direction in listOf(Direction.DOWN, Direction.UP)) {
                val home = device.findObject(By.scrollable(true)) ?: return@collect
                home.fling(direction)
                device.waitForIdle()
            }
        }

    private companion object {
        const val PACKAGE_NAME = "dev.vladleesi.braindanceapp"
        const val CONTENT_TIMEOUT_MILLIS = 15_000L
    }
}
