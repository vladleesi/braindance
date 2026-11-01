// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.api.backendJson
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardLabel
import kotlin.test.Test
import kotlin.test.assertEquals

class TrendingCardMappingTest {
    @Test
    fun trendingFeedDecodesIntoLabeledCards() {
        // The exact shape served by /v1/games/trending, including a reason this client does not know yet.
        val feedJson =
            """[{"id":1,"name":"A","cover":{"url":"//images.igdb.com/igdb/image/upload/t_thumb/co1.jpg"},""" +
                """"reason":"new_release"},{"id":2,"name":"B","reason":"new_platform","platform":"PS5"},""" +
                """{"id":3,"name":"C","reason":"new_platform"},{"id":4,"name":"D","reason":"showcased"},""" +
                """{"id":5,"name":"E","reason":"full_release"},{"id":6,"name":"F","reason":"future_reason"},""" +
                """{"id":7,"name":"G"}]"""
        val cards = backendJson.decodeFromString<List<TrendingGameItem>>(feedJson).map { it.toMiniGameCardModel() }
        assertEquals(
            listOf(
                MiniGameCardLabel.NewRelease,
                MiniGameCardLabel.NewPlatform("PS5"),
                MiniGameCardLabel.NewPlatform(null),
                MiniGameCardLabel.Showcased,
                MiniGameCardLabel.FullRelease,
                null,
                null,
            ),
            cards.map { it.label },
        )
        assertEquals("https://images.igdb.com/igdb/image/upload/t_720p/co1.jpg", cards.first().backgroundImageUrl)
        assertEquals("", cards[1].backgroundImageUrl)
    }
}
