// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.domain.games

import dev.vladleesi.braindanceapp.domain.games.OverviewHighlight.PlayStyle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class OverviewHighlightsTest {
    @Test
    fun explicitModesAndCapabilitiesTakePriority() {
        val highlights =
            overviewHighlights(
                modes = listOf("Split-screen", "Single-player", "Co-operative", "Multiplayer", "Multiplayer"),
                themes = listOf("Survival"),
                perspectives = listOf("Third person"),
                genres = emptyList(),
                mainStorySeconds = 133_200,
            )
        assertEquals(
            listOf(PlayStyle.SOLO_AND_MULTIPLAYER, PlayStyle.CO_OP_AND_SPLIT_SCREEN, OverviewHighlight.MainStory(37)),
            highlights,
        )
    }

    @Test
    fun coOpDoesNotImplyOtherCapabilities() {
        assertEquals(
            listOf(PlayStyle.CO_OP),
            overviewHighlights(listOf("Co-op", "Unknown"), emptyList(), emptyList(), emptyList(), null),
        )
    }

    @Test
    fun onlyDistinctiveNonGenreThemesAndUnambiguousPerspectivesAreShown() {
        val themes = listOf("Action", "Fantasy", "Survival", "Stealth")
        val expected = listOf(OverviewHighlight.Theme("Stealth"), OverviewHighlight.Perspective("Third person"))
        for (order in listOf(themes, themes.reversed())) {
            assertEquals(
                expected,
                overviewHighlights(
                    emptyList(),
                    order,
                    listOf("Third person", "Third person"),
                    listOf("Survival"),
                    0,
                ),
            )
        }
        assertTrue(
            overviewHighlights(
                emptyList(),
                listOf("Action"),
                listOf("First person", "Third person"),
                listOf("Action"),
                -1,
            ).isEmpty(),
        )
    }

    @Test
    fun playtimeRoundsWithoutDisplayingZeroHours() {
        assertEquals(1L, roundedPlayHours(1))
        assertEquals(3L, roundedPlayHours(9_000))
        assertEquals(null, roundedPlayHours(0))
        assertEquals(null, roundedPlayHours(null))
    }
}
