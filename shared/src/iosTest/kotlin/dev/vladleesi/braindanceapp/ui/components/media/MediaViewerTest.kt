// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The gallery and its full-screen viewer together, as the details screen shows them. Runs on iOS, without network. */
private const val SCREENSHOTS = 4

@OptIn(ExperimentalTestApi::class)
class MediaViewerTest {
    private val dispatcher = NavigationEventDispatcher()
    private val back = DirectNavigationEventInput().also(dispatcher::addInput)
    private var screenBacks = 0

    // A video first, as games have them: the viewer shows only the screenshots.
    private val gallery =
        MediaGallery(
            videos = listOf(GameMedia.YouTubeVideo("aaaaaaaaaaa")),
            screenshots =
                List(SCREENSHOTS) { index ->
                    GameMedia.Screenshot("https://images.igdb.com/igdb/image/upload/t_thumb/sc$index.jpg")
                },
        )

    private fun ComposeUiTest.setGallery(state: MediaGalleryState) {
        val owner =
            object : NavigationEventDispatcherOwner {
                override val navigationEventDispatcher = dispatcher
            }
        setContent {
            CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
                BraindanceTheme {
                    // Stands in for NavDisplay, which handles back for the screen.
                    NavigationBackHandler(
                        state = rememberNavigationEventState(NavigationEventInfo.None),
                        onBackCompleted = { screenBacks++ },
                    )
                    Box {
                        MediaGallery(state = state, gameName = "Sample game", onWatchOnYouTube = {})
                        if (state.isViewerOpen) {
                            MediaViewer(state = state, gameName = "Sample game")
                        }
                    }
                }
            }
        }
    }

    @Test
    fun theViewerOpensOnTheSelectionAndClosingKeepsTheScreenshotViewedLast() =
        runComposeUiTest {
            val state = MediaGalleryState(gallery, initialIndex = 2)
            setGallery(state)

            onNodeWithContentDescription("Open full screen").performClick()
            waitForIdle()
            assertTrue(state.isViewerOpen)
            onNodeWithText("2 / 4").assertIsDisplayed()

            onRoot().performKeyInput { pressKey(Key.DirectionRight) }
            waitForIdle()
            onNodeWithText("3 / 4").assertIsDisplayed()

            onNodeWithContentDescription("Close").performClick()
            waitForIdle()

            assertFalse(state.isViewerOpen)
            assertEquals(3, state.selectedIndex, "the gallery shows the screenshot viewed last")
        }

    @Test
    fun videosHaveNoFullScreenButtonAndTheViewerNeverReachesThem() =
        runComposeUiTest {
            val state = MediaGalleryState(gallery)
            setGallery(state)

            onNodeWithContentDescription("Open full screen").assertDoesNotExist()

            state.select(1)
            waitForIdle()
            onNodeWithContentDescription("Open full screen").performClick()
            waitForIdle()
            onRoot().performKeyInput {
                pressKey(Key.DirectionLeft)
                pressKey(Key.MoveHome)
            }
            waitForIdle()

            assertEquals(1, state.selectedIndex, "the first screenshot, not the video before it")
            onNodeWithText("1 / 4").assertIsDisplayed()
        }

    @Test
    fun backAndEscapeCloseTheViewerBeforeTheScreen() =
        runComposeUiTest {
            val state = MediaGalleryState(gallery, initialIndex = 1)
            setGallery(state)

            state.openViewer()
            waitForIdle()
            back.backCompleted()
            waitForIdle()
            assertFalse(state.isViewerOpen)
            assertEquals(0, screenBacks, "Back closed the viewer, not the screen")

            state.openViewer()
            waitForIdle()
            onRoot().performKeyInput { pressKey(Key.Escape) }
            waitForIdle()
            assertFalse(state.isViewerOpen)

            back.backCompleted()
            waitForIdle()
            assertEquals(1, screenBacks, "with the viewer closed, Back leaves the screen")
        }

    @Test
    fun theScreenshotItselfIsNotAButton() =
        runComposeUiTest {
            val state = MediaGalleryState(gallery, initialIndex = 1)
            setGallery(state)

            onNodeWithContentDescription("Image: Sample game, screenshot 1 of 4").performClick()
            waitForIdle()

            assertFalse(state.isViewerOpen, "only the full-screen button opens the viewer")
        }
}
