// SPDX-License-Identifier: MPL-2.0

// IGDB vocabulary IDs are documented where declared; field names repeat by design in APICalypse field lists.
@file:Suppress("MagicNumber", "StringLiteralDuplication")

package dev.vladleesi.braindanceapp.backend

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/** The subset of an IGDB game used for candidate filtering, scoring, and the response body. */
@Serializable
internal data class IgdbGame(
    val id: Int,
    val name: String? = null,
    val cover: IgdbCover? = null,
    @SerialName("game_type") val gameType: Int? = null,
    @SerialName("game_status") val gameStatus: Int? = null,
    @SerialName("version_parent") val versionParent: Int? = null,
    @SerialName("parent_game") val parentGame: Int? = null,
    val genres: List<Int> = emptyList(),
    val themes: List<Int> = emptyList(),
    val keywords: List<Int> = emptyList(),
    @SerialName("game_modes") val gameModes: List<Int> = emptyList(),
    @SerialName("player_perspectives") val playerPerspectives: List<Int> = emptyList(),
    val collections: List<Int> = emptyList(),
    val franchise: Int? = null,
    val franchises: List<Int> = emptyList(),
    @SerialName("first_release_date") val firstReleaseDate: Long? = null,
    @SerialName("total_rating") val totalRating: Double? = null,
    @SerialName("total_rating_count") val totalRatingCount: Int? = null,
    val hypes: Int? = null,
    @SerialName("similar_games") val similarGames: List<Int> = emptyList(),
)

@Serializable
internal data class IgdbCover(
    val url: String? = null,
)

/** The source game, its direct IGDB recommendations with metadata, and its source keyword names. */
internal data class SourceGraph(
    val source: IgdbGame,
    val keywordNames: Map<Int, String>,
    val direct: List<IgdbGame>,
    val reverse: List<IgdbGame>,
)

internal object RecommendationQueries {
    private val metadataFields =
        listOf(
            "name",
            "cover.url",
            "game_type",
            "game_status",
            "version_parent",
            "parent_game",
            "genres",
            "themes",
            "keywords",
            "game_modes",
            "player_perspectives",
            "collections",
            "franchise",
            "franchises",
            "first_release_date",
            "total_rating",
            "total_rating_count",
            "hypes",
            "similar_games",
        )
    private val candidateFields = metadataFields.joinToString(",")
    private val sourceFields =
        (
            metadataFields.filterNot { it == "similar_games" || it == "keywords" } +
                "keywords.name" +
                metadataFields.map { "similar_games.$it" }
        ).joinToString(",")

    /**
     * IGDB `game_types` IDs match the documented values of the deprecated `category` enum: main game 0, standalone
     * expansion 4, remake 8, remaster 9, expanded game 10. DLC, bundles, mods, episodes, seasons, ports, forks, packs,
     * and updates are not recommended. Missing types are kept so incomplete entries are not silently dropped.
     */
    val recommendableGameTypes = setOf(0, 4, 8, 9, 10)
    private val gameTypeFilter = "(game_type = (${recommendableGameTypes.joinToString(",")}) | game_type = null)"
    const val EROTIC_THEME = 42

    const val DIRECT_LIMIT = 10
    const val REVERSE_LIMIT = 30
    const val SECOND_DEGREE_LIMIT = 60
    const val SERIES_LIMIT = 20
    const val PROFILE_LIMIT = 50
    const val KEYWORD_LIMIT = 60
    const val MAX_KEYWORDS = 20
    private const val MAX_PROFILE_GENRES = 3

    /** Broad IGDB genres (Adventure, Indie, Arcade) and the Action theme match too much to constrain retrieval. */
    private val broadGenres = setOf(31, 32, 33)
    private val broadThemes = setOf(1)

    fun sourceQuery(id: Int): String =
        "query games \"source\" {fields $sourceFields;where id = $id;limit 1;};" +
            "query games \"reverse\" {fields $candidateFields;where similar_games = ($id) & version_parent = null " +
            "& $gameTypeFilter;sort total_rating_count desc;limit $REVERSE_LIMIT;};"

    /**
     * Builds bounded, independent candidate searches that run as one IGDB multiquery request. Popularity only orders
     * results inside each targeted metadata filter; ranking happens after all candidates are scored.
     */
    fun discoveryQuery(
        graph: SourceGraph,
        retrievalKeywords: List<Int>,
    ): String? {
        val source = graph.source
        val searchFilter =
            buildString {
                append("version_parent = null & $gameTypeFilter & total_rating_count > 0")
                if (EROTIC_THEME !in source.themes) append(" & themes != ($EROTIC_THEME)")
                append(" & id != ${source.id}")
            }
        val queries = mutableListOf<String>()
        val secondDegree = secondDegreeIds(graph)
        if (secondDegree.isNotEmpty()) {
            queries +=
                "query games \"second\" {fields $candidateFields;where id = (${secondDegree.joinToString(",")});" +
                "limit ${secondDegree.size};};"
        }
        val series =
            buildList {
                if (source.collections.isNotEmpty()) add("collections = (${source.collections.joinToString(",")})")
                val franchises = (source.franchises + listOfNotNull(source.franchise)).distinct().sorted()
                if (franchises.isNotEmpty()) {
                    add("franchises = (${franchises.joinToString(",")})")
                    add("franchise = (${franchises.joinToString(",")})")
                }
            }
        if (series.isNotEmpty()) {
            queries += search("series", "(${series.joinToString(" | ")}) & $searchFilter", SERIES_LIMIT)
        }
        profileFilter(source)?.let { queries += search("profile", "$it & $searchFilter", PROFILE_LIMIT) }
        if (retrievalKeywords.isNotEmpty()) {
            val genres =
                source.genres.sorted().takeIf { it.isNotEmpty() }?.let {
                    " & genres = (${it.joinToString(
                        ",",
                    )})"
                }
            queries +=
                search(
                    "keywords",
                    "keywords = (${retrievalKeywords.joinToString(",")})${genres.orEmpty()} & $searchFilter",
                    KEYWORD_LIMIT,
                )
        }
        return queries.takeIf { it.isNotEmpty() }?.joinToString("")
    }

    /** Games recommended by several direct or reverse neighbors are the most likely second-degree matches. */
    fun secondDegreeIds(graph: SourceGraph): List<Int> {
        val known = (graph.direct + graph.reverse).map { it.id }.toSet() + graph.source.id
        val links = mutableMapOf<Int, Int>()
        for (neighbor in graph.direct + graph.reverse) {
            for (id in neighbor.similarGames.distinct()) {
                if (id !in known) links[id] = (links[id] ?: 0) + 1
            }
        }
        return links.entries
            .sortedWith(compareByDescending<Map.Entry<Int, Int>> { it.value }.thenBy { it.key })
            .take(SECOND_DEGREE_LIMIT)
            .map { it.key }
            .sorted()
    }

    /** Requires every distinctive genre and at least one distinctive theme and perspective of the source. */
    private fun profileFilter(source: IgdbGame): String? {
        val genres =
            source.genres
                .filterNot { it in broadGenres }
                .ifEmpty { source.genres }
                .sorted()
        val themes = source.themes.filterNot { it in broadThemes || it == EROTIC_THEME }.sorted()
        val clauses =
            buildList {
                if (genres.isNotEmpty()) {
                    // IGDB [a,b] requires all values; with many genres an exact profile would be too narrow.
                    add(
                        if (genres.size <= MAX_PROFILE_GENRES) {
                            "genres = [${genres.joinToString(",")}]"
                        } else {
                            "genres = (${genres.joinToString(",")})"
                        },
                    )
                }
                if (themes.isNotEmpty()) add("themes = (${themes.joinToString(",")})")
                if (source.playerPerspectives.isNotEmpty()) {
                    add("player_perspectives = (${source.playerPerspectives.sorted().joinToString(",")})")
                }
            }
        // One clause is too broad unless it requires several distinctive genres, as for sparse indie entries.
        val distinctiveGenres = source.genres.count { it !in broadGenres }
        return clauses.takeIf { it.size >= 2 || distinctiveGenres >= 2 }?.joinToString(" & ")
    }

    private fun search(
        name: String,
        filter: String,
        limit: Int,
    ) = "query games \"$name\" {fields $candidateFields;where $filter;sort total_rating_count desc;limit $limit;};"

    private val json = Json { ignoreUnknownKeys = true }

    /** Parses a multiquery response into named result lists. Throws on malformed payloads. */
    fun parseMultiquery(body: ByteArray): Map<String, JsonArray> =
        json
            .parseToJsonElement(body.decodeToString())
            .jsonArray
            .associate { entry ->
                val item = entry.jsonObject
                item.getValue("name").jsonPrimitive.content to (item["result"]?.jsonArray ?: JsonArray(emptyList()))
            }

    fun parseGames(results: JsonArray?): List<IgdbGame> = results.orEmpty().mapNotNull(::parseGame)

    /** Returns null for an absent source game. Throws on malformed payloads. */
    fun parseSourceGraph(results: Map<String, JsonArray>): SourceGraph? {
        val element = results["source"]?.firstOrNull()?.jsonObject ?: return null
        val direct = element["similar_games"]?.jsonArray.orEmpty().mapNotNull(::parseGame)
        val keywordObjects = element["keywords"]?.jsonArray.orEmpty().map { it.jsonObject }
        val keywordNames =
            keywordObjects.associate { keyword ->
                keyword
                    .getValue("id")
                    .jsonPrimitive.content
                    .toInt() to
                    keyword["name"]?.jsonPrimitive?.content.orEmpty()
            }
        val flattened =
            JsonObject(
                element +
                    mapOf(
                        "similar_games" to JsonArray(direct.map { JsonPrimitive(it.id) }),
                        "keywords" to JsonArray(keywordNames.keys.map(::JsonPrimitive)),
                    ),
            )
        val source = json.decodeFromJsonElement<IgdbGame>(flattened)
        return SourceGraph(source, keywordNames, direct, parseGames(results["reverse"]))
    }

    /** Skips entries without a valid integer ID; other malformed fields throw so the caller can degrade safely. */
    private fun parseGame(element: JsonElement): IgdbGame? {
        val item = element as? JsonObject ?: return null
        (item["id"] as? JsonPrimitive)?.intOrNull ?: return null
        return json.decodeFromJsonElement<IgdbGame>(item)
    }
}
