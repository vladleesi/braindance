// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.ui.components.PreviewProjectLinks
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DesktopSidebarTest {
    @Test
    fun theExpandedSidebarCollapsesAndListsTheFooterLinks() =
        runComposeUiTest {
            var toggles = 0
            setContent { Sidebar(collapsed = false, onToggle = { toggles++ }) }

            onNodeWithText(PRIVACY).assertIsDisplayed()
            onNodeWithContentDescription(COLLAPSE).performClick()
            assertEquals(1, toggles)
        }

    @Test
    fun theCollapsedSidebarKeepsEveryDestinationButNotTheFooterLinks() =
        runComposeUiTest {
            var opened: BottomBarRoute? = null
            setContent { Sidebar(collapsed = true, onToggle = {}, onRouteClick = { opened = it }) }

            onNodeWithContentDescription(EXPAND).assertIsDisplayed()
            onNodeWithText(PRIVACY).assertDoesNotExist()
            onNodeWithText("Discover")
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
            onNodeWithText("Library").performClick()
            assertEquals(BottomBarRoute.LibraryRoute, opened)
        }

    @Test
    fun withoutRoomToExpandThereIsNoToggle() =
        runComposeUiTest {
            setContent { Sidebar(collapsed = true, onToggle = null) }

            onNodeWithContentDescription(EXPAND).assertDoesNotExist()
            onNodeWithContentDescription(COLLAPSE).assertDoesNotExist()
        }

    private companion object {
        const val PRIVACY = "Privacy"
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
    BraindanceTheme {
        DesktopSidebar(
            selectedRoute = BottomBarRoute.DiscoverRoute,
            isProfileSelected = false,
            onRouteClick = onRouteClick,
            links = PreviewProjectLinks,
            collapsed = collapsed,
            onToggleCollapsed = onToggle,
        )
    }
}
