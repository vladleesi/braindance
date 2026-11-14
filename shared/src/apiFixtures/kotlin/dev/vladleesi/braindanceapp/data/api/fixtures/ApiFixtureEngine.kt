// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.fixtures

import dev.vladleesi.braindanceapp.data.config.ApiConfig
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockEngineConfig
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.request.HttpRequestData
import io.ktor.client.request.HttpResponseData
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/** The API and image host of fixture builds. The reserved `.invalid` domain never resolves. */
internal const val API_FIXTURES_HOST = "fixtures.invalid"

/** Matches the BACKEND_BASE_URL that the shared build sets when API_FIXTURES=true. */
internal const val API_FIXTURES_BASE_URL = "https://$API_FIXTURES_HOST"

/**
 * Answers the Braindance API and its image requests from [ApiFixtureData], so developer builds run without an API or
 * network access. Requests for any other host get 404 instead of reaching the network.
 */
internal fun createApiFixtureEngine(dispatcher: CoroutineDispatcher = Dispatchers.Default): HttpClientEngine =
    MockEngine(
        MockEngineConfig().apply {
            this.dispatcher = dispatcher
            addHandler { request -> respondFixture(request) }
        },
    )

private val jsonHeaders = headersOf(HttpHeaders.ContentType, ContentType.Application.Json.toString())
private val pngHeaders = headersOf(HttpHeaders.ContentType, ContentType.Image.PNG.toString())
private val giveawayPathPrefix = "${ApiConfig.Endpoints.GIVEAWAYS}/"

private fun MockRequestHandleScope.respondFixture(request: HttpRequestData): HttpResponseData {
    if (request.url.host != API_FIXTURES_HOST) return respondError(HttpStatusCode.NotFound)
    if (request.method != HttpMethod.Get) return respondError(HttpStatusCode.MethodNotAllowed)
    val path = request.url.encodedPath.removePrefix("/")
    val image = FixtureImages.pngOrNull(path) ?: return respondApi(path, request.url.parameters)
    return respond(image, HttpStatusCode.OK, pngHeaders)
}

/** Follows docs/openapi.yaml: an invalid ID is 400, an unknown game an empty list, an unknown giveaway 404. */
private fun MockRequestHandleScope.respondApi(
    path: String,
    parameters: Parameters,
): HttpResponseData {
    val json =
        when {
            path == ApiConfig.Endpoints.HEALTH -> return respond("", HttpStatusCode.OK)
            path == ApiConfig.Endpoints.TRENDING -> ApiFixtureData.trendingJson
            path == ApiConfig.Endpoints.GIVEAWAYS -> ApiFixtureData.giveawaysJson
            path == ApiConfig.Endpoints.MOST_ANTICIPATED ->
                when (val pageSize = parameters["pageSize"]) {
                    null -> ApiFixtureData.anticipatedJson(Int.MAX_VALUE)
                    else -> pageSize.toPositiveIntOrNull()?.let(ApiFixtureData::anticipatedJson)
                }
            path == ApiConfig.Endpoints.GAME_DETAILS ->
                parameters.positiveIntOrNull("id")?.let(ApiFixtureData::gameDetailsJson)
            path == ApiConfig.Endpoints.GAME_RECOMMENDATIONS ->
                parameters.positiveIntOrNull("id")?.let(ApiFixtureData::recommendationsJson)
            path.startsWith(giveawayPathPrefix) -> {
                val id =
                    path.removePrefix(giveawayPathPrefix).toPositiveIntOrNull()
                        ?: return respondError(HttpStatusCode.BadRequest)
                ApiFixtureData.giveawayJsonOrNull(id) ?: return respondError(HttpStatusCode.NotFound)
            }
            else -> return respondError(HttpStatusCode.NotFound)
        }
    return if (json == null) respondError(HttpStatusCode.BadRequest) else respond(json, HttpStatusCode.OK, jsonHeaders)
}

private fun Parameters.positiveIntOrNull(name: String): Int? = get(name)?.toPositiveIntOrNull()

private fun String.toPositiveIntOrNull(): Int? = toIntOrNull()?.takeIf { it > 0 }
