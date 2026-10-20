# Braindance API

Kotlin/JVM Ktor backend for IGDB and GamerPower. Version **0.5.1**. Use JDK 21 and run commands from the repository
root. Twitch credentials stay on the backend; GamerPower needs no key.

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
docker build -f backend/Dockerfile -t braindance-api:0.5.1 backend
docker run --env-file backend/.env -p 8080:8080 braindance-api:0.5.1
```

The Dockerfile requires the prepared distribution. To run without Docker, use
`backend/build/install/backend/bin/backend`. The image includes a JVM startup archive; overriding `JAVA_OPTS`
replaces its default `-Xshare:auto -XX:SharedArchiveFile=/app/startup.jsa` options.

## Hosting

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
| `/health` | Readiness; no credential checks or upstream calls |
| `/healthz` | Credential presence; 503 if either Twitch value is missing |
| `/v1/games/details?id=42` | Game details |
| `/v1/games/anticipated?pageSize=20` | Upcoming games; optional `currentTimestamp` in Unix seconds |
| `/v1/games/popular?ids=42,43&pageSize=20` | Games for 1–50 comma-separated IDs |
| `/v1/games/popularity?type=34&pageSize=40` | Popularity primitives; only `type=34` is supported |
| `/v1/giveaways` | Giveaway list |
| `/v1/giveaways/42` | Giveaway details |
| `/v1/giveaways/image?url=...` | Proxy for images under `https://www.gamerpower.com/offers/` |

Game IDs are positive 32-bit integers; game lists require `pageSize` from 1–50. An explicit `currentTimestamp`
must be 1–4102444800; omission uses the current time. Unknown or duplicate query keys are rejected.
Legacy game POSTs retain their JSON bodies. Game URLs/POST bodies are limited to 2 KiB; upstream JSON to 1 MiB
for IGDB and 2 MiB for GamerPower, and images to 5 MiB. Token requests time out after 5 seconds; other upstream
requests after 8 seconds. Responses default to `private, no-store`.

## Optional caching and proxy guard

Public caching requires `PUBLIC_CACHE_ENABLED=true` and an HTTPS `PUBLIC_BASE_URL`. Eligible anonymous game and
giveaway GET/200 responses use `public, max-age=1200`. Preserve full query strings and Origin in cache keys,
respect backend cache headers, and never override no-store for credentials, cookies, health, preflight, errors,
or legacy POSTs. Purge existing entries when changing CORS or disabling caching.

If using `CLOUDFLARE_ORIGIN_SECRET`, choose at least 32 printable, non-space ASCII characters. A trusted proxy must
overwrite visitor guard headers and supply exactly one matching `X-Origin-Verify` on every request, including health
checks. Guarded caching also requires `X-Public-Cache-Eligible: 1` only for original requests satisfying the
[backend policy](src/main/kotlin/dev/vladleesi/braindanceapp/backend/ApiCachePolicy.kt); otherwise supply `0`.
Leave caching disabled if you cannot verify proxy rules. The guard does not provide network isolation.

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
