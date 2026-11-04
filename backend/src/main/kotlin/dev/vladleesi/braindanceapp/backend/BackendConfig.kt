// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber")

package dev.vladleesi.braindanceapp.backend

/** How the IGDB rate limit is shared: one Redis key for all replicas, or per process for a single instance. */
enum class RateLimiterMode(
    val configValue: String,
) {
    REDIS("redis"),
    IN_MEMORY("in-memory"),
    ;

    companion object {
        fun parse(value: String): RateLimiterMode =
            requireNotNull(entries.firstOrNull { it.configValue == value }) { "Invalid IGDB_RATE_LIMITER" }
    }
}

data class BackendConfig(
    val clientId: String?,
    val clientSecret: String?,
    val redisUrl: String?,
    val allowedOrigins: Set<String>,
    val port: Int,
    val publicBaseUrl: String? = null,
    val rateLimiterMode: RateLimiterMode = RateLimiterMode.REDIS,
    val publicCacheEnabled: Boolean = false,
    val cloudflareOriginSecret: String? = null,
) {
    val credentialsAvailable: Boolean get() = !clientId.isNullOrEmpty() && !clientSecret.isNullOrEmpty()

    companion object {
        fun fromEnvironment(env: Map<String, String> = System.getenv()) =
            BackendConfig(
                clientId = env["TWITCH_CLIENT_ID"],
                clientSecret = env["TWITCH_CLIENT_SECRET"],
                redisUrl = env["REDIS_URL"],
                allowedOrigins =
                    env["CORS_ALLOWED_ORIGINS"]
                        .orEmpty()
                        .split(',')
                        .map(String::trim)
                        .filter(String::isNotEmpty)
                        .toSet(),
                port = env["PORT"]?.toIntOrNull()?.takeIf { it in 1..65535 } ?: 8080,
                publicBaseUrl = env["PUBLIC_BASE_URL"]?.trimEnd('/')?.takeIf(String::isNotEmpty),
                rateLimiterMode = RateLimiterMode.parse(env["IGDB_RATE_LIMITER"] ?: RateLimiterMode.REDIS.configValue),
                publicCacheEnabled = env["PUBLIC_CACHE_ENABLED"] == "true",
                cloudflareOriginSecret = env["CLOUDFLARE_ORIGIN_SECRET"],
            )
    }
}
