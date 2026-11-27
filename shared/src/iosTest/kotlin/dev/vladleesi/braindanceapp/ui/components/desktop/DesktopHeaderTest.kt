// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class DesktopHeaderTest {
    @Test
    fun typingSearchesAndEnterSubmitsTheSearch() =
        runComposeUiTest {
            var submitted = 0
            setContent {
                BraindanceTheme {
                    var text by remember { mutableStateOf("") }
                    DesktopHeader(
                        searchText = text,
                        onSearchTextChange = { text = it },
                        onSearch = { submitted++ },
                        onClearSearch = { text = "" },
                        onLeaveSearch = {},
                        onProfileClick = {},
                        isProfileSelected = false,
                    )
                }
            }

            onNode(hasSetTextAction()).performTextInput("zelda")
            onNode(hasSetTextAction()).performImeAction()

            onNode(hasSetTextAction()).assert(editableText("zelda"))
            assertEquals(1, submitted)
        }

    @Test
    fun clearingEmptiesTheFieldAndHidesTheClearButton() =
        runComposeUiTest {
            setContent {
                BraindanceTheme {
                    var text by remember { mutableStateOf("hollow") }
                    DesktopHeader(
                        searchText = text,
                        onSearchTextChange = { text = it },
                        onSearch = {},
                        onClearSearch = { text = "" },
                        onLeaveSearch = {},
                        onProfileClick = {},
                        isProfileSelected = false,
                    )
                }
            }

            onNodeWithContentDescription(CLEAR).performClick()

            onNode(hasSetTextAction()).assert(editableText(""))
            onNodeWithContentDescription(CLEAR).assertDoesNotExist()
        }

    @Test
    fun theEmptyFieldsShortcutHintIsHiddenFromScreenReaders() =
        runComposeUiTest {
            setContent { BraindanceTheme { HeaderWithDefaults() } }

            onNodeWithText("/").assertDoesNotExist()
        }

    @Test
    fun theAccountButtonOpensProfileAndShowsWhenProfileIsOpen() =
        runComposeUiTest {
            var opened = 0
            setContent { BraindanceTheme { HeaderWithDefaults(onProfileClick = { opened++ }, profileSelected = true) } }

            onNodeWithContentDescription(PROFILE)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Selected, true))
                .performClick()
            assertEquals(1, opened)
        }

    @Test
    fun notificationsAreAnnouncedButNotYetAvailable() =
        runComposeUiTest {
            setContent { BraindanceTheme { HeaderWithDefaults() } }

            onNodeWithContentDescription(NOTIFICATIONS).assertIsNotEnabled()
        }

    private companion object {
        const val CLEAR = "Clear search"
        const val PROFILE = "Profile"
        const val NOTIFICATIONS = "Notifications, coming soon"

        fun editableText(text: String) =
            SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(text))
    }
}

@Composable
private fun HeaderWithDefaults(
    onProfileClick: () -> Unit = {},
    profileSelected: Boolean = false,
) {
    DesktopHeader(
        searchText = "",
        onSearchTextChange = {},
        onSearch = {},
        onClearSearch = {},
        onLeaveSearch = {},
        onProfileClick = onProfileClick,
        isProfileSelected = profileSelected,
    )
}
