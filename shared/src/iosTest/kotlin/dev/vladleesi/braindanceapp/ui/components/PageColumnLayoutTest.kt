// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class PageColumnLayoutTest {
    @Test
    fun theTopBarTitleAndTheContentShareTheCenteredColumnsEdges() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    Column(Modifier.requiredWidth(CONTAINER_WIDTH).testTag(CONTAINER)) {
                        TopAppBar(title = TITLE, maxContentWidth = COLUMN_WIDTH)
                        Box(
                            Modifier
                                .pageColumn(Dimens.medium, COLUMN_WIDTH)
                                .fillMaxWidth()
                                .height(BLOCK_HEIGHT)
                                .testTag(CONTENT),
                        )
                        // A card that limits its own width keeps it, at the column's start.
                        Box(
                            Modifier
                                .pageColumn(Dimens.medium, COLUMN_WIDTH)
                                .widthIn(max = NARROW_WIDTH)
                                .fillMaxWidth()
                                .height(BLOCK_HEIGHT)
                                .testTag(NARROW),
                        )
                    }
                }
            }
            waitForIdle()

            val container = onNodeWithTag(CONTAINER).getBoundsInRoot()
            val content = onNodeWithTag(CONTENT).boundsIn(container)
            val narrow = onNodeWithTag(NARROW).boundsIn(container)
            val title = onNodeWithText(TITLE).boundsIn(container)

            // Phones and tablets have the standard gutter on both sides of the centered column.
            val expectedStart = (CONTAINER_WIDTH - COLUMN_WIDTH) / 2
            assertNear(expectedStart, content.left, "content start")
            assertNear(COLUMN_WIDTH, content.right - content.left, "content width")
            assertNear(expectedStart, title.left, "title start")
            assertNear(expectedStart, narrow.left, "narrow card start")
            assertNear(NARROW_WIDTH, narrow.right - narrow.left, "narrow card width")
        }

    @Test
    fun aNarrowWindowGivesTheColumnEverythingButTheGutters() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    Box(Modifier.requiredWidth(PHONE_WIDTH).testTag(CONTAINER)) {
                        Box(
                            Modifier
                                .pageColumn(Dimens.medium)
                                .fillMaxWidth()
                                .height(BLOCK_HEIGHT)
                                .testTag(CONTENT),
                        )
                    }
                }
            }
            waitForIdle()

            val container = onNodeWithTag(CONTAINER).getBoundsInRoot()
            val content = onNodeWithTag(CONTENT).boundsIn(container)
            assertNear(Dimens.medium, content.left, "content start")
            assertNear(PHONE_WIDTH - Dimens.medium * 2, content.right - content.left, "content width")
        }

    private fun SemanticsNodeInteraction.boundsIn(container: DpRect): DpRect {
        val bounds = getBoundsInRoot()
        return DpRect(
            left = bounds.left - container.left,
            top = bounds.top - container.top,
            right = bounds.right - container.left,
            bottom = bounds.bottom - container.top,
        )
    }

    private fun assertNear(
        expected: Dp,
        actual: Dp,
        name: String,
    ) {
        assertTrue(abs((actual - expected).value) < 1f, "$name is $actual, expected $expected")
    }

    private companion object {
        const val CONTAINER = "container"
        const val CONTENT = "content"
        const val NARROW = "narrow"
        const val TITLE = "Title"
        val CONTAINER_WIDTH = 600.dp
        val PHONE_WIDTH = 360.dp
        val COLUMN_WIDTH = 300.dp
        val NARROW_WIDTH = 100.dp
        val BLOCK_HEIGHT = 10.dp
    }
}
