// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.runComposeUiTest
import dev.vladleesi.braindanceapp.ui.screens.ErrorScreen
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class BackOnlyFrameTest {
    @Test
    fun aFailedLoadKeepsAWayBackAndItsRetry() =
        runComposeUiTest {
            var backs = 0
            var retries = 0
            setContent {
                BraindanceTheme {
                    BackOnlyFrame(onBack = { backs++ }) { modifier ->
                        ErrorScreen(errorMessage = "Couldn't load this game", modifier = modifier) { retries++ }
                    }
                }
            }

            onNodeWithText("Couldn't load this game").assertIsDisplayed()
            onNodeWithContentDescription("Back").performClick()
            onNodeWithText("Retry").performClick()

            assertEquals(1, backs)
            assertEquals(1, retries)
        }
}
