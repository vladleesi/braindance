// SPDX-License-Identifier: MPL-2.0

@file:Suppress("MagicNumber")

package dev.vladleesi.braindanceapp.backend

data class BackendConfig(
    val clientId: String?,
    val clientSecret: String?,
    val redisUrl: String?,
    val allowedOrigins: Set<String>,
    val port: Int,
    val publicBaseUrl: String? = null,
    val rateLimiterMode: String = "redis",
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
                rateLimiterMode = env["IGDB_RATE_LIMITER"] ?: "redis",
                publicCacheEnabled = env["PUBLIC_CACHE_ENABLED"] == "true",
                cloudflareOriginSecret = env["CLOUDFLARE_ORIGIN_SECRET"],
            )
    }
}
