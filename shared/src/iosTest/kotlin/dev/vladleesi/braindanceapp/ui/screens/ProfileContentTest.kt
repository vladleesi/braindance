// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.runComposeUiTest
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.ui.components.CommunityLinks
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ProfileContentTest {
    @Test
    fun everyRowOpensItsOwnDestination() =
        runComposeUiTest {
            val opened = mutableListOf<String>()
            setContent {
                BraindanceTheme {
                    ProfileContent(
                        links =
                            ProjectLinks(
                                shareLabel = Res.string.profile_support_share,
                                openRepository = { opened += "github" },
                                share = { opened += "share" },
                                openIgdb = { opened += "igdb" },
                                openGamerPower = { opened += "gamerpower" },
                            ),
                        community =
                            CommunityLinks(
                                openThreads = { opened += "threads" },
                                openX = { opened += "x" },
                                openReleases = { opened += "releases" },
                            ),
                        versionLabel = "v1.0.0",
                        onBackButtonPressed = {},
                    )
                }
            }

            listOf(
                "Star on GitHub",
                "Threads",
                "X",
                "Share",
                "Release notes",
                "Game data by IGDB",
                "Giveaways by GamerPower",
            ).forEach { title -> onNodeWithText(title).performScrollTo().performClick() }

            assertEquals(
                listOf("github", "threads", "x", "share", "releases", "igdb", "gamerpower"),
                opened,
            )
        }

    @Test
    fun showsTheVersionWithTheDevelopmentStage() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    ProfileContent(
                        links =
                            ProjectLinks(
                                shareLabel = Res.string.profile_support_share,
                                openRepository = {},
                                share = {},
                                openIgdb = {},
                                openGamerPower = {},
                            ),
                        community = CommunityLinks(openThreads = {}, openX = {}, openReleases = {}),
                        versionLabel = "v1.0.0",
                        onBackButtonPressed = {},
                    )
                }
            }

            onNodeWithText("v1.0.0").performScrollTo().assertIsDisplayed()
            onNodeWithText("In development").performScrollTo().assertIsDisplayed()
        }
}
