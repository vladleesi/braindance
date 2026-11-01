# Backend changelog

Release notes for the independently versioned Braindance API, including its original Cloudflare Worker implementation.
Frontend releases are tracked in the [frontend changelog](../CHANGELOG.md).

## 0.9.0

- Add `GET /v1/games/trending`, a Trending Now feed that weighs current attention from IGDB, Twitch, and Steam by
  recent launches, early access exits, new platform releases, and IGDB events. Entries include an optional verified
  `reason` (and `platform` for new platform releases). A cache miss makes two IGDB requests, weak candidates are
  dropped rather than used as filler, and the previous feed is served privately for up to 24 hours when a refresh
  fails. `/v1/games/popular-now` remains for released clients.

## 0.8.1

- Avoid a duplicate recommendation computation when concurrent requests arrive just as a computation for the same
  game finishes.

## 0.8.0

- Add `GET /v1/games/popular-now`, a Popular Right Now feed fused from IGDB Playing and Visits, Steam peak players,
  and Twitch hours watched with two IGDB requests per cache miss. It returns only IDs, names, and cover URLs,
  validates each popularity type's name and freshness, and serves the previous feed privately for up to 24 hours
  when IGDB fails. `/v1/games/popular` and `/v1/games/popularity` remain for released clients.
- Return only names and covers from `/v1/games/anticipated`, and skip upcoming games without covers, cancelled
  games, and erotic games.

## 0.7.1

- Cache successful game, giveaway, and giveaway image responses in process for 10 minutes within fixed memory
  budgets, sharing one upstream request between concurrent identical requests. Requests that bypass the edge cache
  no longer exhaust the shared IGDB rate limit.
- Round the default anticipated-games timestamp to 10 minutes so requests in one period share a cached response.
- Keep at most eight IGDB requests open at once, matching IGDB's documented limit.
- Stop following upstream redirects, so responses cannot steer the image proxy or API calls to other hosts.

## 0.7.0

- Add `GET /v1/games/recommendations` with hybrid ranking of up to 50 similar games from IGDB graph links and
  weighted metadata similarity, filtering editions, DLC, bundles, and other non-game entries.
- Discover candidates with two IGDB multiquery requests per cache miss; cache complete rankings in process for
  24 hours with single-flight deduplication and serve expired results when IGDB fails.
- Give complete recommendation responses a 24-hour shared cache lifetime (`s-maxage=86400`).
- Allow internal IGDB calls to wait briefly for a rate-limit slot instead of failing immediately.

## 0.6.2

- Publish the container image as a single manifest without attestation entries to restore faster cold starts.

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
