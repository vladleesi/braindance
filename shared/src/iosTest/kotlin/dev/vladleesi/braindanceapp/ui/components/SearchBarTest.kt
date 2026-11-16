// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import X
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class SearchBarTest {
    @Test
    fun placeholderGivesWayToTheQueryAndSearchSubmitsIt() =
        runComposeUiTest {
            val submitted = mutableListOf<String>()
            setContent {
                BraindanceTheme {
                    var query by remember { mutableStateOf("") }
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = { submitted += it },
                        placeholder = PLACEHOLDER,
                    )
                }
            }

            onNodeWithText(PLACEHOLDER).assertIsDisplayed()
            val field = onNode(hasSetTextAction())
            field.performTextInput("Hollow Knight")
            onNodeWithText(PLACEHOLDER).assertDoesNotExist()
            field.performImeAction()

            assertEquals(listOf("Hollow Knight"), submitted)
        }

    @Test
    fun clearButtonIsANamedButtonThatEmptiesTheQuery() =
        runComposeUiTest {
            var query by mutableStateOf("Hades")
            setContent {
                BraindanceTheme {
                    SearchBar(
                        query = query,
                        onQueryChange = { query = it },
                        onSearch = {},
                        placeholder = PLACEHOLDER,
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                SearchFieldIcon(imageVector = X, label = CLEAR, onClick = { query = "" })
                            }
                        },
                    )
                }
            }

            onNodeWithContentDescription(CLEAR)
                .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
                .performClick()

            assertEquals("", query)
            onNodeWithText(PLACEHOLDER).assertIsDisplayed()
            onNodeWithContentDescription(CLEAR).assertDoesNotExist()
        }

    private companion object {
        const val PLACEHOLDER = "Search games"
        const val CLEAR = "Clear search"
    }
}
