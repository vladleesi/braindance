// SPDX-License-Identifier: MPL-2.0

@file:Suppress("ComplexCondition", "StringLiteralDuplication")

package dev.vladleesi.braindanceapp.backend

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.hooks.ResponseBodyReadyForSend
import io.ktor.server.application.install
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.request.uri
import io.ktor.util.AttributeKey
import java.net.URI
import java.security.MessageDigest
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

internal val publicResponse = AttributeKey<Unit>("PublicResponse")

internal const val PRIVATE_CACHE_CONTROL = "private, no-store"
internal const val PUBLIC_CACHE_CONTROL = "public, max-age=1200"

/**
 * Cloudflare reads this header instead of Cache-Control for its own cache and does not forward it, so edge-only
 * directives stay out of browsers. Responses without it, including every private one, follow Cache-Control.
 */
internal const val CLOUDFLARE_CACHE_CONTROL_HEADER = "Cloudflare-CDN-Cache-Control"

// Browsers keep public responses for 20 minutes because they cannot be purged.
private val BROWSER_LIFETIME = 20.minutes

// When a refresh fails, for example on a cold start, the edge may serve its last copy for a day. Cloudflare ignores
// stale-if-error next to s-maxage in Cache-Control, so the directive is sent only in its own header.
private val EDGE_STALE_IF_ERROR = 1.days

/** Cache-Control and Cloudflare edge directives for a public response on [route] at [nowMillis]. */
internal data class PublicCacheHeaders(
    val cacheControl: String,
    val cloudflareCacheControl: String,
)

internal fun publicCacheHeaders(
    route: String?,
    nowMillis: Long,
): PublicCacheHeaders {
    // Popular Right Now pages expire together at the end of the feed window, so pages fetched in one window come
    // from the same feed rather than from feeds ranked hours apart.
    // Shared caches keep public data longer than browsers, so the edge serves it across scaled-to-zero instances.
    val edge =
        when (val lifetime = ApiRoute.forTemplate(route)?.edgeLifetime) {
            is EdgeLifetime.Fixed -> lifetime.duration
            EdgeLifetime.FeedWindow -> TrendingService.feedTimeLeft(nowMillis)
            EdgeLifetime.None, null -> BROWSER_LIFETIME
        }
    val browser = minOf(BROWSER_LIFETIME, edge)
    val cacheControl =
        if (edge == BROWSER_LIFETIME) {
            PUBLIC_CACHE_CONTROL
        } else {
            "public, max-age=${browser.inWholeSeconds}, s-maxage=${edge.inWholeSeconds}"
        }
    return PublicCacheHeaders(
        cacheControl,
        "public, max-age=${edge.inWholeSeconds}, stale-if-error=${EDGE_STALE_IF_ERROR.inWholeSeconds}",
    )
}

internal const val ORIGIN_SECRET_HEADER = "X-Origin-Verify"
internal const val PUBLIC_CACHE_ELIGIBLE_HEADER = "X-Public-Cache-Eligible"
private const val MIN_ORIGIN_SECRET_LENGTH = 32
private val proxyTransportHeaders = setOf("forwarded", "x-forwarded-host", "transfer-encoding")

// Keep reverse-proxy eligibility rules aligned with this list. Even empty headers bypass cache.
internal val cacheBypassHeaders =
    setOf(
        "authorization",
        "proxy-authorization",
        "cookie",
        "x-api-key",
        "x-auth-token",
        "x-user-id",
        "x-session-id",
        "range",
        "transfer-encoding",
        "x-http-method-override",
        "x-http-method",
        "x-method-override",
        "x-forwarded-host",
        "x-host",
        "x-original-url",
        "x-rewrite-url",
        "forwarded",
    )

internal fun Application.installApiCachePolicy(config: BackendConfig) {
    require(!config.publicCacheEnabled || URI(config.publicBaseUrl.orEmpty()).scheme == "https") {
        "PUBLIC_CACHE_ENABLED requires an HTTPS PUBLIC_BASE_URL"
    }
    config.cloudflareOriginSecret?.let { secret ->
        require(secret.length >= MIN_ORIGIN_SECRET_LENGTH && secret.all { it in '!'..'~' }) {
            "CLOUDFLARE_ORIGIN_SECRET must contain at least 32 printable non-space ASCII characters"
        }
    }
    install(
        createApplicationPlugin("ApiCachePolicy") {
            on(ResponseBodyReadyForSend) { call, content ->
                if (call.canCachePublicResponse(config, content)) {
                    val headers = publicCacheHeaders(routeOf(call.request.path()), System.currentTimeMillis())
                    call.response.headers.append(HttpHeaders.CacheControl, headers.cacheControl)
                    call.response.headers.append(CLOUDFLARE_CACHE_CONTROL_HEADER, headers.cloudflareCacheControl)
                } else {
                    call.response.headers.append(HttpHeaders.CacheControl, PRIVATE_CACHE_CONTROL)
                }
                call.response.headers.append("X-Content-Type-Options", "nosniff")
                call.response.headers.append("Referrer-Policy", "no-referrer")
                call.response.headers.append("X-Frame-Options", "DENY")
                call.response.headers.append("Content-Security-Policy", "default-src 'none'; frame-ancestors 'none'")
            }
        },
    )
}

private fun ApplicationCall.canCachePublicResponse(
    config: BackendConfig,
    content: OutgoingContent,
): Boolean {
    // A route must explicitly attest to a public body. New routes are private even if they return GET/200.
    if (!config.publicCacheEnabled || attributes.getOrNull(publicResponse) == null) return false
    if (request.httpMethod != HttpMethod.Get || !hasPublicQuery()) return false
    val origins = request.headers.getAll(HttpHeaders.Origin)
    if (origins != null &&
        (origins.size != 1 || !isAllowedOrigin(origins.single(), config.allowedOrigins))
    ) {
        return false
    }
    // Proxies can rewrite transport headers. A guarded attestation checks the original request.
    val guarded = config.cloudflareOriginSecret != null
    if (
        guarded &&
        (!hasValidOriginSecret(config) || request.headers.getAll(PUBLIC_CACHE_ELIGIBLE_HEADER) != listOf("1"))
    ) {
        return false
    }
    if (
        cacheBypassHeaders.any {
            !(guarded && it in proxyTransportHeaders) && request.headers.getAll(it) != null
        }
    ) {
        return false
    }
    if (!guarded) {
        val lengths = request.headers.getAll(HttpHeaders.ContentLength)
        if (lengths != null && lengths != listOf("0")) return false
        val publicBaseUrl = config.publicBaseUrl ?: return false
        if (request.headers.getAll(HttpHeaders.Host) != listOf(URI(publicBaseUrl).rawAuthority)) return false
    }
    if ((content.status ?: response.status()) != HttpStatusCode.OK) return false
    // Only the exact CORS headers emitted by this API are safe; other variations still fail closed.
    val safeHeaders = setOf("content-type", "content-length")
    val corsHeaders =
        mapOf(
            "vary" to "Origin",
            "access-control-allow-origin" to origins?.singleOrNull(),
            "access-control-allow-methods" to "GET, OPTIONS",
            "access-control-allow-headers" to "Content-Type",
            "access-control-max-age" to "86400",
        )
    return listOf(response.headers.allValues(), content.headers).all { headers ->
        headers.names().all { name ->
            name.lowercase() in safeHeaders ||
                corsHeaders[name.lowercase()]?.let { headers.getAll(name) == listOf(it) } == true
        }
    }
}

private fun ApplicationCall.hasPublicQuery(): Boolean {
    val path = request.path()
    // Do not cache path aliases or unknown/duplicate query parameters, even when a handler ignores them.
    if (request.uri.substringBefore('?') != path) return false
    val names = routeOf(path)?.let(routeQueryParameters::getValue) ?: return false
    return request.queryParameters.entries().all { (name, values) -> name in names && values.size == 1 }
}

internal fun ApplicationCall.hasValidOriginSecret(config: BackendConfig): Boolean {
    val expected = config.cloudflareOriginSecret ?: return true
    val supplied = request.headers.getAll(ORIGIN_SECRET_HEADER)?.singleOrNull() ?: return false
    return MessageDigest.isEqual(expected.encodeToByteArray(), supplied.encodeToByteArray())
}
