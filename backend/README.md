# Braindance API

The Kotlin/JVM Ktor backend serves game and giveaway data from IGDB and GamerPower to Braindance.
Version **0.10.2**. This reference covers backend development and optional independent hosting;
using the [experimental Web preview](https://braindance.vladleesi.dev/) does not require backend setup.

Twitch credentials stay on the backend; GamerPower needs no key.
Build with JDK 21 and run commands from the repository root; the container image runs Java 25.
See the [changelog](CHANGELOG.md), the [project README](../README.md#platforms), and the
[development guide](../docs/development.md) for client setup.

## Configuration

Create a confidential app in the [Twitch Developer Console](https://dev.twitch.tv/console/apps). Supply its
credentials through environment variables or your host's secret store, never Git or client builds.

| Variable | Purpose |
| --- | --- |
| `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET` | Twitch credentials for IGDB |
| `IGDB_RATE_LIMITER` | `in-memory` for one instance; defaults to `redis` |
| `REDIS_URL` | Shared Redis connection URL, required only in Redis mode |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact browser origins, including scheme and port; empty by default |
| `PUBLIC_BASE_URL` | External API origin for giveaway image links; HTTPS behind a production proxy. Without it, images keep their GamerPower URLs |
| `PORT` | Listening port; default `8080`, bound to `0.0.0.0` |
| `PUBLIC_CACHE_ENABLED` | Default `false`; enables eligible public GET caching |
| `CLOUDFLARE_ORIGIN_SECRET` | Optional trusted-proxy guard; leave unset for direct hosting and local development |

## Run locally

Copy [`example.env`](example.env) to the ignored `backend/.env`, fill in both Twitch credentials, and set
`IGDB_RATE_LIMITER=in-memory` and `PUBLIC_BASE_URL=http://localhost:8080`. Add your browser development server's
exact origin to `CORS_ALLOWED_ORIGINS`; localhost is not allowed implicitly. The app does not load env files itself:

```sh
set -a; . backend/.env; set +a
./gradlew :backend:run
```

`/health` is a dependency-free liveness check that reports the version; `/healthz` reports credential presence.
Only a real game request verifies IGDB access. For direct IDE launches, set the JVM option
`-DbackendVersion=development`.

## Package and host

```sh
./gradlew :backend:test :backend:installDist
docker build -f backend/Dockerfile -t braindance-api:0.10.2 backend
docker run --env-file backend/.env -p 8080:8080 braindance-api:0.10.2
```

The Dockerfile needs the prepared distribution; without Docker, run `backend/build/install/backend/bin/backend`.
Overriding `JAVA_OPTS` replaces the image's startup-archive options.

Any Java 21 or newer, or container host works. Configure HTTPS, runtime secrets, restarts, and abuse controls; set
`PUBLIC_BASE_URL` to the external HTTPS origin and `CORS_ALLOWED_ORIGINS` to your website origins, and point the
clients' `BACKEND_BASE_URL` at it. CORS restricts browsers; it does not authenticate callers, and the API is public.
Configure [shared rate limiting](#shared-rate-limiting) before adding replicas. Before enabling
[publication](../.github/workflows/publish-backend.yml), replace or disable its `deploy` job for your infrastructure;
forks do not skip it automatically. Tag images with the backend version and source commit, and deploy a digest.

## API

All endpoints are GET-only and return JSON. Unknown or duplicate query keys are rejected with 400.

| Endpoint | Returns |
| --- | --- |
| `/health`, `/healthz` | `{"ok":true,"version":"0.10.2"}`; `/healthz` returns 503 without Twitch credentials |
| `/v1/games/details?id=42` | Game details |
| `/v1/games/recommendations?id=42&limit=24` | Similar games, best first; `limit` 1–50 (default 24); 404 if unknown |
| `/v1/games/trending?page=1&pageSize=20` | [Popular Right Now](#popular-right-now) feed of released games |
| `/v1/games/anticipated?pageSize=20` | Upcoming games ranked by IGDB hypes; `pageSize` 1–50 |
| `/v1/giveaways`, `/v1/giveaways/42` | Giveaway list and details; links that are not absolute HTTPS URLs are removed |
| `/v1/giveaways/image?url=...` | Proxy for JPEG, PNG, WebP, GIF, and AVIF images under `https://www.gamerpower.com/offers/` |

Game cards are `{"id","name","cover":{"url"}}`. Request URLs are limited to 2 KiB; upstream JSON to 1 MiB (IGDB) and
2 MiB (GamerPower), images to 1 MiB. Upstream requests time out after 8 seconds (token requests after 5), at most
eight IGDB requests and four image fetches are open at once, and redirects are not followed. An image request that
waits more than 3 seconds for a fetch slot gets 503, and a failed image URL is not fetched again for a minute.
Successful game, giveaway, and image responses are cached in process for 10 minutes, and identical concurrent requests
share one upstream call. Responses default to `private, no-store`.

Failures write one JSON line to standard output: `request_failed` (an unexpected exception, with its class and, for
Twitch tokens, the reason and upstream status), `upstream_failed` (an unusable IGDB status or content type), or
`limiter_rejected` (no IGDB rate-limit slot). Each event is written at most once per second per route or operation,
with a `suppressed` count of the skipped repeats. Lines never contain visitor URLs, query strings, headers, bodies,
or exception messages.

## Popular Right Now

`/v1/games/trending` returns cards with an optional verified `reason`, strongest first. It ranks current interest in
released games, including early access, from IGDB [PopScore](https://api-docs.igdb.com/#popscore) signals (Visits,
Want to Play, Twitch, Playing, Steam) and boosts dated recent activity. Upcoming games belong to Most Anticipated.

| `reason` | Evidence |
| --- | --- |
| `new_release` | First release within 30 days |
| `full_release` | Full release within 21 days after early access on that platform |
| `new_platform` | First playable release on a platform within 21 days, 30+ days after launch; adds `platform` |
| `showcased` | Featured by an [IGDB event](https://api-docs.igdb.com/#event) that ended within 14 days |

Up to 100 games are ranked. Without parameters the endpoint returns the first 20. `page` (default 1) and `pageSize`
(1–50, default 20) must be positive integers without signs or leading zeros, and a page must start within the first
100 games; pages past the end are empty. The feed is shorter rather than filled with weak candidates.

A cache miss makes two IGDB multiqueries. A request waits up to 4.5 seconds; a slower refresh, as on a cold start,
keeps running for up to 20 seconds and fills the cache for the next request. One ranked feed serves each six-hour UTC
window (00:00, 06:00, 12:00, 18:00), in process and at the edge, so pages fetched in one window match; separate
instances can still rank slightly differently. If a refresh fails, the previous feed is served privately for up to 24
hours. The edge header also allows `stale-while-revalidate` for 10 minutes, so right after a window ends Cloudflare can
keep serving the previous feed while it fetches the next one; pages fetched in those minutes can come from different
windows.
IGDB stores no popularity history, so the feed does not measure growth. Signals, weights, and eligibility are
documented in [`Trending.kt`](src/main/kotlin/dev/vladleesi/braindanceapp/backend/Trending.kt); each cache miss logs a
`trending` line with candidate counts per stage.

## Recommendations

A cache miss makes two IGDB [multiqueries](https://api-docs.igdb.com/#multi-query): the source game's graph, then
bounded searches for related series, metadata profiles, and keywords. Only main games, standalone expansions,
remakes, remasters, and expanded games are returned. Complete rankings are cached for 24 hours and served for up to
7 more days if a refresh fails; partial rankings after a failed second request are private and cached for 2 minutes.
Scoring is documented in
[`RecommendationRanking.kt`](src/main/kotlin/dev/vladleesi/braindanceapp/backend/RecommendationRanking.kt).

## Caching and proxy guard

Public caching requires `PUBLIC_CACHE_ENABLED=true` and an HTTPS `PUBLIC_BASE_URL`. Eligible anonymous GET/200
responses let browsers keep them for 20 minutes (`max-age=1200`) and shared caches longer (`s-maxage`):

| Route | Shared lifetime |
| --- | --- |
| `/v1/games/details`, complete `/v1/games/recommendations` | 1 day |
| `/v1/games/anticipated` | 6 hours |
| `/v1/games/trending` | Until the current six-hour feed window ends; browsers at most that long |
| `/v1/giveaways`, `/v1/giveaways/42` | 1 hour |
| `/v1/giveaways/image` | 7 days |

Public responses also send `Cloudflare-CDN-Cache-Control` with the same shared lifetime and `stale-if-error=86400`,
so Cloudflare serves its last copy for up to a day when the origin returns 5xx. Cloudflare reads that header instead
of `Cache-Control` and does not forward it; other proxies ignore it. A proxy must keep full query strings and Origin
in cache keys, respect backend cache headers, list the routes above among cacheable paths, and never override no-store
for credentials, cookies, health, preflight, errors, stale fallbacks, or non-GET methods. Purge cached entries when
changing CORS or disabling caching.

With `CLOUDFLARE_ORIGIN_SECRET` (at least 32 printable, non-space ASCII characters), a trusted proxy must overwrite
visitor guard headers and send exactly one matching `X-Origin-Verify` on every request, including health checks.
Guarded caching also requires `X-Public-Cache-Eligible: 1` only for original requests that satisfy the
[backend policy](src/main/kotlin/dev/vladleesi/braindanceapp/backend/ApiCachePolicy.kt), otherwise `0`. Leave caching
disabled if you cannot verify the proxy rules. The guard does not provide network isolation.

## Shared rate limiting

For multiple instances, use `IGDB_RATE_LIMITER=redis` with the same `REDIS_URL` database and keyspace on every
replica. The limiter allows four IGDB calls per rolling second and fails closed if Redis is unavailable. Use private
Redis with TLS and authentication, `noeviction`, and durable persistence; keep its credentials in runtime secrets.
After state loss, pause IGDB traffic for at least one second, and never use independent primaries during failover.

## Tests and evaluations

```sh
./gradlew :backend:test
BRAINDANCE_BENCHMARK=1 ./gradlew :backend:test --tests '*BenchmarkTest*' --tests '*TrendingTest*' --rerun -i
BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
  ./gradlew :backend:test --tests '*LiveTrendingEvaluationTest*' --tests '*LiveRecommendationEvaluationTest*' --rerun -i
```

The default suite uses synthetic fixtures; Redis integration tests need `TEST_REDIS_URL` for a disposable database,
which CI supplies. Benchmarks exclude IGDB latency. Live evaluations read real IGDB data: the Popular Right Now
report compares tuning variants with a manual IGDB homepage snapshot, and the recommendation report compares
against curated title patterns.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Startup requires Redis | Set `IGDB_RATE_LIMITER=in-memory` for one instance, or configure shared Redis. |
| Browser cannot load data | Check the client backend URL and the exact allowed origin; localhost needs an entry. |
| Wrong image host or HTTP links | Set `PUBLIC_BASE_URL` to the external HTTPS origin. |
| 403 with the proxy guard | Check the injected header and backend secret; leave the guard unset locally. |
| IGDB returns 429 | Check the shared four-calls-per-second limit and upstream rate limits. |
