// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.serialization.json.JsonArray
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PopularityTest {
    private val now = 1_800_000_000L

    private fun game(
        id: Int,
        released: Long? = 1_500_000_000,
        gameType: Int? = 0,
        versionParent: Int? = null,
        status: Int? = null,
        themes: List<Int> = emptyList(),
        ratingCategories: List<Int> = emptyList(),
        collections: List<Int> = emptyList(),
        name: String = "Game $id",
    ) = IgdbGame(
        id = id,
        name = name,
        gameType = gameType,
        versionParent = versionParent,
        gameStatus = status,
        firstReleaseDate = released,
        themes = themes,
        collections = collections,
        ageRatings = ratingCategories.map { IgdbAgeRating(it) },
    )

    /** A multiquery result with the given type names and, per type ID, games in value order. */
    private fun results(
        typeNames: Map<Int, String>,
        primitives: Map<Int, List<Pair<Int, Double>>>,
        calculatedAt: Long = now,
    ): Map<String, JsonArray> {
        val types = typeNames.entries.joinToString(",") { """{"id":${it.key},"name":"${it.value}"}""" }
        val lists =
            PopularitySignal.entries.joinToString(",") { signal ->
                val rows =
                    primitives[signal.typeId].orEmpty().joinToString(",") { (gameId, value) ->
                        """{"game_id":$gameId,"value":$value,"calculated_at":$calculatedAt}"""
                    }
                """{"name":"${signal.queryName}","result":[$rows]}"""
            }
        val body = """[{"name":"types","result":[$types]},$lists]"""
        return RecommendationQueries.parseMultiquery(body.encodeToByteArray())
    }

    private val typeNames =
        mapOf(1 to "Visits", 2 to "Want to Play", 3 to "Playing", 5 to "24hr Peak Players", 34 to "24hr Hours Watched")

    @Test
    fun signalsAreRankedByValueWithStableTies() {
        val signals =
            validPopularitySignals(
                results(typeNames, mapOf(1 to listOf(9 to 5.0, 4 to 5.0, 7 to 9.0))),
                now * 1000,
                listOf(PopularitySignal.IGDB_VISITS),
            )
        assertEquals(mapOf(PopularitySignal.IGDB_VISITS to listOf(7, 4, 9)), signals)
    }

    @Test
    fun signalsWithChangedMeaningMissingOrOldDataAreDropped() {
        val primitives = PopularitySignal.entries.associate { it.typeId to listOf(it.typeId to 1.0) }
        val all = PopularitySignal.entries

        fun valid(
            names: Map<Int, String> = typeNames,
            values: Map<Int, List<Pair<Int, Double>>> = primitives,
            calculatedAt: Long = now,
            signals: List<PopularitySignal> = all,
        ) = validPopularitySignals(results(names, values, calculatedAt), now * 1000, signals).keys
        assertEquals(all.toSet(), valid())
        // Type 3 no longer means Playing, so its list is not trusted.
        assertFalse(PopularitySignal.IGDB_PLAYING in valid(names = typeNames + (3 to "Played")))
        // Only requested signals are read; empty and week-old lists are dropped.
        assertEquals(setOf(PopularitySignal.IGDB_VISITS), valid(signals = listOf(PopularitySignal.IGDB_VISITS)))
        assertEquals(emptySet(), valid(values = emptyMap()))
        assertEquals(emptySet(), valid(calculatedAt = now - 8 * 86_400))
    }

    @Test
    fun signalQueriesListOnlyRequestedTypes() {
        val signals = listOf(PopularitySignal.IGDB_VISITS, PopularitySignal.IGDB_WANT_TO_PLAY)
        val query = popularitySignalQueries(200, signals)
        assertContains(query, "where id = (1,2);")
        assertContains(query, "query popularity_primitives \"want_to_play\" {fields game_id,value,calculated_at;")
        assertContains(query, "where popularity_type = 2;sort value desc;limit 200;")
        assertFalse("popularity_type = 34" in query)
    }

    @Test
    fun eligibilityKeepsReleasedMainGamesAndMatureRatings() {
        assertTrue(FeedEligibility.isReleased(game(1), now))
        assertFalse(FeedEligibility.isReleased(game(2, released = now + 1), now))
        assertFalse(FeedEligibility.isReleased(game(3, released = null), now))
        assertTrue(FeedEligibility.isReleased(game(4, released = null, status = 4), now), "undated early access")

        val listable =
            listOf(
                game(1),
                game(2, gameType = null),
                game(3, gameType = 8),
                // ESRB M with GRAC 19+ and ACB R 18+: mature, not adults-only.
                game(4, ratingCategories = listOf(6, 26, 38)),
            )
        assertTrue(listable.all(FeedEligibility::isListable))
        val unlisted =
            listOf(
                game(5, gameType = 1),
                game(6, versionParent = 1),
                game(7, themes = listOf(RecommendationQueries.EROTIC_THEME)),
                game(8, ratingCategories = listOf(7)),
                game(9, status = 6),
                game(10, status = 7),
            )
        assertTrue(unlisted.none(FeedEligibility::isListable))
        assertEquals("(first_release_date <= $now | game_status = 4)", FeedEligibility.releasedFilter(now))
    }

    @Test
    fun varietySkipsRepeatedTitlesAndThirdCollectionEntries() {
        val variety = FeedVariety()
        assertTrue(variety.accept(game(1, collections = listOf(77)), "Game 1"))
        assertTrue(variety.accept(game(2, collections = listOf(77)), "Game 2"))
        assertFalse(variety.accept(game(3, collections = listOf(77)), "Game 3"))
        assertFalse(variety.accept(game(4), "game 1"))
        assertTrue(variety.accept(game(5), "Game 5"))
    }
}
