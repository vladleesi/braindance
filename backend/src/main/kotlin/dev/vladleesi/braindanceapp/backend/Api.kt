// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber", "StringLiteralDuplication")

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.host
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.port
import io.ktor.server.request.receiveChannel
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.readAvailable
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.ByteArrayOutputStream
import java.net.URI
import java.time.Instant

internal val gameQueryParameters =
    mapOf(
        "/v1/games/details" to setOf("id"),
        "/v1/games/anticipated" to setOf("currentTimestamp", "pageSize"),
        "/v1/games/popular" to setOf("ids", "pageSize"),
        "/v1/games/popularity" to setOf("type", "pageSize"),
    )

fun Application.api(
    config: BackendConfig,
    igdb: IgdbService,
    giveaways: GiveawayService,
) {
    installApiCachePolicy(config)
    installRequestTiming()
    routing {
        route("{...}") {
            handle {
                if (!call.hasValidOriginSecret(config)) {
                    respondJson(403, """{"error":"Forbidden"}""")
                    return@handle
                }
                val path = call.request.path()
                val method = call.request.httpMethod.value
                val origin =
                    call.request.headers
                        .getAll(HttpHeaders.Origin)
                        ?.singleOrNull()
                val allowed = origin?.let { isAllowedOrigin(it, config.allowedOrigins) } == true
                // Include absent and denied origins so downstream caches cannot reuse a different CORS variant.
                call.response.headers.append(HttpHeaders.Vary, "Origin")
                if (method == "OPTIONS" && (path.startsWith("/v1/") || path == "/health")) {
                    if (allowed) {
                        cors(origin)
                        call.respondText("", status = HttpStatusCode.NoContent)
                    } else {
                        call.respondText("", status = HttpStatusCode.Forbidden)
                    }
                    return@handle
                }
                if (allowed) cors(origin)
                try {
                    when {
                        path == "/health" && method == "GET" -> respondJson(200, """{"ok":true}""")
                        path == "/healthz" && method == "GET" -> {
                            if (config.credentialsAvailable) {
                                respondJson(200, """{"ok":true}""")
                            } else {
                                respondJson(503, """{"error":"Backend credentials unavailable"}""")
                            }
                        }
                        path in gameQueryParameters && method in setOf("GET", "POST") -> respondGame(path, igdb, config)
                        path.startsWith("/v1/giveaways") && method == "GET" -> {
                            if (path == "/v1/giveaways/image") {
                                respondImage(giveaways)
                            } else if (path == "/v1/giveaways" || validGiveawayId(path)) {
                                val port = call.request.port()
                                val suffix = if (port in listOf(80, 443)) "" else ":$port"
                                val requestBase = "${call.request.local.scheme}://${call.request.host()}$suffix"
                                val base = config.publicBaseUrl ?: requestBase
                                val result = giveaways.giveaway(path, base)
                                when (result.status) {
                                    200 -> {
                                        call.attributes.put(publicResponse, Unit)
                                        respondJson(200, checkNotNull(result.body).decodeToString())
                                    }
                                    404 -> respondJson(404, """{"error":"Giveaway not found"}""")
                                    429 -> respondJson(429, """{"error":"Rate limit exceeded"}""")
                                    else -> respondJson(502, """{"error":"Upstream unavailable"}""")
                                }
                            } else {
                                respondJson(404, """{"error":"Not found"}""")
                            }
                        }
                        else -> respondJson(404, """{"error":"Not found"}""")
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (_: Exception) {
                    respondJson(502, """{"error":"Upstream unavailable"}""")
                }
            }
        }
    }
}

private suspend fun RoutingContext.respondGame(
    path: String,
    igdb: IgdbService,
    config: BackendConfig,
) {
    val get = call.request.httpMethod.value == "GET"
    val contentType = call.request.headers[HttpHeaders.ContentType].orEmpty()
    if (!get && !contentType.startsWith("application/json")) {
        respondJson(415, """{"error":"Expected application/json"}""")
        return
    }
    if (!config.credentialsAvailable) {
        respondJson(503, """{"error":"Backend credentials unavailable"}""")
        return
    }
    val input =
        if (get) {
            gameInput(path) ?: run {
                respondJson(400, """{"error":"Invalid request"}""")
                return
            }
        } else {
            val body =
                try {
                    readRequest(2048)
                } catch (_: BodyTooLarge) {
                    respondJson(413, """{"error":"Query too large"}""")
                    return
                }
            try {
                Json.parseToJsonElement(body.decodeToString())
            } catch (_: Exception) {
                respondJson(400, """{"error":"Invalid JSON"}""")
                return
            }
        }
    val result =
        try {
            (input as? JsonObject)?.let { igdb.query(path, it) } ?: ApiResult(400)
        } catch (_: BodyTooLarge) {
            respondJson(413, """{"error":"Query too large"}""")
            return
        }
    when (result.status) {
        200 -> {
            if (get) {
                call.attributes.put(publicResponse, Unit)
            }
            respondJson(200, checkNotNull(result.body).decodeToString())
        }
        400 -> respondJson(400, """{"error":"Invalid request"}""")
        429 -> respondJson(429, """{"error":"Rate limit exceeded"}""")
        else -> respondJson(502, """{"error":"Upstream unavailable"}""")
    }
}

private fun RoutingContext.gameInput(path: String): JsonObject? {
    val parameters = call.request.queryParameters
    val names = checkNotNull(gameQueryParameters[path])
    if (call.request.uri.length > 2048 || parameters.entries().any { it.key !in names || it.value.size != 1 }) {
        return null
    }
    val input = mutableMapOf<String, JsonElement>()
    for ((name, values) in parameters.entries()) {
        if (name == "ids") {
            val ids = values.single().split(',')
            if (ids.isEmpty() || ids.size > 50) return null
            input[name] = JsonArray(ids.map { JsonPrimitive(it.toLongOrNull() ?: return null) })
        } else {
            input[name] = JsonPrimitive(values.single().toLongOrNull() ?: return null)
        }
    }
    // A stable feed URL lets different startups share an edge entry; explicit timestamps remain supported.
    if (path == "/v1/games/anticipated" && "currentTimestamp" !in input) {
        input["currentTimestamp"] = JsonPrimitive(Instant.now().epochSecond)
    }
    return JsonObject(input)
}

private suspend fun RoutingContext.respondImage(giveaways: GiveawayService) {
    val imageResult =
        try {
            giveaways.image(call.request.queryParameters["url"])
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            respondJson(502, """{"error":"Upstream image unavailable"}""")
            return
        }
    val (result, contentType) = imageResult
    when (result.status) {
        200 -> {
            call.attributes.put(publicResponse, Unit)
            call.respondBytes(
                checkNotNull(result.body),
                ContentType.parse(checkNotNull(contentType)),
                status = HttpStatusCode.OK,
            )
        }
        400 -> respondJson(400, """{"error":"Invalid image URL"}""")
        else -> respondJson(502, """{"error":"Upstream image unavailable"}""")
    }
}

internal fun isAllowedOrigin(
    origin: String,
    configured: Set<String>,
): Boolean =
    try {
        val url = URI(origin)
        url.scheme in setOf("http", "https") &&
            url.host != null &&
            url.userInfo == null &&
            url.path.isNullOrEmpty() &&
            url.query == null &&
            url.fragment == null &&
            origin in configured
    } catch (_: Exception) {
        false
    }

internal fun validGiveawayId(path: String): Boolean {
    val digits = Regex("^/v1/giveaways/([0-9]+)$").matchEntire(path)?.groupValues?.get(1) ?: return false
    return digits.toLongOrNull()?.let { it in 1..Int.MAX_VALUE.toLong() } == true
}

private fun RoutingContext.cors(origin: String) {
    call.response.headers.append("Access-Control-Allow-Origin", origin)
    call.response.headers.append("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
    call.response.headers.append("Access-Control-Allow-Headers", "Content-Type")
    call.response.headers.append("Access-Control-Max-Age", "86400")
}

private suspend fun RoutingContext.respondJson(
    status: Int,
    text: String,
) {
    call.respondText(
        text,
        ContentType.parse("application/json; charset=utf-8"),
        HttpStatusCode.fromValue(status),
    )
}

private suspend fun RoutingContext.readRequest(limit: Int): ByteArray {
    val channel = call.receiveChannel()
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(8192)
    while (true) {
        val count = channel.readAvailable(buffer)
        if (count == -1) break
        if (output.size() + count > limit) throw BodyTooLarge()
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
