// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber", "StringLiteralDuplication")

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingContext
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.net.URI
import java.time.Instant

/** Test convenience: builds and owns its own recommendation and trending services. Production uses `Main.kt`. */
internal fun Application.api(
    config: BackendConfig,
    igdb: IgdbService,
    giveaways: GiveawayService,
) {
    val recommendations = RecommendationService(igdb)
    val trending = TrendingService(igdb)
    closeOnStop(recommendations, trending)
    api(config, igdb, giveaways, recommendations, trending)
}

// Each service is a separate dependency so tests can substitute clocks and upstreams per feature.
@Suppress("LongParameterList")
internal fun Application.api(
    config: BackendConfig,
    igdb: IgdbService,
    giveaways: GiveawayService,
    recommendations: RecommendationService,
    trending: TrendingService,
    failures: FailureLog = FailureLog(),
) {
    val backendVersion =
        BackendConfig::class.java.`package`.implementationVersion
            ?: checkNotNull(System.getProperty("backendVersion")) { "Backend build version is unavailable" }
    installApiCachePolicy(config)
    installRequestTiming()
    routing {
        route("{...}") {
            handle {
                if (!call.hasValidOriginSecret(config)) {
                    respondError(403, "Forbidden")
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
                val route = ApiRoute.forTemplate(routeOf(path))
                if (method != "GET" || route == null) {
                    respondError(404, "Not found")
                    return@handle
                }
                try {
                    when (route) {
                        ApiRoute.HEALTH ->
                            respondJson(
                                200,
                                JsonObject(
                                    mapOf("ok" to JsonPrimitive(true), "version" to JsonPrimitive(backendVersion)),
                                ).toString(),
                            )
                        ApiRoute.HEALTHZ ->
                            if (config.credentialsAvailable) {
                                respondJson(200, JsonObject(mapOf("ok" to JsonPrimitive(true))).toString())
                            } else {
                                respondError(503, "Backend credentials unavailable")
                            }
                        ApiRoute.GAME_DETAILS, ApiRoute.ANTICIPATED -> respondGame(route.template, igdb, config)
                        ApiRoute.RECOMMENDATIONS -> respondRecommendations(recommendations, config)
                        ApiRoute.TRENDING -> respondTrending(trending, config)
                        ApiRoute.GIVEAWAYS, ApiRoute.GIVEAWAY_DETAILS -> respondGiveaway(path, giveaways, config)
                        ApiRoute.GIVEAWAY_IMAGE -> respondImage(giveaways, failures)
                    }
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    failures.requestFailed(route.template, e)
                    respondError(502, UNAVAILABLE_MESSAGE)
                }
            }
        }
    }
}

private suspend fun RoutingContext.respondGiveaway(
    path: String,
    giveaways: GiveawayService,
    config: BackendConfig,
) {
    when (val result = giveaways.giveaway(path, config.publicBaseUrl)) {
        is UpstreamOutcome.Ok -> {
            call.attributes.put(publicResponse, Unit)
            respondJson(200, result.body.decodeToString())
        }
        is UpstreamOutcome.Failed -> respondFailure(result.failure, notFound = "Giveaway not found")
    }
}

private suspend fun RoutingContext.respondGame(
    path: String,
    igdb: IgdbService,
    config: BackendConfig,
) {
    if (!config.credentialsAvailable) {
        respondError(503, "Backend credentials unavailable")
        return
    }
    val input =
        gameInput(path) ?: run {
            respondError(400, "Invalid request")
            return
        }
    val result =
        try {
            igdb.query(path, input)
        } catch (_: BodyTooLarge) {
            respondError(413, "Query too large")
            return
        }
    when (result) {
        is UpstreamOutcome.Ok -> {
            call.attributes.put(publicResponse, Unit)
            respondJson(200, result.body.decodeToString())
        }
        is UpstreamOutcome.Failed -> respondFailure(result.failure)
    }
}

private suspend fun RoutingContext.respondRecommendations(
    recommendations: RecommendationService,
    config: BackendConfig,
) {
    val parameters = call.request.queryParameters
    val accepted = routeQueryParameters.getValue(RECOMMENDATIONS_PATH)
    val valid =
        call.request.uri.length <= 2048 &&
            parameters.entries().all { it.key in accepted && it.value.size == 1 }
    val id = parameters["id"]?.toIntOrNull()?.takeIf { valid && it > 0 }
    val limit =
        parameters["limit"]
            ?.let { value -> value.toIntOrNull()?.takeIf { it in 1..RecommendationRanking.MAX_RESULTS } ?: 0 }
            ?: RecommendationService.DEFAULT_LIMIT
    if (id == null || limit == 0) {
        respondError(400, "Invalid request")
        return
    }
    if (!config.credentialsAvailable) {
        respondError(503, "Backend credentials unavailable")
        return
    }
    val result = recommendations.recommendations(id)
    when (val failure = result.failure) {
        null -> {
            // Degraded and stale fallbacks stay private so edge caches do not extend a partial answer.
            if (result.kind == RecommendationResult.Kind.COMPLETE) call.attributes.put(publicResponse, Unit)
            respondJson(200, gameCardsJson(result.games.take(limit)))
        }
        else -> respondFailure(failure, notFound = "Game not found")
    }
}

/**
 * Serves one page of the Popular Right Now feed. Without parameters it is the first 20 games, so current clients
 * share one cache entry; `page` and `pageSize` accept only canonical positive integers, so each page has one URL.
 */
private suspend fun RoutingContext.respondTrending(
    trending: TrendingService,
    config: BackendConfig,
) {
    val parameters = call.request.queryParameters
    val accepted = routeQueryParameters.getValue(TRENDING_PATH)
    val valid =
        call.request.uri.length <= 2048 &&
            parameters.entries().all { it.key in accepted && it.value.size == 1 }
    val pageSize = if (valid) feedNumber(parameters["pageSize"], TrendingService.DEFAULT_PAGE_SIZE) else null
    val page = if (valid) feedNumber(parameters["page"], 1) else null
    val inRange =
        pageSize != null &&
            page != null &&
            pageSize <= TrendingService.MAX_PAGE_SIZE &&
            (page - 1).toLong() * pageSize < TrendingRanking.FEED_GAMES
    if (!inRange) {
        respondError(400, "Invalid request")
        return
    }
    if (!config.credentialsAvailable) {
        respondError(503, "Backend credentials unavailable")
        return
    }
    when (val result = trending.trending(page, pageSize)) {
        is ResponseCache.LoadResult.Hit -> {
            // A stale fallback stays private so edge caches do not extend it.
            if (!result.stale) call.attributes.put(publicResponse, Unit)
            respondJson(200, result.response.body.decodeToString())
        }
        is ResponseCache.LoadResult.Uncached -> respondFailure(result.failure)
    }
}

// Compiled once: both patterns run on every matching request.
private val feedNumberPattern = Regex("[1-9][0-9]{0,2}")
internal val giveawayIdPath = Regex("^/v1/giveaways/([0-9]+)$")

/** [default] when absent; a positive decimal without sign or leading zeros; otherwise null. */
private fun feedNumber(
    value: String?,
    default: Int,
): Int? {
    if (value == null) return default
    if (!feedNumberPattern.matches(value)) return null
    return value.toInt()
}

/** Matches the `id`, `name`, and `cover.url` shape of IGDB game objects already used by clients. */
internal fun gameCardsJson(games: List<Recommendation>): String =
    JsonArray(
        games.map { game ->
            JsonObject(
                buildMap {
                    put("id", JsonPrimitive(game.id))
                    put("name", JsonPrimitive(game.name))
                    game.coverUrl?.let { put("cover", JsonObject(mapOf("url" to JsonPrimitive(it)))) }
                },
            )
        },
    ).toString()

private fun RoutingContext.gameInput(path: String): JsonObject? {
    val parameters = call.request.queryParameters
    val names = checkNotNull(gameQueryParameters[path])
    if (call.request.uri.length > 2048 || parameters.entries().any { it.key !in names || it.value.size != 1 }) {
        return null
    }
    val input = mutableMapOf<String, JsonElement>()
    for ((name, values) in parameters.entries()) {
        input[name] = JsonPrimitive(values.single().toLongOrNull() ?: return null)
    }
    // A stable feed URL lets different startups share an edge entry. The timestamp is rounded to the in-process cache
    // lifetime so requests in one period share a cached response.
    if (path == "/v1/games/anticipated") {
        val periodSeconds = IgdbService.RESPONSE_TTL_MILLIS / 1000
        input["currentTimestamp"] = JsonPrimitive(Instant.now().epochSecond / periodSeconds * periodSeconds)
    }
    return JsonObject(input)
}

private suspend fun RoutingContext.respondImage(
    giveaways: GiveawayService,
    failures: FailureLog,
) {
    val imageResult =
        try {
            giveaways.image(call.request.queryParameters["url"])
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            failures.requestFailed("/v1/giveaways/image", e)
            respondError(502, IMAGE_UNAVAILABLE_MESSAGE)
            return
        }
    val (result, contentType) = imageResult
    when (result) {
        is UpstreamOutcome.Ok -> {
            call.attributes.put(publicResponse, Unit)
            call.respondBytes(
                result.body,
                ContentType.parse(checkNotNull(contentType) { "A proxied image always has a content type" }),
                status = HttpStatusCode.OK,
            )
        }
        is UpstreamOutcome.Failed ->
            respondFailure(
                result.failure,
                badRequest = "Invalid image URL",
                busy = "Image proxy busy",
                unavailable = IMAGE_UNAVAILABLE_MESSAGE,
            )
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
    val digits = giveawayIdPath.matchEntire(path)?.groupValues?.get(1) ?: return false
    return digits.toLongOrNull()?.let { it in 1..Int.MAX_VALUE.toLong() } == true
}

private fun RoutingContext.cors(origin: String) {
    call.response.headers.append("Access-Control-Allow-Origin", origin)
    call.response.headers.append("Access-Control-Allow-Methods", "GET, OPTIONS")
    call.response.headers.append("Access-Control-Allow-Headers", "Content-Type")
    call.response.headers.append("Access-Control-Max-Age", "86400")
}

private const val UNAVAILABLE_MESSAGE = "Upstream unavailable"
private const val IMAGE_UNAVAILABLE_MESSAGE = "Upstream image unavailable"

/**
 * The one mapping from an upstream failure to the API's status and error body. Routes only name their own items, such
 * as which thing was not found; statuses never differ between routes.
 */
private suspend fun RoutingContext.respondFailure(
    failure: UpstreamFailure,
    badRequest: String = "Invalid request",
    notFound: String = "Not found",
    busy: String = "Service busy",
    unavailable: String = UNAVAILABLE_MESSAGE,
) {
    when (failure) {
        UpstreamFailure.BAD_REQUEST -> respondError(400, badRequest)
        UpstreamFailure.NOT_FOUND -> respondError(404, notFound)
        UpstreamFailure.RATE_LIMITED -> respondError(429, "Rate limit exceeded")
        UpstreamFailure.BUSY -> respondError(503, busy)
        // An upstream that did not answer in time is reported as unavailable, like any other upstream failure.
        UpstreamFailure.UNAVAILABLE, UpstreamFailure.TIMEOUT -> respondError(502, unavailable)
    }
}

/** A `{"error": message}` body, serialized so a message can never break the JSON. */
private suspend fun RoutingContext.respondError(
    status: Int,
    message: String,
) {
    respondJson(status, JsonObject(mapOf("error" to JsonPrimitive(message))).toString())
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
