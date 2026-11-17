// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.utils.coveredBottomPadding
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class CoverAndKeyboardLayoutTest {
    @Test
    fun aCardWithoutACoverShowsItsTitleOnTheTile() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    GameCard(
                        card = GameCardModel(id = 1, title = "Game Without Cover", backgroundImageUrl = ""),
                        onClick = {},
                        showTitle = false,
                        modifier = Modifier.width(Dimens.gameCardWidth),
                    )
                }
            }

            // With showTitle off, the tile is the only place the title can come from.
            onNodeWithText("Game Without Cover", useUnmergedTree = true).assertExists()
        }

    @Test
    fun contentEndsAtTheKeyboardAboveABottomBarTheKeyboardCovers() {
        assertContentBottom(keyboard = 150.dp, expectedAboveRootBottom = 150.dp)
    }

    @Test
    fun aKeyboardThatOnlyCoversTheBottomBarLeavesTheContentAlone() {
        assertContentBottom(keyboard = 30.dp, expectedAboveRootBottom = BAR_HEIGHT)
        assertContentBottom(keyboard = 0.dp, expectedAboveRootBottom = BAR_HEIGHT)
    }

    /** Lays out content above a bottom bar, with a keyboard of [keyboard] height from the root's bottom edge. */
    private fun assertContentBottom(
        keyboard: Dp,
        expectedAboveRootBottom: Dp,
    ) = runComposeUiTest {
        setContent {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f).fillMaxWidth().coveredBottomPadding(WindowInsets(bottom = keyboard))) {
                    Box(Modifier.fillMaxSize().testTag("content"))
                }
                Box(Modifier.fillMaxWidth().height(BAR_HEIGHT))
            }
        }
        waitForIdle()

        val rootBottom = onRoot().getBoundsInRoot().bottom
        val contentBottom = onNodeWithTag("content").getBoundsInRoot().bottom
        val aboveRootBottom = rootBottom - contentBottom
        assertTrue(
            abs((aboveRootBottom - expectedAboveRootBottom).value) < 1f,
            "content ends $aboveRootBottom above the root's bottom, expected $expectedAboveRootBottom",
        )
    }

    private companion object {
        val BAR_HEIGHT = 50.dp
    }
}
