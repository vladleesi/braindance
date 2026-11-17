// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data

import dev.vladleesi.braindanceapp.data.repository.SearchQuery
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/** The client must produce the API's canonical form, or equal searches would miss the shared caches. */
class SearchQueryTest {
    @Test
    fun textNormalizesToTheApiForm() {
        assertEquals("the witcher 3", SearchQuery.normalize("  The \t Witcher\n3  "))
        assertEquals("doom eternal", SearchQuery.normalize("\"Doom\\\u0000Eternal\""))
        assertEquals("pokémon", SearchQuery.normalize("POKÉMON"))
        assertEquals("ab", SearchQuery.normalize("ab"))
    }

    @Test
    fun textTooShortOrWithoutLettersAndDigitsIsNotSearched() {
        assertNull(SearchQuery.normalize(""))
        assertNull(SearchQuery.normalize("   "))
        assertNull(SearchQuery.normalize(" a "))
        assertNull(SearchQuery.normalize("?!"))
    }

    @Test
    fun longTextIsCutToTheApiLimitWithoutASplitCharacterOrTrailingSpace() {
        assertEquals("x".repeat(SearchQuery.MAX_LENGTH), SearchQuery.normalize("X".repeat(100)))
        assertEquals("x".repeat(63), SearchQuery.normalize("x".repeat(63) + " yz"), "no space where the cut falls")
        // An emoji is two UTF-16 units; one that would straddle the limit is dropped whole.
        val emoji = "🎮"
        assertEquals("x".repeat(63), SearchQuery.normalize("x".repeat(63) + emoji))
    }
}
