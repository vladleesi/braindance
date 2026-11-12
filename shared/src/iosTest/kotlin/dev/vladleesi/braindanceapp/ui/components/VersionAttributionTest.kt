// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class VersionAttributionTest {
    @Test
    fun eachAttributionOpensItsOwnProvider() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            setContent {
                BraindanceTheme {
                    VersionAttribution(
                        versionLabel = "v1.0.0",
                        attributions =
                            listOf(
                                DataAttribution(label = "Game data by IGDB", linkText = "IGDB", onOpen = {
                                    opened +=
                                        "igdb"
                                }),
                                DataAttribution(
                                    label = "Giveaways by GamerPower",
                                    linkText = "GamerPower",
                                    onOpen = { opened += "gamerpower" },
                                ),
                            ),
                    )
                }
            }

            // The provider name is the link and sits at the end of each label; the prefix is plain text.
            onNodeWithText("Giveaways by GamerPower").performTouchInput { click(Offset(width - 2f, centerY)) }
            onNodeWithText("Game data by IGDB").performTouchInput { click(Offset(width - 2f, centerY)) }
            assertEquals(listOf("gamerpower", "igdb"), opened)

            onNodeWithText("Game data by IGDB").performTouchInput { click(Offset(2f, centerY)) }
            onNodeWithText("Giveaways by GamerPower").performTouchInput { click(Offset(2f, centerY)) }
            assertEquals(listOf("gamerpower", "igdb"), opened, "The prefix must not open a provider")
        }

    @Test
    fun narrowWidthKeepsEveryAttributionReachable() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    VersionAttribution(
                        versionLabel = "v1.0.0",
                        attributions =
                            listOf(
                                DataAttribution(label = "Game data by IGDB", linkText = "IGDB", onOpen = {}),
                                DataAttribution(
                                    label = "Giveaways by GamerPower",
                                    linkText = "GamerPower",
                                    onOpen = {},
                                ),
                            ),
                        modifier = Modifier.width(160.dp),
                    )
                }
            }

            onNodeWithText("v1.0.0").assertIsDisplayed()
            onNodeWithText("Game data by IGDB").assertIsDisplayed()
            onNodeWithText("Giveaways by GamerPower").assertIsDisplayed()
        }
}
