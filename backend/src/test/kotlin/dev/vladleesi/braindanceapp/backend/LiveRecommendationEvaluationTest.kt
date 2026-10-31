// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import kotlin.test.Test

/**
 * Opt-in comparison against live IGDB data. Requires real Twitch credentials and makes about 3 IGDB requests per
 * title through the in-memory limiter:
 *
 * ```sh
 * BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
 *   ./gradlew :backend:test --tests '*LiveRecommendationEvaluationTest*' --rerun -i
 * ```
 *
 * Expected-title patterns are a curated rubric of well-known close relatives, not exhaustive ground truth. A hit
 * means the result matches a pattern; a miss is not necessarily a bad recommendation. The test prints a report and
 * fails only if the `game_types` IDs assumed by [RecommendationQueries] no longer match IGDB.
 */
class LiveRecommendationEvaluationTest {
    private data class Case(
        val label: String,
        val search: String,
        val expected: Regex?,
    )

    private val cases =
        listOf(
            Case(
                "GTA VI",
                "Grand Theft Auto VI",
                Regex(
                    "Grand Theft Auto|Mafia|Sleeping Dogs|Watch Dogs|Saints Row|Red Dead|Just Cause|Crackdown|" +
                        "True Crime|Scarface|Yakuza|Like a Dragon|Cyberpunk 2077",
                ),
            ),
            Case(
                "The Witcher 3",
                "The Witcher 3: Wild Hunt",
                Regex(
                    "Witcher|Dragon Age|Kingdom Come|Elder Scrolls|Skyrim|Dragon's Dogma|Baldur's Gate|" +
                        "Cyberpunk 2077|Kingdoms of Amalur|GreedFall|Risen|Gothic|Elex|Mass Effect|Fable|Avowed|" +
                        "Middle-earth|Elden Ring",
                ),
            ),
            Case(
                "INSIDE",
                "Inside",
                Regex(
                    "Limbo|Little Nightmares|Somerville|Planet of Lana|Black The Fall|Stela|Unravel|Gris|" +
                        "Brothers: A Tale|Typoman|Out of Line|Bramble|Albert and Otto",
                ),
            ),
            Case(
                "Stardew Valley",
                "Stardew Valley",
                Regex(
                    "Harvest Moon|Story of Seasons|Rune Factory|Coral Island|Fields of Mistria|Sun Haven|My Time at|" +
                        "Graveyard Keeper|Animal Crossing|Littlewood|Roots of Pacha|Moonstone Island|Garden Story|" +
                        "Ooblets|Wylde Flowers|Potion Permit|Staxel|Slime Rancher|Mineko|Spiritfarer|Travellers Rest",
                ),
            ),
            Case(
                "Civilization VI",
                "Sid Meier's Civilization VI",
                Regex(
                    "Civilization|Humankind|Old World|Endless Legend|Endless Space|Stellaris|Galactic Civilizations|" +
                        "Age of Wonders|Ara: History Untold|Millennia|Master of Orion|Polytopia|Alpha Centauri",
                ),
            ),
            Case(
                "Multiplayer shooter",
                "Counter-Strike 2",
                Regex(
                    "Counter-Strike|Valorant|Rainbow Six|Insurgency|Call of Duty|Battlefield|Escape from Tarkov|" +
                        "Ready or Not|Squad|Overwatch|Apex Legends|Team Fortress|CrossFire|Splitgate|The Finals|" +
                        "Hell Let Loose|XDefiant|Spectre Divide|Ground Branch|Delta Force",
                ),
            ),
            Case(
                "Racing",
                "Forza Horizon 5",
                Regex(
                    "Forza|Need for Speed|The Crew|Test Drive|Burnout|Gran Turismo|GRID|Project CARS|DiRT|" +
                        "Assetto Corsa|Midnight Club|Driveclub|Motorfest",
                ),
            ),
        )

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun compareNativeAndHybridOnLiveData() {
        val clientId = System.getenv("TWITCH_CLIENT_ID")
        val clientSecret = System.getenv("TWITCH_CLIENT_SECRET")
        assumeTrue(
            System.getenv("BRAINDANCE_LIVE_EVAL") == "1" && !clientId.isNullOrBlank() && !clientSecret.isNullOrBlank(),
            "Set BRAINDANCE_LIVE_EVAL=1 and Twitch credentials",
        )
        val config = BackendConfig(clientId, clientSecret, null, emptySet(), 8080)
        val upstream =
            KtorUpstream(
                HttpClient(CIO) {
                    expectSuccess = false
                    install(HttpTimeout)
                },
            )
        val igdb = IgdbService(upstream, InMemoryRateLimiter(), config)
        try {
            runBlocking {
                verifyGameTypes(igdb)
                val resolved = cases.mapNotNull { case -> resolve(igdb, case.search)?.let { case to it } }
                val sparse = sparseGame(igdb)
                val report = StringBuilder()
                for (
                (case, id) in resolved +
                    listOfNotNull(
                        sparse?.let { Case("Sparse, no native", "", null) to it },
                    )
                ) {
                    report.append(evaluate(igdb, case, id))
                }
                println(report)
            }
        } finally {
            upstream.close()
        }
    }

    private suspend fun verifyGameTypes(igdb: IgdbService) {
        val body = igdb.execute("/v4/game_types", "fields type;limit 50;", SLOT_WAIT).body ?: error("game_types failed")
        val types =
            json.parseToJsonElement(body.decodeToString()).jsonArray.associate {
                it.jsonObject
                    .getValue("id")
                    .jsonPrimitive.int to
                    it.jsonObject
                        .getValue("type")
                        .jsonPrimitive.content
            }
        println("IGDB game_types: $types")
        val expected = mapOf(0 to "main", 4 to "standalone", 8 to "remake", 9 to "remaster", 10 to "expanded")
        for ((id, word) in expected) {
            check(types[id]?.lowercase()?.contains(word) == true) { "game_type $id is ${types[id]}, expected $word" }
        }
    }

    private suspend fun resolve(
        igdb: IgdbService,
        title: String,
    ): Int? {
        val query = "search \"$title\";fields name;where game_type = (0,8,9) & version_parent = null;limit 10;"
        val body = igdb.execute("/v4/games", query, SLOT_WAIT).body ?: return null
        val games = json.parseToJsonElement(body.decodeToString()).jsonArray.map { it.jsonObject }
        val match =
            games.firstOrNull { it["name"]?.jsonPrimitive?.content.equals(title, ignoreCase = true) }
                ?: games.firstOrNull()
        return match?.get("id")?.jsonPrimitive?.int
    }

    /** A rated main game without native similar games or keywords exercises the sparse-metadata path. */
    private suspend fun sparseGame(igdb: IgdbService): Int? {
        val query =
            "fields name;where similar_games = null & keywords = null & game_type = 0 & genres != null " +
                "& total_rating_count > 3;sort total_rating_count desc;limit 1;"
        val body = igdb.execute("/v4/games", query, SLOT_WAIT).body ?: return null
        return json
            .parseToJsonElement(
                body.decodeToString(),
            ).jsonArray
            .firstOrNull()
            ?.jsonObject
            ?.get("id")
            ?.jsonPrimitive
            ?.int
    }

    private suspend fun evaluate(
        igdb: IgdbService,
        case: Case,
        id: Int,
    ): String {
        val logs = mutableListOf<String>()
        val first = RecommendationService(igdb, log = { logs += it }).use { it.recommendations(id) }
        val second = RecommendationService(igdb, log = {}).use { it.recommendations(id) }
        val sourceBody =
            igdb
                .execute(RecommendationService.MULTIQUERY_PATH, RecommendationQueries.sourceQuery(id), SLOT_WAIT)
                .body
        val graph =
            sourceBody?.let {
                RecommendationQueries.parseSourceGraph(
                    RecommendationQueries.parseMultiquery(it),
                )
            }
        val sourceName = graph?.source?.name.orEmpty()
        val native = graph?.direct.orEmpty().map { it.name.orEmpty() }
        val hybrid = first.games.map { it.name }

        fun hits(
            names: List<String>,
            count: Int,
        ) = case.expected?.let { pattern -> names.take(count).count { pattern.containsMatchIn(it) } }?.toString()
            ?: "n/a"
        val contamination = Regex("edition|bundle|season pass|soundtrack|\\bdlc\\b|\\bpack\\b", RegexOption.IGNORE_CASE)
        val stable = first.games.take(10).map { it.id } == second.games.take(10).map { it.id }
        return buildString {
            appendLine("## ${case.label}: $sourceName ($id) — ${first.kind}, status ${first.status}")
            appendLine("event: ${logs.joinToString()}")
            appendLine(
                "native: n=${native.size} hits@10=${hits(
                    native,
                    10,
                )} contaminated=${native.count(contamination::containsMatchIn)}",
            )
            appendLine(
                "hybrid: n=${hybrid.size} hits@10=${hits(hybrid, 10)} hits@20=${hits(hybrid, 20)} " +
                    "contaminated=${hybrid.count(contamination::containsMatchIn)} stable_top10=$stable",
            )
            appendLine("native list: ${native.joinToString(" | ")}")
            appendLine("hybrid top 20: ${hybrid.take(20).joinToString(" | ")}")
            appendLine()
        }
    }

    private companion object {
        const val SLOT_WAIT = 2000L
    }
}
