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
 * Records the code paths of a cold start, of browsing Home and a section's full list, and of searching games
 * on Discover, which the release build then precompiles. The journeys wait for live data, so run them with the
 * backend reachable.
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

    @Test
    fun browseFullList() =
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            // See all names its section; the first one opens Popular Right Now.
            device.wait(Until.findObject(By.descStartsWith(SEE_ALL_PREFIX)), CONTENT_TIMEOUT_MILLIS)?.click()
                ?: return@collect
            device.wait(Until.hasObject(By.descStartsWith(COVER_DESCRIPTION_PREFIX)), CONTENT_TIMEOUT_MILLIS)
            // Scrolling renders list cards and loads further pages; the grid is looked up again before each fling.
            for (direction in listOf(Direction.DOWN, Direction.DOWN, Direction.UP)) {
                val list = device.findObject(By.scrollable(true)) ?: return@collect
                list.fling(direction)
                device.waitForIdle()
            }
        }

    @Test
    fun searchGames() =
        rule.collect(packageName = PACKAGE_NAME) {
            pressHome()
            startActivityAndWait()
            device.wait(Until.findObject(By.text(DISCOVER_LABEL)), CONTENT_TIMEOUT_MILLIS)?.click() ?: return@collect
            // Compose exposes the search field as the screen's only EditText.
            val field =
                device.wait(Until.findObject(By.clazz(EDIT_TEXT_CLASS)), CONTENT_TIMEOUT_MILLIS) ?: return@collect
            field.text = SEARCH_QUERY
            // Result cards describe their covers; they replace the placeholders once the search has answered.
            device.wait(Until.hasObject(By.descStartsWith(COVER_DESCRIPTION_PREFIX)), CONTENT_TIMEOUT_MILLIS)
            // Scrolling renders result cards and loads further pages; the grid is looked up again before each fling.
            for (direction in listOf(Direction.DOWN, Direction.DOWN, Direction.UP)) {
                val results = device.findObject(By.scrollable(true)) ?: return@collect
                results.fling(direction)
                device.waitForIdle()
            }
        }

    private companion object {
        const val PACKAGE_NAME = "dev.vladleesi.braindanceapp"
        const val CONTENT_TIMEOUT_MILLIS = 15_000L
        const val DISCOVER_LABEL = "Discover"
        const val SEE_ALL_PREFIX = "See all "
        const val EDIT_TEXT_CLASS = "android.widget.EditText"

        // A broad title with many results, so the journey pages through them.
        const val SEARCH_QUERY = "mario"
        const val COVER_DESCRIPTION_PREFIX = "Image: "
    }
}
