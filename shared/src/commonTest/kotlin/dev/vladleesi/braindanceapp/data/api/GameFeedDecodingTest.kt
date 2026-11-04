// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api

import dev.vladleesi.braindanceapp.data.models.games.GameItem
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class GameFeedDecodingTest {
    @Test
    fun compactFeedCardsDecodeIntoGameItems() {
        // The exact shape served by /v1/games/anticipated.
        val feedJson =
            """[{"id":1942,"name":"The Witcher 3: Wild Hunt","cover":{"url":"//images.igdb.com/t_thumb/co1.jpg"}},""" +
                """{"id":7,"name":"No Cover"}]"""
        val games = backendJson.decodeFromString<List<GameItem>>(feedJson)
        assertEquals(listOf(1942, 7), games.map { it.id })
        assertEquals("//images.igdb.com/t_thumb/co1.jpg", games.first().cover?.url)
        assertNull(games.first().platforms)
        assertNull(games.last().cover)
    }

    @Test
    fun malformedJsonIsRejected() {
        // Unquoted keys and values were accepted while lenient parsing was on.
        assertFailsWith<SerializationException> { backendJson.decodeFromString<List<GameItem>>("[{id:1,name:A}]") }
    }
}
