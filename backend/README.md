# Braindance API

Kotlin/JVM Ktor backend for IGDB and GamerPower. Version **0.9.0**. Use JDK 21 and run commands from the repository
root. Twitch credentials stay on the backend; GamerPower needs no key.
This reference is for backend development and optional independent hosting. For app availability, see the
[project README](../README.md#platforms); for client setup, see the [development guide](../docs/development.md).
See the [backend changelog](CHANGELOG.md) for release notes.

## Configuration

Create a confidential app in the [Twitch Developer Console](https://dev.twitch.tv/console/apps).
Supply its credentials through environment variables or your host's secret store, never Git or client builds.
Redis is optional for one instance: set `IGDB_RATE_LIMITER=in-memory` and leave `REDIS_URL` unset.

| Variable | Purpose |
| --- | --- |
| `TWITCH_CLIENT_ID`, `TWITCH_CLIENT_SECRET` | Twitch credentials for IGDB |
| `IGDB_RATE_LIMITER` | `in-memory` for one instance; defaults to `redis` |
| `REDIS_URL` | Shared Redis connection URL, required only in Redis mode |
| `CORS_ALLOWED_ORIGINS` | Comma-separated exact browser origins, including scheme and port; empty by default |
| `PUBLIC_BASE_URL` | External API origin for image links; use HTTPS behind a production proxy |
| `PORT` | Listening port; default `8080`, bound to `0.0.0.0` |
| `PUBLIC_CACHE_ENABLED` | Default `false`; enables eligible public GET caching |
| `CLOUDFLARE_ORIGIN_SECRET` | Optional trusted-proxy guard; leave unset for direct hosting and local development |

## Run and package

Copy [`backend/example.env`](example.env) to the ignored `backend/.env` and fill in both Twitch credentials.
For local use, set `IGDB_RATE_LIMITER=in-memory` and `PUBLIC_BASE_URL=http://localhost:8080`.
Add your browser development server's exact origin to `CORS_ALLOWED_ORIGINS`; localhost is not allowed implicitly.
Then export the file's shell-compatible values and start the server:

```sh
set -a
. backend/.env
set +a
./gradlew :backend:run
```

The app does not load env files automatically. Check `http://localhost:8080/health` for readiness and `/healthz`
for credential presence. Only a real game request verifies Twitch/IGDB access.

To test, package, and run a container:

```sh
./gradlew :backend:test :backend:installDist
docker build -f backend/Dockerfile -t braindance-api:0.9.0 backend
docker run --env-file backend/.env -p 8080:8080 braindance-api:0.9.0
```

The Dockerfile requires the prepared distribution. To run without Docker, use
`backend/build/install/backend/bin/backend`. The image includes a JVM startup archive; overriding `JAVA_OPTS`
replaces its default `-Xshare:auto -XX:SharedArchiveFile=/app/startup.jsa` options.

## Hosting

Independent hosting is optional; use these instructions when running a backend for development or your own build.

Use any Java 21 or container host. Configure HTTPS, runtime secrets, process restarts, and abuse controls.
Set `PUBLIC_BASE_URL` to the external HTTPS API origin and `CORS_ALLOWED_ORIGINS` to your website origins, without
paths or trailing slashes. Set the clients' `BACKEND_BASE_URL` before building them.
CORS restricts browsers; it does not authenticate callers. The API is public.

Use the in-memory limiter for one instance; configure [shared rate limiting](#optional-shared-rate-limiting) before
adding replicas. Before enabling [publication](../.github/workflows/publish-backend.yml), replace or disable its
existing `deploy` job for your infrastructure; unconfigured forks do not skip it automatically.
Tag published images with the backend version and source commit, and deploy a fixed digest.

## API

| GET endpoint | Purpose |
| --- | --- |
| `/health` | Readiness and backend version; no credential checks or upstream calls |
| `/healthz` | Credential presence; 503 if either Twitch value is missing |
| `/v1/games/details?id=42` | Game details |
| `/v1/games/recommendations?id=42&limit=24` | Ranked similar games; optional `limit` 1–50, default 24 |
| `/v1/games/trending` | Trending Now feed of up to 20 games with verified reasons; accepts no query parameters |
| `/v1/games/popular-now` | Legacy: Popular Right Now feed of 20 games, for released clients; no query parameters |
| `/v1/games/anticipated?pageSize=20` | Upcoming games; optional `currentTimestamp` in Unix seconds |
| `/v1/games/popular?ids=42,43&pageSize=20` | Legacy: games for 1–50 comma-separated IDs, for released clients |
| `/v1/games/popularity?type=34&pageSize=40` | Legacy: popularity primitives; only `type=34`, for released clients |
| `/v1/giveaways` | Giveaway list |
| `/v1/giveaways/42` | Giveaway details |
| `/v1/giveaways/image?url=...` | Proxy for images under `https://www.gamerpower.com/offers/` |

`/health` returns `{"ok":true,"version":"0.9.0"}`. The version comes from `backend/build.gradle.kts` and is embedded
in the JAR's `Implementation-Version` manifest attribute. Gradle passes the same value as the `backendVersion` JVM
property for `:backend:run` and tests, which use compiled classes instead of the packaged JAR.
For direct IDE launches, set the JVM option `-DbackendVersion=development`; packaged applications prefer the JAR version.

Game IDs are positive 32-bit integers; game lists require `pageSize` from 1–50. An explicit `currentTimestamp`
must be 1–4102444800; omission uses the current time rounded down to 10 minutes. Unknown or duplicate query keys
are rejected.
Legacy game POSTs retain their JSON bodies. Recommendations are GET-only. Game URLs/POST bodies are limited to
2 KiB; upstream JSON to 1 MiB for IGDB and 2 MiB for GamerPower, and images to 5 MiB. Token requests time out after
5 seconds; other upstream requests after 8 seconds. At most eight IGDB requests are open at once, and upstream
redirects are not followed. Successful game and giveaway responses and giveaway images up to 1 MiB are cached in
process for 10 minutes within fixed memory budgets, and concurrent identical requests share one upstream call.
Responses default to `private, no-store`.

## Optional caching and proxy guard

Public caching requires `PUBLIC_CACHE_ENABLED=true` and an HTTPS `PUBLIC_BASE_URL`. Eligible anonymous game and
giveaway GET/200 responses use `public, max-age=1200`; complete recommendation rankings add `s-maxage=86400`, and
the Trending Now and Popular Right Now feeds use `public, max-age=1200, s-maxage=21600`. Preserve full query strings
and Origin in cache keys, respect backend cache headers, and never override no-store for credentials, cookies,
health, preflight, errors, stale fallbacks, or legacy POSTs. Purge existing entries when changing CORS or disabling
caching. A proxy rule that lists cacheable paths must include each feed path, such as `/v1/games/trending`.

If using `CLOUDFLARE_ORIGIN_SECRET`, choose at least 32 printable, non-space ASCII characters. A trusted proxy must
overwrite visitor guard headers and supply exactly one matching `X-Origin-Verify` on every request, including health
checks. Guarded caching also requires `X-Public-Cache-Eligible: 1` only for original requests satisfying the
[backend policy](src/main/kotlin/dev/vladleesi/braindanceapp/backend/ApiCachePolicy.kt); otherwise supply `0`.
Leave caching disabled if you cannot verify proxy rules. The guard does not provide network isolation.

## Recommendations

`/v1/games/recommendations` returns a JSON array of `{"id","name","cover":{"url"}}` objects, strongest match first.
It returns 404 for unknown games and may return fewer results than `limit` when remaining candidates are weak.
The `similar_games` field of `/v1/games/details` is IGDB's unranked list, kept for released clients.

A cache miss makes two IGDB [multiquery](https://api-docs.igdb.com/#multi-query) requests, each using one
rate-limit slot:

1. The source game with its `similar_games` metadata, and games that list the source as similar.
2. Bounded searches for second-degree graph neighbors, the same collection or franchise, the source's distinctive
   genre/theme/perspective profile, and distinctive source keywords shared with its neighbors.

Candidates are filtered to main games, standalone expansions, remakes, remasters, and expanded games without a
`version_parent`; DLC, bundles, ports, editions, cancelled entries, and duplicates are removed. Scores combine
IDF-weighted metadata similarity (keywords, themes, genres, perspectives, modes, release era) with IGDB graph links,
shared series, and popularity; the last three only amplify games whose metadata already agrees. Storefront and
platform keywords such as achievements are ignored. Weights live in `RecommendationRanking.kt`.

Complete rankings are cached in process for 24 hours (512 games) and served for up to 7 more days if a refresh
fails. Concurrent misses for one game share one computation. If the second request fails or exceeds the latency
budget, the ranked native neighbors are returned privately and cached for 2 minutes. Process caches are lost when an
instance stops; the shared edge cache keeps complete rankings across instances when public caching is enabled and
the proxy rule includes this path.

Quality and benchmark checks:

```sh
./gradlew :backend:test --tests '*Recommendation*'
BRAINDANCE_BENCHMARK=1 ./gradlew :backend:test --tests '*RecommendationBenchmarkTest*' --rerun -i
BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
  ./gradlew :backend:test --tests '*LiveRecommendationEvaluationTest*' --rerun -i
```

The default suite uses synthetic fixtures. The benchmark uses a zero-latency fake IGDB and measures backend
overhead only. The live evaluation reads real IGDB data with your credentials and prints a native-versus-hybrid
report against curated title patterns.

## Trending Now

`/v1/games/trending` returns up to 20 `{"id","name","cover":{"url"},"reason"?,"platform"?}` objects, strongest
first, and fewer when candidates are weak. `reason` appears only when a dated IGDB record supports it; `platform`
is the platform abbreviation for `new_platform`, when IGDB has one.

| `reason` | Evidence |
| --- | --- |
| `new_release` | First release within 30 days |
| `full_release` | Full release within 21 days after an early access release on that platform |
| `new_platform` | First playable release on a platform within 21 days, 30+ days after launch |
| `showcased` | Featured by an [IGDB event](https://api-docs.igdb.com/#event) that ended within 14 days |

A cache miss makes two IGDB requests: one multiquery for the top 100 games of each Popular Right Now signal, their
type names, and recent events; then one games query for the 120 candidates with the most attention, with release
dates. Each game scores `attention × relevance` and needs 0.15 to appear. Attention is a weighted rank score across
signals, discounted for games only one signal lists. Relevance is low for established games and peaks after a
launch, early access exit, new platform release, or event, then decays over weeks. Established games without recent
activity therefore need near-top attention on most signals. Eligibility and variety match Popular Right Now, except
that upcoming games are allowed. Weights and windows live in `Trending.kt`.

IGDB popularity primitives hold only current values, so the feed does not measure growth over time. Trailers are
undated in IGDB, updates and patch notes are absent, event coverage depends on IGDB curation, and games outside
every signal's top 100 are not candidates.

The feed is cached in process for 30 minutes, and concurrent misses share one computation. If a refresh fails or
nothing qualifies, the previous feed is served privately for up to 24 hours; missing event data only removes
`showcased` reasons.

```sh
./gradlew :backend:test --tests '*TrendingTest*'
BRAINDANCE_BENCHMARK=1 ./gradlew :backend:test --tests '*TrendingTest*' --rerun -i
BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... \
  ./gradlew :backend:test --tests '*LiveTrendingEvaluationTest*' --rerun -i
```

The benchmark excludes IGDB latency. The live evaluation prints recent events, release status names, each entry's
scores and reason, request sizes and latency, and the Popular Right Now feed.

## Popular Right Now

Released clients use `/v1/games/popular-now`; it and `/v1/games/anticipated` return `{"id","name","cover":{"url"}}`
cards. A cache miss makes one multiquery for the top 50 games of four
[popularity primitives](https://api-docs.igdb.com/#popularity-primitive) (IGDB Playing and Visits, Steam 24-hour
peak players, Twitch 24-hour hours watched) with their type names, then one games query for the 60 best candidates.
Lists are combined with weighted reciprocal rank fusion, `weight / (20 + rank)`; Playing weighs 0.30, Steam and
Twitch 0.25 each, and Visits 0.20. A signal is ignored when its IGDB type name no longer matches or its newest value
is older than seven days.

Released main games, standalone expansions, remakes, remasters, expanded games, and early access games remain;
unreleased, cancelled, edition, DLC, adult-only, coverless, and duplicate-title entries are removed, with at most two
games per collection. The feed is cached in process for 30 minutes, and a failed refresh serves the previous feed
privately for up to 24 hours. Most Anticipated keeps its single IGDB query ranked by `hypes`; it skips coverless,
cancelled, and erotic entries.

Run `BRAINDANCE_LIVE_EVAL=1 TWITCH_CLIENT_ID=... TWITCH_CLIENT_SECRET=... ./gradlew :backend:test --tests
'*LivePopularNowEvaluationTest*' --rerun -i` to print live popularity type names and compare the old and new feeds.

## Optional shared rate limiting

For multiple instances, use `IGDB_RATE_LIMITER=redis` and the same `REDIS_URL` database/keyspace on every replica.
The limiter allows four IGDB calls per rolling second and fails closed if Redis is unavailable. Use private Redis
with TLS/authentication, `noeviction`, and durable persistence. After state loss, pause IGDB traffic for at least
one second; never use independent primaries during failover. Keep connection credentials in runtime secrets.
Optional integration tests use `TEST_REDIS_URL` pointing to a disposable Redis database; CI supplies it.

## Troubleshooting

| Symptom | Check |
| --- | --- |
| Startup requires Redis | Set `IGDB_RATE_LIMITER=in-memory` for one instance, or configure shared Redis. |
| Browser cannot load data | Check the client backend URL and exact allowed origin; localhost also needs an entry. |
| Wrong image host or HTTP links | Set `PUBLIC_BASE_URL` to the external HTTPS origin. |
| 403 with the proxy guard | Check the injected header and backend secret; leave the guard unset locally. |
| IGDB returns 429 | Check the shared four-calls-per-second limit and upstream rate limits. |

For startup diagnostics, compare no-store health and game requests on the same instance; logs separate readiness,
HTTP initialization, and upstream work. Cached responses do not measure backend startup.
