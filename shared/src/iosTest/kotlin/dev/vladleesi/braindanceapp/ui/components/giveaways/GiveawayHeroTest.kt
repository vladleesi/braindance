// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.giveaways

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayEnding
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class GiveawayHeroTest {
    @Test
    fun anActiveGiveawayOpensFromItsActionAndSaysWhenItEndsSoon() =
        runComposeUiTest {
            var opened = 0
            setContent {
                BraindanceTheme {
                    GiveawayHero(
                        id = 1,
                        details = details(GiveawayEnding.EndsOn(date = LocalDate(2026, 11, 18), daysLeft = 1)),
                        expanded = false,
                        onGetGiveaway = { opened++ },
                    )
                }
            }

            onNodeWithText("Ends tomorrow").assertExists()
            onNodeWithText("Get giveaway").assertIsEnabled().performClick()
            assertEquals(1, opened)
        }

    @Test
    fun anEndedGiveawayDisablesItsActionAndSaysSo() =
        runComposeUiTest {
            var opened = 0
            setContent {
                BraindanceTheme {
                    val ended = GiveawayEnding.Ended(date = LocalDate(2026, 11, 10))
                    Column {
                        GiveawayHero(id = 1, details = details(ended), expanded = true, onGetGiveaway = { opened++ })
                        GiveawayFactsCard(
                            ending = ended,
                            claimedCount = 12_500,
                            addedOn = null,
                            platformNames = listOf("PC", "Epic Games Store"),
                        )
                    }
                }
            }

            onNodeWithText("Giveaway ended").assertIsNotEnabled().performClick()
            assertEquals(0, opened)
            // The hero's tag and the facts both say it is over; the facts also keep the day it ended.
            onAllNodesWithText("Ended", useUnmergedTree = true).assertCountEquals(2)
            onNodeWithText("Nov 10, 2026", useUnmergedTree = true).assertExists()
            onNodeWithText("12,500+ players", useUnmergedTree = true).assertExists()
            onNodeWithText("PC, Epic Games Store", useUnmergedTree = true).assertExists()
        }

    @Test
    fun aGiveawayWithoutALinkHasNoAction() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    GiveawayHero(
                        id = 1,
                        details = details(GiveawayEnding.OpenEnded),
                        expanded = false,
                        onGetGiveaway = null,
                    )
                }
            }

            onNodeWithText("Get giveaway").assertDoesNotExist()
        }

    private fun details(ending: GiveawayEnding) =
        GiveawayDetails(
            title = "Sample Giveaway",
            imageUrl = "",
            worth = "$4.99",
            type = "Game",
            claimedCount = 12_500,
            platforms = emptyList(),
            platformNames = listOf("PC"),
            description = "",
            claimSteps = emptyList(),
            ending = ending,
            addedOn = null,
            openUrl = "https://example.com",
            sourceUrl = null,
        )
}
