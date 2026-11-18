// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GameKindTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun kindsFollowTheApiNames() {
        assertEquals(GameKind.EXPANDED_GAME, GameKind.fromApi("expanded_game"))
        assertEquals(GameKind.OTHER, GameKind.fromApi("a_kind_added_later"))
        assertEquals(GameKind.OTHER, GameKind.fromApi(null))
    }

    @Test
    fun aKindFromANewerApiDecodesAsOtherInsteadOfFailing() {
        val decoded =
            json.decodeFromString<List<FamilyGame>>(
                """[{"id":1,"name":"A","kind":"standalone_expansion"},""" +
                    """{"id":2,"name":"B","kind":"a_kind_added_later"},{"id":3,"name":"C"}]""",
            )
        assertEquals(GameKind.STANDALONE_EXPANSION, decoded[0].kind)
        assertEquals(GameKind.OTHER, decoded[1].kind)
        assertNull(decoded[2].kind)
    }

    @Test
    fun everyKindKeepsItsApiNameWhenWritten() {
        for (kind in GameKind.entries) {
            val text = json.encodeToString(GameKindSerializer, kind)
            assertEquals("\"${kind.apiName}\"", text)
            assertEquals(kind, json.decodeFromString(GameKindSerializer, text))
        }
    }
}
