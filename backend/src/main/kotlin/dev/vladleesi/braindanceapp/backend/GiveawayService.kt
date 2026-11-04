// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber", "ComplexCondition")

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpHeaders
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.net.URI
import java.time.Clock

class GiveawayService(
    private val upstream: Upstream,
    clock: Clock = Clock.systemUTC(),
) {
    // Rewritten links depend on the request base, so it is part of the key; production uses one fixed base.
    private val giveaways = ResponseCache(CACHE_TTL_MILLIS, GIVEAWAY_CACHE_BYTES, MAX_GIVEAWAY_BYTES, clock)
    private val images = ResponseCache(CACHE_TTL_MILLIS, IMAGE_CACHE_BYTES, MAX_CACHED_IMAGE_BYTES, clock)

    suspend fun giveaway(
        path: String,
        requestBase: String,
    ): ApiResult {
        val url =
            if (path == "/v1/giveaways") {
                "https://www.gamerpower.com/api/giveaways"
            } else {
                val id =
                    Regex("^/v1/giveaways/(\\d+)$")
                        .matchEntire(path)
                        ?.groupValues
                        ?.get(1)
                        ?.toLongOrNull()
                        ?.takeIf { it in 1..Int.MAX_VALUE.toLong() } ?: return ApiResult(404)
                "https://www.gamerpower.com/api/giveaway?id=$id"
            }
        val result = giveaways.getOrLoad("$url\n$requestBase") { fetchGiveaway(url, requestBase) }
        return when (result) {
            is ResponseCache.LoadResult.Hit -> ApiResult(200, result.response.body)
            is ResponseCache.LoadResult.Uncached -> ApiResult(result.status)
        }
    }

    private suspend fun fetchGiveaway(
        url: String,
        requestBase: String,
    ): ResponseCache.LoadResult {
        val response = upstream.giveaway(url)
        val status = response.status.value
        val contentType = response.headers[HttpHeaders.ContentType].orEmpty()
        return when {
            status == 404 || status == 429 -> ResponseCache.LoadResult.Uncached(status)
            status !in 200..299 || !contentType.contains("application/json") -> ResponseCache.LoadResult.Uncached(502)
            else -> {
                val rewritten =
                    when (val payload = Json.parseToJsonElement(response.body.decodeToString())) {
                        is JsonArray -> JsonArray(payload.map { rewrite(it, requestBase) })
                        else -> rewrite(payload, requestBase)
                    }
                ResponseCache.LoadResult.Hit(CachedResponse(rewritten.toString().encodeToByteArray()))
            }
        }
    }

    suspend fun image(rawUrl: String?): Pair<ApiResult, String?> {
        val url = validImageUrl(rawUrl) ?: return ApiResult(400) to null
        return when (val result = images.getOrLoad(url) { fetchImage(url) }) {
            is ResponseCache.LoadResult.Hit -> ApiResult(200, result.response.body) to result.response.contentType
            is ResponseCache.LoadResult.Uncached -> ApiResult(result.status) to null
        }
    }

    private suspend fun fetchImage(url: String): ResponseCache.LoadResult {
        val response =
            try {
                upstream.image(url)
            } catch (_: BodyTooLarge) {
                return ResponseCache.LoadResult.Uncached(502)
            }
        val contentType = response.headers[HttpHeaders.ContentType]
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (response.status.value !in 200..299 ||
            !isAllowedImageType(contentType) ||
            (contentLength != null && contentLength > 5 * 1024 * 1024)
        ) {
            return ResponseCache.LoadResult.Uncached(502)
        }
        return ResponseCache.LoadResult.Hit(CachedResponse(response.body, contentType))
    }

    private fun rewrite(
        element: JsonElement,
        base: String,
    ): JsonElement {
        val obj = element as? JsonObject ?: return element
        return JsonObject(
            obj.toMutableMap().apply {
                for (field in listOf("image", "thumbnail")) {
                    val original = (obj[field] as? JsonPrimitive)?.contentOrNull ?: continue
                    val validated = validImageUrl(original) ?: continue
                    val encoded = java.net.URLEncoder.encode(validated, Charsets.UTF_8)
                    this[field] = JsonPrimitive("$base/v1/giveaways/image?url=$encoded")
                }
                // Clients open these links outside the app, so anything but an absolute HTTPS link is dropped,
                // including javascript: and data: links from upstream data.
                for (field in linkFields) {
                    val original = obj[field] ?: continue
                    val link = (original as? JsonPrimitive)?.takeIf { it.isString }?.content
                    if (safeHttpsLink(link) == null) remove(field)
                }
            },
        )
    }

    private companion object {
        val linkFields = listOf("open_giveaway_url", "open_giveaway", "gamerpower_url")
        val encodedPathSeparators = Regex("%(2e|2f|5c)", RegexOption.IGNORE_CASE)
        const val CACHE_TTL_MILLIS = 10 * 60 * 1000L
        const val GIVEAWAY_CACHE_BYTES = 8L * 1024 * 1024
        const val MAX_GIVEAWAY_BYTES = 2 * 1024 * 1024
        const val IMAGE_CACHE_BYTES = 16L * 1024 * 1024
        const val MAX_CACHED_IMAGE_BYTES = 1024 * 1024
    }

    internal fun validImageUrl(value: String?): String? =
        try {
            val url = URI(value ?: return null).normalize()
            // The raw path is what is forwarded, so it is the one checked. Encoded dots and slashes could otherwise
            // pass as /offers/ here and resolve to another path upstream.
            val rawPath = url.rawPath.orEmpty()
            if (!url.scheme.equals("https", ignoreCase = true) ||
                !url.host.equals("www.gamerpower.com", ignoreCase = true) ||
                url.port !in listOf(-1, 443) ||
                !rawPath.startsWith("/offers/") ||
                encodedPathSeparators.containsMatchIn(rawPath) ||
                url.userInfo != null
            ) {
                null
            } else {
                url.toASCIIString()
            }
        } catch (_: Exception) {
            null
        }
}

/** [value] when it is an absolute HTTPS link with a host and no credentials, otherwise null. */
internal fun safeHttpsLink(value: String?): String? =
    try {
        val url = URI(value?.trim() ?: return null)
        value.takeIf {
            url.scheme.equals("https", ignoreCase = true) &&
                !url.host.isNullOrEmpty() &&
                url.userInfo == null
        }
    } catch (_: Exception) {
        null
    }

private val allowedImageTypes = setOf("image/jpeg", "image/png", "image/webp", "image/gif", "image/avif")

/**
 * Whether [contentType] is a raster image type the proxy may serve. SVG and other scriptable or unusual types are
 * refused, so the proxy does not depend on response headers to neutralise them.
 */
internal fun isAllowedImageType(contentType: String?): Boolean =
    contentType
        ?.substringBefore(';')
        ?.trim()
        ?.lowercase() in allowedImageTypes
