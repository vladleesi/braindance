# Backend changelog

Release notes for the independently versioned Braindance API, including its original Cloudflare Worker implementation.
Frontend releases are tracked in the [frontend changelog](../CHANGELOG.md).

## 0.6.1

- Update Ktor and serialization libraries and migrate the Redis rate limiter to Jedis 8.
- Run the backend container on Java 25.

## 0.6.0

- Include the backend version in `/health` responses, derived from the Gradle build version.

## 0.5.2

- Remove the unused Pebble service manager from the runtime container image.

## 0.5.1

- Standardize eligible public game, giveaway, and image responses on a 20-minute cache lifetime.
- Remove stale-serving cache directives while retaining no-store protections for private requests and errors.

## 0.5.0

- Add validated GET endpoints for game queries while retaining existing POST endpoints.
- Extend public caching to game reads, giveaway details, and image responses, with separate CORS variants.
- Add request timing diagnostics.

## 0.4.1

- Require exact, explicitly configured browser origins, including local development origins.
- Verify proxy-provided cache eligibility when transport headers are rewritten.

## 0.4.0

- Add opt-in public caching for anonymous giveaway-list responses.
- Add proxy origin verification, cache bypass rules, and API security headers.

## 0.3.2

- Reduce startup work with a JVM class-data archive and deferred HTTP client initialization.
- Add startup phase timing diagnostics and include the archive preparation script in container builds.

## 0.3.0

- Move the API to Kotlin/Ktor on Java 21 with Docker packaging.
- Add per-instance IGDB rate limiting and an optional Redis limiter shared across replicas.
- Retain game queries, giveaway endpoints, and image proxying with bounded inputs and upstream responses.

## 0.2.3

- Preserve configured Worker variables, including browser origins, across deployments.

## 0.2.2

- Add browser CORS handling and preflight responses.
- Proxy approved giveaway images and thumbnails for browser clients.

## 0.2.0

- Add GamerPower giveaway list and detail endpoints with validated IDs, response limits, and upstream timeouts.

## 0.1.0

- Introduce a Cloudflare Worker gateway for IGDB game details, upcoming and popular games, and popularity queries.
- Keep Twitch credentials server-side, reuse OAuth tokens, and enforce a shared four-requests-per-second limit.
- Validate request inputs, bound response sizes, and expose a credential-presence health check.
