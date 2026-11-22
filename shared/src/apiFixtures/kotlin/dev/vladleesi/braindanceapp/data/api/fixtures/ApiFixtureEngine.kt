// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.fixtures

import dev.vladleesi.braindanceapp.data.config.ApiConfig
import dev.vladleesi.braindanceapp.data.repository.GameSearchRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.data.repository.SearchQuery
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
            path == ApiConfig.Endpoints.TRENDING -> trendingJsonOrNull(parameters)
            path == ApiConfig.Endpoints.EVENTS -> ApiFixtureData.eventsJson.takeIf { parameters.names().isEmpty() }
            path == ApiConfig.Endpoints.EVENT_GAMES ->
                parameters
                    .takeIf { it.names() == setOf("id") && it.getAll("id")?.size == 1 }
                    ?.positiveIntOrNull("id")
                    ?.let(ApiFixtureData::eventGamesJson)
            path == ApiConfig.Endpoints.GIVEAWAYS -> ApiFixtureData.giveawaysJson
            path == ApiConfig.Endpoints.MOST_ANTICIPATED -> anticipatedJsonOrNull(parameters)
            path == ApiConfig.Endpoints.GAME_DETAILS ->
                parameters.positiveIntOrNull("id")?.let(ApiFixtureData::gameDetailsJson)
            path == ApiConfig.Endpoints.GAME_RECOMMENDATIONS ->
                parameters.positiveIntOrNull("id")?.let(ApiFixtureData::recommendationsJson)
            path == ApiConfig.Endpoints.GAME_SEARCH -> searchJsonOrNull(parameters)
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

/** Follows the API's feed rules: an optional `page`, at the default page size, that starts within the ranked games. */
private fun trendingJsonOrNull(parameters: Parameters): String? {
    if (parameters.names().any { it != "page" }) return null
    val page = parameters["page"]?.let { it.toPositiveIntOrNull() ?: return null } ?: 1
    if ((page - 1) * HomeRepo.POPULAR_PAGE_SIZE >= HomeRepo.POPULAR_MAX_GAMES) return null
    return ApiFixtureData.trendingJson(page)
}

/**
 * Follows the API's Most Anticipated paging: pages after the first only at Home's page size, starting within the games
 * the API serves. Like earlier fixtures, the first page takes any `pageSize`, or every game without one, so tests can
 * list the whole feed.
 */
private fun anticipatedJsonOrNull(parameters: Parameters): String? {
    val pageSize =
        parameters["pageSize"]?.let { it.toPositiveIntOrNull() ?: return null }
            ?: return ApiFixtureData.anticipatedJson(Int.MAX_VALUE)
    val page = parameters["page"]?.let { it.toPositiveIntOrNull() ?: return null } ?: 1
    val laterPageValid =
        pageSize == HomeRepo.ANTICIPATED_PAGE_SIZE && (page - 1) * pageSize < HomeRepo.ANTICIPATED_MAX_GAMES
    return if (page == 1 || laterPageValid) ApiFixtureData.anticipatedJson(pageSize, page) else null
}

/** Follows the API's search rules: a valid query, `pageSize` up to 50, and pages that start within 200 results. */
private fun searchJsonOrNull(parameters: Parameters): String? {
    val query = parameters["q"]?.let(SearchQuery::normalize) ?: return null
    val page = parameters["page"]?.toPositiveIntOrNull() ?: return null
    val pageSize = parameters["pageSize"]?.toPositiveIntOrNull()?.takeIf { it <= MAX_PAGE_SIZE } ?: return null
    if ((page - 1) * pageSize >= GameSearchRepo.MAX_RESULTS) return null
    return ApiFixtureData.searchJson(query, page, pageSize)
}

private const val MAX_PAGE_SIZE = 50

private fun Parameters.positiveIntOrNull(name: String): Int? = get(name)?.toPositiveIntOrNull()

private fun String.toPositiveIntOrNull(): Int? = toIntOrNull()?.takeIf { it > 0 }
