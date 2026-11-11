// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Opt-in check of [EditionPreference] against live IGDB data. Requires real Twitch credentials and makes about ten
 * IGDB requests through the in-memory limiter:
 *
 * ```sh
 * BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
 *   ./gradlew :backend:test --tests '*LiveEditionPreferenceTest*' --rerun -i
 * ```
 *
 * It fails when the IGDB vocabulary IDs the rules assume change, or when Darksiders II no longer resolves to its
 * Deathinitive Edition. Other examples are printed, since IGDB editors may change their data.
 */
class LiveEditionPreferenceTest {
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun resolvesKnownEditionsOnLiveData() {
        val clientId = System.getenv("TWITCH_CLIENT_ID")
        val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")
        assumeTrue(
            System.getenv("BRAINDANCE_LIVE_EVAL") == "1" && !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank(),
            "Set BRAINDANCE_LIVE_EVAL=1 and Twitch credentials",
        )
        val config = BackendConfig(clientId, clientSecret, null, emptySet(), 8080)
        val upstream = KtorUpstream(HttpClient(CIO) { upstreamDefaults() })
        val igdb = IgdbService(upstream, InMemoryRateLimiter(), config)
        try {
            runBlocking {
                assertEquals(
                    mapOf(5 to "Offline", 6 to "Cancelled", 7 to "Rumored", 8 to "Delisted"),
                    names(igdb, "game_statuses", "status").filterKeys { it in 5..8 },
                )
                assertEquals(
                    mapOf(9 to "Remaster", 10 to "Expanded Game"),
                    names(igdb, "game_types", "type").filterKeys { it in 9..10 },
                )
                val storefronts = names(igdb, "website_types", "type")
                assertEquals(
                    listOf("Steam", "Epic", "GOG", "Xbox", "Playstation", "Nintendo"),
                    listOf(13, 16, 17, 22, 23, 24).map { storefronts[it] },
                )
                // Darksiders II, Darksiders, Metro: Last Light, Crysis 3, Final Doom.
                val replacements = resolve(igdb, listOf(1269, 1270, 539, 1268, 8191))
                println(
                    replacements.entries.joinToString("\n", "Edition replacements:\n") {
                        "${it.key} -> ${it.value.id} ${it.value.name}"
                    },
                )
                assertEquals(20048, replacements[1269]?.id)
                val recommendations = RecommendationService(igdb, log = ::println).use { it.recommendations(1270) }
                println("Darksiders recommendations: ${recommendations.games.map { "${it.id} ${it.name}" }}")
            }
        } finally {
            upstream.close()
        }
    }

    private suspend fun names(
        igdb: IgdbService,
        endpoint: String,
        field: String,
    ): Map<Int, String> {
        val body =
            igdb.execute("/v4/$endpoint", "fields $field;limit 50;", SLOT_WAIT).bodyOrNull()
                ?: error("$endpoint failed")
        return json.parseToJsonElement(body.decodeToString()).jsonArray.associate {
            val row = it.jsonObject
            row.getValue("id").jsonPrimitive.int to row.getValue(field).jsonPrimitive.content
        }
    }

    private suspend fun resolve(
        igdb: IgdbService,
        ids: List<Int>,
    ): Map<Int, IgdbGame> {
        val gamesQuery =
            "query games \"games\" {fields name,game_status,remasters,expanded_games;" +
                "where id = (${ids.joinToString(",")});limit ${ids.size};};"
        val games =
            RecommendationQueries.parseGames(
                RecommendationQueries.parseMultiquery(checkNotNull(execute(igdb, gamesQuery)))["games"],
            )
        val query = EditionPreference.query(games, RecommendationQueries.SUCCESSOR_FIELDS) ?: return emptyMap()
        val results = RecommendationQueries.parseMultiquery(checkNotNull(execute(igdb, query)))
        return EditionPreference.replacements(games, results, System.currentTimeMillis() / 1000) { true }
    }

    private suspend fun execute(
        igdb: IgdbService,
        query: String,
    ) = igdb.execute(RecommendationService.MULTIQUERY_PATH, query, SLOT_WAIT).bodyOrNull()

    private fun UpstreamOutcome.bodyOrNull() = (this as? UpstreamOutcome.Ok)?.body

    private companion object {
        const val SLOT_WAIT = 5_000L
    }
}
