// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber", "StringLiteralDuplication", "ComplexCondition")

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive
import java.time.Clock

data class ApiResult(
    val status: Int,
    val body: ByteArray? = null,
)

class IgdbService(
    private val upstream: Upstream,
    private val rateLimiter: RateLimiter,
    private val config: BackendConfig,
    private val clock: Clock = Clock.systemUTC(),
) {
    private val mutex = Mutex()
    private var token: String? = null
    private var expiresAt = 0L

    suspend fun query(
        path: String,
        input: JsonObject,
    ): ApiResult {
        val query = buildQuery(path, input) ?: return ApiResult(400)
        return execute(query.first, query.second)
    }

    /**
     * Sends a validated APICalypse body to IGDB. Each attempt consumes one rate-limit slot; [slotWaitMillis] lets
     * internal callers briefly wait for a slot instead of failing immediately when a burst saturates the limit.
     */
    internal suspend fun execute(
        path: String,
        body: String,
        slotWaitMillis: Long = 0,
    ): ApiResult {
        var accessToken = getToken()
        repeat(2) { attempt ->
            if (!takeSlot(slotWaitMillis)) return ApiResult(429)
            val response = upstream.igdb(path, body, checkNotNull(config.clientId), accessToken)
            if (response.status.value == 401 && attempt == 0) {
                accessToken = getToken(rejectedToken = accessToken)
            } else {
                if (response.status.value == 429) return ApiResult(429)
                if (response.status.value !in 200..299) return ApiResult(502)
                val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
                if (!contentType.contains("application/json")) return ApiResult(502)
                return ApiResult(200, response.body)
            }
        }
        return ApiResult(502)
    }

    private suspend fun takeSlot(waitMillis: Long): Boolean {
        val deadline = clock.millis() + waitMillis
        while (true) {
            if (rateLimiter.takeSlot()) return true
            val remaining = deadline - clock.millis()
            if (remaining <= 0) return false
            delay(remaining.coerceAtMost(SLOT_RETRY_MILLIS))
        }
    }

    private suspend fun getToken(rejectedToken: String? = null): String =
        mutex.withLock {
            if (rejectedToken != null && token == rejectedToken) {
                token = null
                expiresAt = 0
            }
            token?.takeIf { clock.millis() < expiresAt }?.let { return@withLock it }
            val response = upstream.token(checkNotNull(config.clientId), checkNotNull(config.clientSecret))
            if (response.status.value !in 200..299) error("token_request_failed")
            val payload = Json.parseToJsonElement(response.body.decodeToString()) as? JsonObject
            val newToken =
                payload
                    ?.get("access_token")
                    ?.let { it as? JsonPrimitive }
                    ?.takeIf { it.isString }
                    ?.content
                    ?.takeIf(String::isNotEmpty)
            val expiry =
                payload
                    ?.get("expires_in")
                    ?.jsonPrimitive
                    ?.content
                    ?.toDoubleOrNull()
            if (newToken == null || expiry == null || !expiry.isFinite() || expiry <= 0) error("token_response_invalid")
            token = newToken
            expiresAt = clock.millis() + ((expiry - 60).coerceAtLeast(0.0) * 1000).toLong()
            newToken
        }

    private fun positive(
        value: kotlinx.serialization.json.JsonElement?,
        max: Long,
    ): Long? {
        val primitive = value as? JsonPrimitive ?: return null
        if (primitive.isString) return null
        val number = primitive.content.toDoubleOrNull() ?: return null
        if (!number.isFinite() || number <= 0 || number > max || number % 1.0 != 0.0) return null
        return number.toLong()
    }

    internal fun buildQuery(
        path: String,
        input: JsonObject,
    ): Pair<String, String>? {
        return when (path) {
            "/v1/games/details" -> {
                val id = positive(input["id"], Int.MAX_VALUE.toLong()) ?: return null
                // similar_games stays for released clients; current clients use /v1/games/recommendations.
                "/v4/games" to (
                    "fields name,cover.url,similar_games.name,similar_games.cover.url," +
                        "videos.video_id,genres.name,summary,storyline,platforms.name,websites.url," +
                        "websites.type,screenshots.animated,screenshots.url,first_release_date," +
                        "release_dates.human;where id = $id;limit 15;"
                )
            }
            "/v1/games/anticipated" -> {
                val timestamp = positive(input["currentTimestamp"], 4102444800) ?: return null
                val size = positive(input["pageSize"], 50) ?: return null
                "/v4/games" to (
                    "fields name,platforms.name,cover.url;" +
                        "where first_release_date > $timestamp & hypes > 0 & version_parent = null;" +
                        "sort hypes desc;limit $size;"
                )
            }
            "/v1/games/popular" -> {
                val size = positive(input["pageSize"], 50) ?: return null
                val ids = runCatching { input["ids"]?.jsonArray }.getOrNull() ?: return null
                if (ids.isEmpty() || ids.size > 50) return null
                val parsed = ids.map { positive(it, Int.MAX_VALUE.toLong()) ?: return null }
                "/v4/games" to (
                    "fields name,platforms.name,cover.url;" +
                        "where id = (${parsed.joinToString(", ")}) & age_ratings != null " +
                        "& age_ratings.rating_category != 7 & age_ratings.rating_category != 26 " +
                        "& age_ratings.rating_category != 38;sort hypes desc;limit $size;"
                )
            }
            "/v1/games/popularity" -> {
                if (positive(input["type"], 34) != 34L) return null
                val size = positive(input["pageSize"], 50) ?: return null
                "/v4/popularity_primitives" to
                    "fields game_id;where popularity_type = 34;sort value desc;limit $size;"
            }
            else -> null
        }
    }

    private companion object {
        const val SLOT_RETRY_MILLIS = 150L
    }
}
