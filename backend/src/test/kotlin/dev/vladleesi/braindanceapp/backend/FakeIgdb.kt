// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonObject
import java.util.concurrent.atomic.AtomicInteger

/**
 * In-memory IGDB stand-in for recommendation tests. It evaluates the APICalypse filter shapes that
 * [RecommendationQueries] emits using IGDB's documented semantics: `(a,b)` matches any value, `[a,b]` requires all
 * values, and `!= (a,b)` excludes any value. Results are sorted and limited like IGDB. It is not a general parser.
 */
internal class FakeIgdb(
    games: List<IgdbGame>,
    private val keywordNames: Map<Int, String> = emptyMap(),
) : Upstream {
    private val byId = games.associateBy { it.id }
    private val json = Json { explicitNulls = false }
    val igdbRequests = AtomicInteger()
    val bodies = mutableListOf<String>()

    /** Optional per-request override by zero-based IGDB request index. */
    var failures: Map<Int, HttpStatusCode> = emptyMap()

    /** When set, every IGDB request suspends until the gate completes. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun token(
        clientId: String,
        clientSecret: String,
    ) = UpstreamResponse(
        HttpStatusCode.OK,
        headersOf(HttpHeaders.ContentType, "application/json"),
        """{"access_token":"test-token","expires_in":3600}""".encodeToByteArray(),
    )

    override suspend fun igdb(
        path: String,
        query: String,
        clientId: String,
        token: String,
    ): UpstreamResponse {
        val index = igdbRequests.getAndIncrement()
        synchronized(bodies) { bodies += query }
        gate?.await()
        failures[index]?.let { return UpstreamResponse(it, headersOf(), byteArrayOf()) }
        check(path == RecommendationService.MULTIQUERY_PATH) { "Unexpected path $path" }
        val results =
            subqueries(query).map { (name, body) ->
                JsonObject(
                    mapOf(
                        "name" to JsonPrimitive(name),
                        "result" to run(name, body),
                    ),
                )
            }
        return UpstreamResponse(
            HttpStatusCode.OK,
            headersOf(HttpHeaders.ContentType, "application/json"),
            JsonArray(results).toString().encodeToByteArray(),
        )
    }

    override suspend fun giveaway(url: String) = error("unused")

    override suspend fun image(url: String) = error("unused")

    private fun subqueries(query: String): List<Pair<String, String>> =
        Regex(
            "query games \"([a-z]+)\" \\{(.*?)};",
        ).findAll(query).map { it.groupValues[1] to it.groupValues[2] }.toList()

    private fun run(
        name: String,
        body: String,
    ): JsonArray {
        val where =
            Regex("where (.*?);")
                .find(body)
                ?.groupValues
                ?.get(1)
                .orEmpty()
        val limit =
            Regex("limit ([0-9]+);")
                .find(body)
                ?.groupValues
                ?.get(1)
                ?.toInt() ?: 10
        var matches = byId.values.filter { matches(it, where) }.sortedBy { it.id }
        if (body.contains("sort total_rating_count desc")) {
            matches = matches.sortedByDescending { it.totalRatingCount ?: -1 }
        }
        return JsonArray(matches.take(limit).map { if (name == "source") expandedSource(it) else plain(it) })
    }

    private fun plain(game: IgdbGame): JsonObject = json.encodeToJsonElement(game).jsonObject

    private fun expandedSource(game: IgdbGame): JsonObject =
        JsonObject(
            plain(game) +
                mapOf(
                    "similar_games" to JsonArray(game.similarGames.mapNotNull(byId::get).map(::plain)),
                    "keywords" to
                        JsonArray(
                            game.keywords.map {
                                JsonObject(
                                    mapOf(
                                        "id" to JsonPrimitive(it),
                                        "name" to JsonPrimitive(keywordNames[it] ?: "kw$it"),
                                    ),
                                )
                            },
                        ),
                ),
        )

    private fun matches(
        game: IgdbGame,
        expression: String,
    ): Boolean = splitTopLevel(expression, " & ").all { clause(game, it.trim()) }

    private fun clause(
        game: IgdbGame,
        clause: String,
    ): Boolean {
        if (clause.startsWith("(") && closingParen(clause) == clause.lastIndex) {
            val inner = clause.substring(1, clause.lastIndex)
            return splitTopLevel(inner, " | ").any { matches(game, it.trim()) }
        }
        val match = checkNotNull(Regex("([a-z_]+) (!=|=|>) (.+)").matchEntire(clause)) { "Unsupported clause $clause" }
        val (field, operator, value) = match.destructured
        val actual = field(game, field)
        return when {
            value == "null" -> (actual == JsonNull) == (operator == "=")
            operator == ">" ->
                (actual as? JsonPrimitive)?.content?.toDoubleOrNull()?.let { it > value.toDouble() } ==
                    true
            else -> {
                val values =
                    value
                        .trim('(', ')', '[', ']')
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                val present = presentValues(actual)
                val result =
                    if (value.startsWith("[")) values.all { it in present } else values.any { it in present }
                if (operator == "=") result else !result
            }
        }
    }

    private fun presentValues(element: JsonElement?): Set<String> =
        when (element) {
            is JsonArray -> element.map { (it as JsonPrimitive).content }.toSet()
            is JsonPrimitive -> setOf(element.content)
            else -> emptySet()
        }

    private fun field(
        game: IgdbGame,
        name: String,
    ): JsonElement = plain(game)[name] ?: JsonNull

    private fun closingParen(text: String): Int {
        var depth = 0
        text.forEachIndexed { index, char ->
            if (char == '(') depth++
            if (char == ')' && --depth == 0) return index
        }
        return -1
    }

    private fun splitTopLevel(
        text: String,
        separator: String,
    ): List<String> {
        val parts = mutableListOf<String>()
        var depth = 0
        var start = 0
        var index = 0
        while (index < text.length) {
            when (text[index]) {
                '(', '[' -> depth++
                ')', ']' -> depth--
            }
            if (depth == 0 && text.startsWith(separator, index)) {
                parts += text.substring(start, index)
                index += separator.length
                start = index
            } else {
                index++
            }
        }
        parts += text.substring(start)
        return parts
    }
}
