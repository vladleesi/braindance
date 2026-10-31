// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Compares native `similar_games` with the hybrid engine on [SyntheticCatalog] using an explicit rubric. Results
 * describe the synthetic fixtures only; they do not measure live IGDB quality.
 */
class RecommendationEvaluationTest {
    private val config = BackendConfig("id", "secret", null, emptySet(), 8080)

    private data class Metrics(
        val precisionAt10: Double,
        val recallAt20: Double,
        val misordered: Int,
        val contaminated: Int,
        val results: Int,
    )

    private fun evaluate(
        scenario: SyntheticCatalog.Scenario,
        ranked: List<Int>,
    ): Metrics {
        val top10 = ranked.take(10)
        val expectedTop = minOf(10, scenario.relevant.size)
        val lastRelevant = ranked.indexOfLast { it in scenario.relevant }
        return Metrics(
            precisionAt10 = top10.count { it in scenario.relevant }.toDouble() / expectedTop,
            recallAt20 = ranked.take(20).count { it in scenario.relevant }.toDouble() / scenario.relevant.size,
            // An unrelated game placed above any relevant game is a ranking error.
            misordered = ranked.take(lastRelevant + 1).count { it in scenario.unrelated },
            contaminated = ranked.count { it in scenario.contaminated },
            results = ranked.size,
        )
    }

    private fun recommend(sourceId: Int): List<Int> =
        runBlocking {
            val igdb = FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames)
            val service = RecommendationService(IgdbService(igdb, RateLimiter { true }, config), log = ::println)
            try {
                val result = service.recommendations(sourceId)
                assertEquals(200, result.status)
                assertEquals(RecommendationResult.Kind.COMPLETE, result.kind)
                assertEquals(2, igdb.igdbRequests.get(), "IGDB requests per cache miss")
                result.games.map { it.id }
            } finally {
                service.close()
            }
        }

    @Test
    fun hybridRankingBeatsNativeSimilarGamesOnRubric() {
        val byId = SyntheticCatalog.games.associateBy { it.id }
        val report =
            StringBuilder(
                "scenario | native p@10 r@20 misordered contaminated n | hybrid p@10 r@20 misordered contaminated n\n",
            )
        for (scenario in SyntheticCatalog.scenarios) {
            val native = evaluate(scenario, byId.getValue(scenario.sourceId).similarGames)
            val hybrid = evaluate(scenario, recommend(scenario.sourceId))
            report.append(
                "${scenario.name} | ${native.format()} | ${hybrid.format()}\n",
            )
            assertTrue(hybrid.precisionAt10 >= native.precisionAt10, "${scenario.name}: $hybrid vs $native")
            assertTrue(hybrid.recallAt20 >= native.recallAt20, "${scenario.name}: $hybrid vs $native")
            assertEquals(0, hybrid.misordered, "${scenario.name}: unrelated game ranked above a relevant one")
            assertEquals(0, hybrid.contaminated, "${scenario.name}: edition or DLC returned")
        }
        println(report)
    }

    @Test
    fun expectedRelationshipsLeadTheirLists() {
        val crime = recommend(100)
        // Every crime sandbox outranks the hugely popular generic open-world game.
        assertTrue(crime.take(6).toSet() == setOf(101, 102, 103, 104, 105, 106), "crime top six: $crime")
        val farm = recommend(200).take(6)
        assertTrue(220 !in farm && 221 !in farm && 240 !in farm, "farm top six: $farm")
        val strategy = recommend(300)
        assertTrue(strategy.take(5).toSet() == setOf(301, 302, 303, 304, 305), "4X top five: $strategy")
        assertTrue(340 !in strategy.take(5) && 342 !in strategy, "4X: $strategy")
    }

    @Test
    fun sparseMetadataStillProducesRelatedResults() {
        val sparse = recommend(400)
        assertTrue(sparse.isNotEmpty() && sparse.containsAll(listOf(401, 402)), "sparse: $sparse")
    }

    @Test
    fun seriesFloodIsDiversified() {
        val sports = recommend(500).take(10)
        val outsideSeries = sports.count { it in setOf(520, 521, 522) }
        assertTrue(outsideSeries >= 3, "sports top ten: $sports")
    }

    @Test
    fun rankingIsDeterministicAndOrderIndependent() {
        val graph =
            runBlocking {
                val igdb = FakeIgdb(SyntheticCatalog.games, SyntheticCatalog.keywordNames)
                val service = IgdbService(igdb, RateLimiter { true }, config)
                val response =
                    service.execute(
                        RecommendationService.MULTIQUERY_PATH,
                        RecommendationQueries.sourceQuery(100),
                    )
                checkNotNull(
                    RecommendationQueries.parseSourceGraph(
                        RecommendationQueries.parseMultiquery(checkNotNull(response.body)),
                    ),
                )
            }
        val discovered = SyntheticCatalog.games.filter { it.id in 100..199 }
        val expected = RecommendationRanking.rank(graph, RecommendationRanking.candidatePool(graph, discovered))
        repeat(5) { seed ->
            val shuffled = discovered.shuffled(kotlin.random.Random(seed))
            val reversedGraph = graph.copy(direct = graph.direct.reversed(), reverse = graph.reverse.reversed())
            assertEquals(
                expected,
                RecommendationRanking.rank(reversedGraph, RecommendationRanking.candidatePool(reversedGraph, shuffled)),
            )
        }
    }

    private fun Metrics.format() =
        "%.2f %.2f %d %d %d".format(precisionAt10, recallAt20, misordered, contaminated, results)
}
