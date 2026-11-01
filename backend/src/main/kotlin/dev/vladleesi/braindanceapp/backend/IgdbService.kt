// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber", "StringLiteralDuplication", "ComplexCondition")

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpHeaders
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
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

    // IGDB allows eight open requests; the rate limiter alone admits more when IGDB responds slowly.
    private val openRequests = Semaphore(MAX_OPEN_REQUESTS)

    // Game queries are public and identical for every visitor, so the validated query is a complete cache key.
    private val responses = ResponseCache(RESPONSE_TTL_MILLIS, RESPONSE_CACHE_BYTES, MAX_CACHED_RESPONSE_BYTES, clock)

    suspend fun query(
        path: String,
        input: JsonObject,
    ): ApiResult {
        val (upstreamPath, body) = buildQuery(path, input) ?: return ApiResult(400)
        val result =
            responses.getOrLoad("$upstreamPath\n$body") {
                val response = execute(upstreamPath, body)
                if (response.status == 200) {
                    ResponseCache.LoadResult.Hit(CachedResponse(checkNotNull(response.body)))
                } else {
                    ResponseCache.LoadResult.Uncached(response.status)
                }
            }
        return when (result) {
            is ResponseCache.LoadResult.Hit -> ApiResult(200, result.response.body)
            is ResponseCache.LoadResult.Uncached -> ApiResult(result.status)
        }
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
            val response =
                openRequests.withPermit { upstream.igdb(path, body, checkNotNull(config.clientId), accessToken) }
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
                "/v4/games" to (
                    "fields name,cover.url,videos.video_id,genres.name,summary,storyline,platforms.name,websites.url," +
                        "websites.type,screenshots.animated,screenshots.url,first_release_date," +
                        "release_dates.human;where id = $id;limit 15;"
                )
            }
            "/v1/games/anticipated" -> {
                val timestamp = positive(input["currentTimestamp"], 4102444800) ?: return null
                val size = positive(input["pageSize"], 50) ?: return null
                // Cards render only name and cover. Null-safe clauses keep games whose status or themes are missing.
                "/v4/games" to (
                    "fields name,cover.url;" +
                        "where first_release_date > $timestamp & hypes > 0 & version_parent = null & cover != null" +
                        " & (game_status = null | game_status != 6) & (themes = null | themes != (42));" +
                        "sort hypes desc;limit $size;"
                )
            }
            else -> null
        }
    }

    companion object {
        /** In-process lifetime of successful game responses; shorter than the 20-minute shared edge lifetime. */
        const val RESPONSE_TTL_MILLIS = 10 * 60 * 1000L
        private const val RESPONSE_CACHE_BYTES = 16L * 1024 * 1024
        private const val MAX_CACHED_RESPONSE_BYTES = 256 * 1024
        private const val MAX_OPEN_REQUESTS = 8
        private const val SLOT_RETRY_MILLIS = 150L
    }
}
