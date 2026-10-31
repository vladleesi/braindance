// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.ln1p
import kotlin.math.pow
import kotlin.math.sqrt

/** A ranked recommendation with only the fields the client renders. */
internal data class Recommendation(
    val id: Int,
    val name: String,
    val coverUrl: String?,
    val score: Double,
)

/**
 * Deterministic hybrid ranking. Metadata similarity is the base signal; IGDB graph links, shared series, and
 * popularity amplify it only when the metadata already agrees, so they cannot lift an unrelated game to the top.
 * Feature weights use inverse document frequency over the candidate pool, which adapts to each source: features that
 * every candidate shares stop separating them, while rare shared traits dominate.
 */
internal object RecommendationRanking {
    const val MAX_RESULTS = 50

    // Metadata family weights; families the source lacks are dropped and the rest renormalized.
    private const val KEYWORD_WEIGHT = 0.37
    private const val THEME_WEIGHT = 0.22
    private const val GENRE_WEIGHT = 0.23
    private const val PERSPECTIVE_WEIGHT = 0.08
    private const val MODE_WEIGHT = 0.05
    private const val ERA_WEIGHT = 0.05

    // A candidate with no data in a family is uncertain, which is better than data that does not match.
    private const val MISSING_FAMILY_SIMILARITY = 0.15
    private const val ERA_DECAY_YEARS = 12.0
    private const val SECONDS_PER_YEAR = 31_557_600.0

    private const val METADATA_SHARE = 0.50
    private const val GRAPH_SHARE = 0.30
    private const val SERIES_SHARE = 0.12
    private const val SECONDARY_SHARE = 0.08

    // Graph evidence is combined as a noisy-OR of independent IGDB links.
    private const val DIRECT_LINK = 0.85
    private const val REVERSE_LINK = 0.60
    private const val NEIGHBOR_LINK = 0.18
    private const val MAX_NEIGHBOR_LINKS = 6

    // Metadata similarity at which graph, series, and popularity boosts reach full strength. The graph gate is
    // quadratic: an IGDB link confirms agreeing metadata but cannot carry a game whose gameplay profile differs.
    private const val GRAPH_GATE = 0.5
    private const val BOOST_GATE = 0.4

    private const val FRANCHISE_ONLY = 0.7
    private const val POPULAR_RATING_COUNT = 2000.0
    private const val POPULAR_HYPES = 200.0
    private const val NEUTRAL_QUALITY = 0.4
    private const val MIN_QUALITY_COUNT = 5
    private const val MISSING_COVER_PENALTY = 0.85

    private const val ABSOLUTE_MIN_SCORE = 0.2
    private const val RELATIVE_MIN_SCORE = 0.4
    private const val SERIES_REPEAT_PENALTY = 0.88

    // IGDB game_status values: cancelled 6, rumored 7.
    private const val CANCELLED_STATUS = 6
    private const val RUMORED_STATUS = 7
    private val cancelledOrRumored = setOf(CANCELLED_STATUS, RUMORED_STATUS)
    private const val POPULARITY_SHARE = 0.7
    private const val NEUTRAL_RATING = 50.0

    /**
     * Storefront, platform, and presentation keywords describe distribution rather than gameplay. They are excluded
     * from retrieval and carry no similarity weight. Matching is by source keyword name and is not game-specific.
     */
    private val genericKeyword =
        Regex(
            "achievement|trophies|steam|trading card|cloud sav|controller|digital distribution|soundtrack|" +
                "leaderboard|\\bdlc\\b|downloadable content|microtransaction|season pass|\\bpc\\b|xbox|playstation|" +
                "nintendo|\\bps[0-9]\\b|game pass|remote play|crossplay|cross-platform|cross platform|voice acting|" +
                "subtitles|localization|\\b4k\\b|\\bhdr\\b|ray tracing|fps\\b|e3 |gamescom|game awards|trailer|" +
                "unreal engine|\\bunity\\b|ported|available on|digital download|physical release|bonus content",
            RegexOption.IGNORE_CASE,
        )

    private val yearSuffix = Regex("\\((19|20)[0-9]{2}\\)")

    fun isGenericKeyword(name: String): Boolean = genericKeyword.containsMatchIn(name)

    /** Keywords shared with IGDB-similar neighbors are confirmed traits; generic keywords never drive retrieval. */
    fun retrievalKeywords(graph: SourceGraph): List<Int> {
        val neighbors = graph.direct + graph.reverse
        val distinctive =
            graph.source.keywords.distinct().filterNot {
                isGenericKeyword(
                    graph.keywordNames[it].orEmpty(),
                )
            }
        val support = distinctive.associateWith { keyword -> neighbors.count { keyword in it.keywords } }
        return distinctive
            .sortedWith(compareByDescending<Int> { support.getValue(it) }.thenBy { it })
            .take(RecommendationQueries.MAX_KEYWORDS)
            .sorted()
    }

    /** Ranks a pool produced by [candidatePool]; returns at most [limit] games above the relevance threshold. */
    fun rank(
        graph: SourceGraph,
        candidates: List<IgdbGame>,
        limit: Int = MAX_RESULTS,
    ): List<Recommendation> {
        val source = graph.source
        if (candidates.isEmpty()) return emptyList()
        val corpus = candidates + source
        val weights = FeatureWeights(corpus)
        val genericKeywords = source.keywords.filter { isGenericKeyword(graph.keywordNames[it].orEmpty()) }.toSet()
        val sourceKeywords = source.keywords.toSet() - genericKeywords
        val directIds = graph.direct.map { it.id }.toSet()
        // Links from filtered entries (editions, DLC) would count the source's own variants as votes.
        val poolIds = candidates.map { it.id }.toSet()
        val neighbors = (graph.direct + graph.reverse).filter { it.id in poolIds }
        val neighborIds = neighbors.map { it.id }.toSet()
        val scored =
            candidates.mapNotNull { candidate ->
                val comparable = candidate.copy(keywords = candidate.keywords.filterNot { it in genericKeywords })
                val metadata = metadataSimilarity(source, sourceKeywords, comparable, weights)
                val graphEvidence = graphEvidence(source, candidate, directIds, neighbors, neighborIds)
                val graphTerm = GRAPH_SHARE * graphEvidence * gate(metadata, GRAPH_GATE).pow(2)
                val boostGate = gate(metadata, BOOST_GATE)
                val seriesTerm = SERIES_SHARE * series(source, candidate) * boostGate
                val secondaryTerm = SECONDARY_SHARE * secondary(candidate) * boostGate
                val coverFactor = if (candidate.cover?.url.isNullOrBlank()) MISSING_COVER_PENALTY else 1.0
                val score = (METADATA_SHARE * metadata + graphTerm + seriesTerm + secondaryTerm) * coverFactor
                score.takeIf { it.isFinite() }?.let { candidate to it }
            }
        val best = scored.maxOfOrNull { it.second } ?: return emptyList()
        val threshold = maxOf(ABSOLUTE_MIN_SCORE, RELATIVE_MIN_SCORE * best)
        return diversify(scored.filter { it.second >= threshold }, limit.coerceIn(1, MAX_RESULTS))
    }

    /**
     * Removes the source, editions, non-game content, and duplicate entries. Duplicates share a title and release year,
     * so same-named remakes from other years remain; the lowest ID wins for determinism.
     */
    internal fun candidatePool(
        graph: SourceGraph,
        discovered: List<IgdbGame>,
    ): List<IgdbGame> {
        val source = graph.source
        val excludedIds = setOfNotNull(source.id, source.versionParent, source.parentGame)
        val allowErotic = RecommendationQueries.EROTIC_THEME in source.themes
        val unique = (graph.direct + graph.reverse + discovered).distinctBy { it.id }.sortedBy { it.id }
        val seenTitles = mutableSetOf<String>()
        return unique.filter { game ->
            val title = normalizedTitle(game.name.orEmpty())
            val year = game.firstReleaseDate?.let { (it / SECONDS_PER_YEAR).toInt() }
            game.id !in excludedIds &&
                title.isNotEmpty() &&
                game.versionParent == null &&
                (game.gameType == null || game.gameType in RecommendationQueries.recommendableGameTypes) &&
                game.gameStatus !in cancelledOrRumored &&
                (allowErotic || RecommendationQueries.EROTIC_THEME !in game.themes) &&
                seenTitles.add("$title:$year")
        }
    }

    internal fun normalizedTitle(name: String): String =
        name
            .lowercase()
            .replace(yearSuffix, "")
            .filter(Char::isLetterOrDigit)

    internal fun metadataSimilarity(
        source: IgdbGame,
        sourceKeywords: Set<Int>,
        candidate: IgdbGame,
        weights: FeatureWeights,
    ): Double {
        val families =
            listOfNotNull(
                family(KEYWORD_WEIGHT, sourceKeywords, candidate.keywords.toSet()) { a, b ->
                    weights.cosine(Family.KEYWORD, a, b)
                },
                family(THEME_WEIGHT, source.themes.toSet(), candidate.themes.toSet()) { a, b ->
                    weights.jaccard(Family.THEME, a, b)
                },
                family(GENRE_WEIGHT, source.genres.toSet(), candidate.genres.toSet()) { a, b ->
                    weights.jaccard(Family.GENRE, a, b)
                },
                family(PERSPECTIVE_WEIGHT, source.playerPerspectives.toSet(), candidate.playerPerspectives.toSet()) {
                    a,
                    b,
                    ->
                    weights.jaccard(Family.PERSPECTIVE, a, b)
                },
                family(MODE_WEIGHT, source.gameModes.toSet(), candidate.gameModes.toSet()) { a, b ->
                    weights.jaccard(Family.MODE, a, b)
                },
                eraSimilarity(source, candidate)?.let { ERA_WEIGHT to it },
            )
        val total = families.sumOf { it.first }
        if (total == 0.0) return 0.0
        return families.sumOf { (weight, similarity) -> weight * similarity } / total
    }

    private inline fun family(
        weight: Double,
        source: Set<Int>,
        candidate: Set<Int>,
        similarity: (Set<Int>, Set<Int>) -> Double,
    ): Pair<Double, Double>? =
        when {
            source.isEmpty() -> null
            candidate.isEmpty() -> weight to MISSING_FAMILY_SIMILARITY
            else -> weight to similarity(source, candidate)
        }

    private fun eraSimilarity(
        source: IgdbGame,
        candidate: IgdbGame,
    ): Double? {
        val sourceDate = source.firstReleaseDate ?: return null
        val candidateDate = candidate.firstReleaseDate ?: return MISSING_FAMILY_SIMILARITY
        return exp(-abs(sourceDate - candidateDate) / SECONDS_PER_YEAR / ERA_DECAY_YEARS)
    }

    private fun graphEvidence(
        source: IgdbGame,
        candidate: IgdbGame,
        directIds: Set<Int>,
        neighbors: List<IgdbGame>,
        neighborIds: Set<Int>,
    ): Double {
        val direct = if (candidate.id in directIds) 1 else 0
        val reverse = if (source.id in candidate.similarGames) 1 else 0
        val inbound = neighbors.count { it.id != candidate.id && candidate.id in it.similarGames }
        val outbound = candidate.similarGames.count { it != candidate.id && it in neighborIds }
        val links = (inbound + outbound).coerceAtMost(MAX_NEIGHBOR_LINKS)
        val miss = (1 - DIRECT_LINK).pow(direct) * (1 - REVERSE_LINK).pow(reverse) * (1 - NEIGHBOR_LINK).pow(links)
        return 1 - miss
    }

    private fun series(
        source: IgdbGame,
        candidate: IgdbGame,
    ): Double {
        if (source.collections.any { it in candidate.collections }) return 1.0
        val sourceFranchises = source.franchises + listOfNotNull(source.franchise)
        val candidateFranchises = candidate.franchises + listOfNotNull(candidate.franchise)
        return if (sourceFranchises.any { it in candidateFranchises }) FRANCHISE_ONLY else 0.0
    }

    /** Popularity and rating only break ties between comparably similar games. */
    private fun secondary(candidate: IgdbGame): Double {
        val ratings = ln1p((candidate.totalRatingCount ?: 0).toDouble()) / ln1p(POPULAR_RATING_COUNT)
        val hype = ln1p((candidate.hypes ?: 0).toDouble()) / ln1p(POPULAR_HYPES)
        val popularity = maxOf(ratings, hype).coerceIn(0.0, 1.0)
        val rating = candidate.totalRating
        val quality =
            if (rating != null && (candidate.totalRatingCount ?: 0) >= MIN_QUALITY_COUNT) {
                ((rating - NEUTRAL_RATING) / NEUTRAL_RATING).coerceIn(0.0, 1.0)
            } else {
                NEUTRAL_QUALITY
            }
        return POPULARITY_SHARE * popularity + (1 - POPULARITY_SHARE) * quality
    }

    private fun gate(
        metadata: Double,
        fullAt: Double,
    ) = (metadata / fullAt).coerceIn(0.0, 1.0)

    /** Greedy selection that lowers repeated entries from the same collection so one series cannot fill the list. */
    private fun diversify(
        scored: List<Pair<IgdbGame, Double>>,
        limit: Int,
    ): List<Recommendation> {
        val remaining =
            scored.sortedWith(
                compareByDescending<Pair<IgdbGame, Double>> { it.second }.thenBy { it.first.id },
            )
        val selected = mutableListOf<Recommendation>()
        val seriesCounts = mutableMapOf<Int, Int>()
        val pending = remaining.toMutableList()
        while (selected.size < limit && pending.isNotEmpty()) {
            var bestIndex = 0
            var bestScore = Double.NEGATIVE_INFINITY
            pending.forEachIndexed { index, (game, score) ->
                val repeats = game.collections.maxOfOrNull { seriesCounts[it] ?: 0 } ?: 0
                val adjusted = score * SERIES_REPEAT_PENALTY.pow(repeats)
                if (adjusted > bestScore) {
                    bestScore = adjusted
                    bestIndex = index
                }
            }
            val (game, _) = pending.removeAt(bestIndex)
            game.collections.forEach { seriesCounts[it] = (seriesCounts[it] ?: 0) + 1 }
            selected += Recommendation(game.id, checkNotNull(game.name), game.cover?.url, bestScore)
        }
        return selected
    }

    internal enum class Family { KEYWORD, THEME, GENRE, PERSPECTIVE, MODE }

    /** Smoothed IDF weights (`ln((n + 1) / (df + 1)) + 1`) computed over the source and candidate pool. */
    internal class FeatureWeights(
        corpus: List<IgdbGame>,
    ) {
        private val unseenWeight = ln(corpus.size + 1.0) + 1
        private val weights =
            Family.entries.associateWith { family ->
                val counts = mutableMapOf<Int, Int>()
                corpus.forEach { game ->
                    values(family, game).distinct().forEach { counts[it] = (counts[it] ?: 0) + 1 }
                }
                counts.mapValues { (_, frequency) -> ln((corpus.size + 1.0) / (frequency + 1.0)) + 1 }
            }

        fun weight(
            family: Family,
            feature: Int,
        ): Double = weights.getValue(family)[feature] ?: unseenWeight

        fun jaccard(
            family: Family,
            source: Set<Int>,
            candidate: Set<Int>,
        ): Double {
            val union = (source + candidate).sumOf { weight(family, it) }
            if (union == 0.0) return 0.0
            return source.intersect(candidate).sumOf { weight(family, it) } / union
        }

        fun cosine(
            family: Family,
            source: Set<Int>,
            candidate: Set<Int>,
        ): Double {
            val sourceNorm = sqrt(source.sumOf { weight(family, it).pow(2) })
            val candidateNorm = sqrt(candidate.sumOf { weight(family, it).pow(2) })
            if (sourceNorm == 0.0 || candidateNorm == 0.0) return 0.0
            return source.intersect(candidate).sumOf { weight(family, it).pow(2) } / (sourceNorm * candidateNorm)
        }

        private fun values(
            family: Family,
            game: IgdbGame,
        ): List<Int> =
            when (family) {
                Family.KEYWORD -> game.keywords
                Family.THEME -> game.themes
                Family.GENRE -> game.genres
                Family.PERSPECTIVE -> game.playerPerspectives
                Family.MODE -> game.gameModes
            }
    }
}
