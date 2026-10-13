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

class GiveawayService(
    private val upstream: Upstream,
) {
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
        val response = upstream.giveaway(url)
        if (response.status.value == 404) return ApiResult(404)
        if (response.status.value == 429) return ApiResult(429)
        if (response.status.value !in 200..299) return ApiResult(502)
        if (!response.headers[HttpHeaders.ContentType].orEmpty().contains("application/json")) return ApiResult(502)
        val payload = Json.parseToJsonElement(response.body.decodeToString())
        val rewritten =
            when (payload) {
                is JsonArray -> JsonArray(payload.map { rewrite(it, requestBase) })
                else -> rewrite(payload, requestBase)
            }
        return ApiResult(200, rewritten.toString().encodeToByteArray())
    }

    suspend fun image(rawUrl: String?): Pair<ApiResult, String?> {
        val url = validImageUrl(rawUrl) ?: return ApiResult(400) to null
        val response =
            try {
                upstream.image(url)
            } catch (_: BodyTooLarge) {
                return ApiResult(502) to null
            }
        val contentType = response.headers[HttpHeaders.ContentType]
        val contentLength = response.headers[HttpHeaders.ContentLength]?.toLongOrNull()
        if (response.status.value !in 200..299 ||
            contentType?.startsWith("image/") != true ||
            (contentLength != null && contentLength > 5 * 1024 * 1024)
        ) {
            return ApiResult(502) to null
        }
        return ApiResult(200, response.body) to contentType
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
            },
        )
    }

    internal fun validImageUrl(value: String?): String? =
        try {
            val url = URI(value ?: return null).normalize()
            if (!url.scheme.equals("https", ignoreCase = true) ||
                !url.host.equals("www.gamerpower.com", ignoreCase = true) ||
                url.port !in listOf(-1, 443) ||
                !url.path.startsWith("/offers/") ||
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
