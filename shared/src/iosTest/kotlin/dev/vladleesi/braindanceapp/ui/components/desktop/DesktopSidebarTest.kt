// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.system.ThemeMode
import dev.vladleesi.braindanceapp.ui.components.LocalOverlayHost
import dev.vladleesi.braindanceapp.ui.components.OverlayHost
import dev.vladleesi.braindanceapp.ui.components.OverlayHostLayout
import dev.vladleesi.braindanceapp.ui.components.PreviewProjectLinks
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DesktopSidebarTest {
    @Test
    fun theExpandedSidebarKeepsLinksInTheHelpMenu() =
        runComposeUiTest {
            var toggles = 0
            setContent { Sidebar(collapsed = false, onToggle = { toggles++ }) }

            onNodeWithText(PRIVACY).assertDoesNotExist()
            onNodeWithContentDescription("Dark").assertIsSelected()
            onNodeWithContentDescription("Light").performClick()
            onNodeWithContentDescription("Light").assertIsSelected()
            onNodeWithContentDescription(HELP).performClick()
            onNodeWithText(PRIVACY).assertIsDisplayed()
            onNodeWithText("Terms of use").assertIsDisplayed()
            onNodeWithText("Open-source licenses").assertIsDisplayed()
            onNodeWithText("Contact support").assertIsDisplayed()
            onNodeWithText("Game data by IGDB").assertIsDisplayed()
            onNodeWithText("Giveaways by GamerPower").assertIsDisplayed()
            onNodeWithText(PRIVACY).performClick()
            onNodeWithText(PRIVACY).assertDoesNotExist()
            onNodeWithContentDescription(COLLAPSE).performClick()
            assertEquals(1, toggles)
        }

    @Test
    fun theCollapsedSidebarKeepsNavigationAndHelp() =
        runComposeUiTest {
            var opened: BottomBarRoute? = null
            setContent { Sidebar(collapsed = true, onToggle = {}, onRouteClick = { opened = it }) }

            onNodeWithContentDescription(EXPAND).assertIsDisplayed()
            onNodeWithContentDescription("Theme (Dark)").performClick()
            onNodeWithText("System").performClick()
            onNodeWithContentDescription("Theme (System)").assertIsDisplayed()
            onNodeWithContentDescription(HELP).assertIsDisplayed()
            onNodeWithText(PRIVACY).assertDoesNotExist()
            onNodeWithText("Discover")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            onNodeWithText("Library").performClick()
            assertEquals(BottomBarRoute.LibraryRoute, opened)
        }

    @Test
    fun withoutRoomToExpandHelpIsStillAvailable() =
        runComposeUiTest {
            setContent { Sidebar(collapsed = true, onToggle = null) }

            onNodeWithContentDescription(EXPAND).assertDoesNotExist()
            onNodeWithContentDescription(COLLAPSE).assertDoesNotExist()
            onNodeWithContentDescription(HELP).performClick()
            onNodeWithText(PRIVACY).assertIsDisplayed()
        }

    private companion object {
        const val PRIVACY = "Privacy policy"
        const val HELP = "Help & links"
        const val COLLAPSE = "Collapse sidebar"
        const val EXPAND = "Expand sidebar"
    }
}

@Composable
private fun Sidebar(
    collapsed: Boolean,
    onToggle: (() -> Unit)?,
    onRouteClick: (BottomBarRoute) -> Unit = {},
) {
    val host = remember { OverlayHost() }
    var mode by remember { mutableStateOf(ThemeMode.Dark) }
    BraindanceTheme(darkTheme = mode != ThemeMode.Light) {
        CompositionLocalProvider(LocalOverlayHost provides host) {
            OverlayHostLayout(host) {
                DesktopSidebar(
                    selectedRoute = BottomBarRoute.DiscoverRoute,
                    isProfileSelected = false,
                    onRouteClick = onRouteClick,
                    themeMode = mode,
                    onChooseTheme = { mode = it },
                    links = PreviewProjectLinks,
                    collapsed = collapsed,
                    onToggleCollapsed = onToggle,
                )
            }
        }
    }
}
