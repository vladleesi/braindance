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
                                emailSupport = { opened += "support" },
                                openPrivacyPolicy = { opened += "privacy" },
                                openLicenses = { opened += "licenses" },
                            ),
                        community =
                            CommunityLinks(
                                openThreads = { opened += "threads" },
                                openX = { opened += "x" },
                                openBluesky = { opened += "bluesky" },
                                openReddit = { opened += "reddit" },
                                openDiscord = { opened += "discord" },
                                openReleases = { opened += "releases" },
                            ),
                        versionLabel = "v1.0.0",
                        onBackButtonPressed = {},
                    )
                }
            }

            listOf(
                "Discord",
                "Reddit",
                "X",
                "Bluesky",
                "Threads",
                "Star on GitHub",
                "Share",
                "Contact support",
                "Privacy policy",
                "Open-source licenses",
                "Release notes",
                "Game data by IGDB",
                "Giveaways by GamerPower",
            ).forEach { title -> onNodeWithText(title).performScrollTo().performClick() }

            assertEquals(
                listOf(
                    "discord",
                    "reddit",
                    "x",
                    "bluesky",
                    "threads",
                    "github",
                    "share",
                    "support",
                    "privacy",
                    "licenses",
                    "releases",
                    "igdb",
                    "gamerpower",
                ),
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
                                emailSupport = {},
                                openPrivacyPolicy = {},
                                openLicenses = {},
                            ),
                        community =
                            CommunityLinks(
                                openThreads = {},
                                openX = {},
                                openBluesky = {},
                                openReddit = {},
                                openDiscord = {},
                                openReleases = {},
                            ),
                        versionLabel = "v1.0.0",
                        onBackButtonPressed = {},
                    )
                }
            }

            onNodeWithText("v1.0.0").performScrollTo().assertIsDisplayed()
            onNodeWithText("In development").performScrollTo().assertIsDisplayed()
        }
}
