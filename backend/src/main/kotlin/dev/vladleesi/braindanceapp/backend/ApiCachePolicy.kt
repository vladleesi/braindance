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

internal val publicResponse = AttributeKey<Unit>("PublicResponse")

internal const val PRIVATE_CACHE_CONTROL = "private, no-store"
internal const val PUBLIC_CACHE_CONTROL = "public, max-age=1200"

// Ranked recommendations change slowly; a day-long shared lifetime lets the edge serve them across scaled-to-zero
// backend instances while browsers keep the standard 20-minute lifetime.
internal const val RECOMMENDATIONS_CACHE_CONTROL = "public, max-age=1200, s-maxage=86400"
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
                val cacheable = call.canCachePublicResponse(config, content)
                call.response.headers.append(
                    HttpHeaders.CacheControl,
                    if (cacheable && call.request.path() == RECOMMENDATIONS_PATH) {
                        RECOMMENDATIONS_CACHE_CONTROL
                    } else if (cacheable) {
                        PUBLIC_CACHE_CONTROL
                    } else {
                        PRIVATE_CACHE_CONTROL
                    },
                )
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
            "access-control-allow-methods" to "GET, POST, OPTIONS",
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
    val names =
        when {
            path == "/v1/giveaways" || validGiveawayId(path) -> emptySet()
            path == "/v1/giveaways/image" -> setOf("url")
            path == RECOMMENDATIONS_PATH -> recommendationQueryParameters
            else -> gameQueryParameters[path] ?: return false
        }
    return request.queryParameters.entries().all { (name, values) -> name in names && values.size == 1 }
}

internal fun ApplicationCall.hasValidOriginSecret(config: BackendConfig): Boolean {
    val expected = config.cloudflareOriginSecret ?: return true
    val supplied = request.headers.getAll(ORIGIN_SECRET_HEADER)?.singleOrNull() ?: return false
    return MessageDigest.isEqual(expected.encodeToByteArray(), supplied.encodeToByteArray())
}
