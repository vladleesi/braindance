// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

private const val OK_STATUS = 200

/** Outcome of a recommendation request. Only [Kind.COMPLETE] results may be cached publicly. */
internal data class RecommendationResult(
    /** Null when recommendations were computed; otherwise why they could not be. */
    val failure: UpstreamFailure? = null,
    val games: List<Recommendation> = emptyList(),
    val kind: Kind = Kind.COMPLETE,
) {
    /** The HTTP status the API answers with. */
    val status: Int get() = failure?.status ?: OK_STATUS

    enum class Kind {
        /** Full hybrid discovery succeeded. */
        COMPLETE,

        /** Discovery failed; ranked native IGDB neighbors only. */
        DEGRADED,

        /** A refresh failed and an expired complete result was served instead. */
        STALE,
    }
}

/**
 * Owns recommendation discovery, ranking, and caching. A cache miss costs two IGDB requests: one multiquery for the
 * source game and its graph neighbors, then one multiquery for bounded candidate searches. Concurrent requests for the
 * same game share one computation. The cache is process-local; shared reuse across instances comes from the edge cache.
 */
internal class RecommendationService(
    private val igdb: IgdbService,
    private val clock: Clock = Clock.systemUTC(),
    private val log: (String) -> Unit = ::println,
    private val maxEntries: Int = MAX_CACHE_ENTRIES,
) : AutoCloseable {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val inFlight = ConcurrentHashMap<Int, Deferred<RecommendationResult>>()
    private val cache =
        object : LinkedHashMap<Int, CacheEntry>(maxEntries, LOAD_FACTOR, true) {
            override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, CacheEntry>) = size > maxEntries
        }

    private data class CacheEntry(
        val result: RecommendationResult,
        val freshUntil: Long,
        val usableUntil: Long,
    )

    suspend fun recommendations(gameId: Int): RecommendationResult {
        freshResult(gameId)?.let { return it }
        // Waiters can cancel independently; the shared computation is bounded by its own timeouts.
        val computation =
            inFlight.computeIfAbsent(gameId) {
                scope.async(start = CoroutineStart.LAZY) {
                    try {
                        // A previous computation can store its result and finish between the lookup above and
                        // this one starting, so check again before computing.
                        freshResult(gameId) ?: refresh(gameId)
                    } finally {
                        inFlight.remove(gameId)
                    }
                }
            }
        computation.start()
        return computation.await()
    }

    override fun close() {
        scope.cancel()
    }

    private suspend fun refresh(gameId: Int): RecommendationResult {
        val started = TimeSource.Monotonic.markNow()
        val metrics = Metrics()
        val computed =
            try {
                compute(gameId, started, metrics)
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) RecommendationResult(UpstreamFailure.TIMEOUT) else throw e
            } catch (e: Exception) {
                metrics.error = e::class.simpleName
                RecommendationResult(UpstreamFailure.UNAVAILABLE)
            }
        val result =
            if (computed.failure != null && computed.failure != UpstreamFailure.NOT_FOUND) {
                staleFallback(gameId) ?: computed
            } else {
                computed
            }
        store(gameId, result)
        log(metrics.event(gameId, result, started.elapsedNow().inWholeMilliseconds))
        return result
    }

    private suspend fun compute(
        gameId: Int,
        started: TimeMark,
        metrics: Metrics,
    ): RecommendationResult {
        val sourceResponse =
            withTimeout(SOURCE_TIMEOUT) {
                igdb.execute(MULTIQUERY_PATH, RecommendationQueries.sourceQuery(gameId), SLOT_WAIT.inWholeMilliseconds)
            }
        metrics.record(sourceResponse)
        val sourceBody =
            when (sourceResponse) {
                is UpstreamOutcome.Ok -> sourceResponse.body
                is UpstreamOutcome.Failed -> return RecommendationResult(sourceFailure(sourceResponse.failure))
            }
        val graph =
            RecommendationQueries.parseSourceGraph(RecommendationQueries.parseMultiquery(sourceBody))
                ?: return RecommendationResult(UpstreamFailure.NOT_FOUND)

        val discoveryQuery =
            RecommendationQueries.discoveryQuery(graph, RecommendationRanking.retrievalKeywords(graph))
        val budget = (TOTAL_BUDGET - started.elapsedNow()).coerceAtLeast(MIN_DISCOVERY)
        // Without any usable discovery filter the graph neighbors are the complete candidate set.
        val discovered = if (discoveryQuery == null) emptyList() else discover(discoveryQuery, budget, metrics)
        val scoringStarted = TimeSource.Monotonic.markNow()
        val pool = RecommendationRanking.candidatePool(graph, discovered.orEmpty())
        val ranked = RecommendationRanking.rank(graph, pool)
        metrics.scoringMicros = scoringStarted.elapsedNow().inWholeMicroseconds
        metrics.candidates = pool.size
        val kind = if (discovered == null) RecommendationResult.Kind.DEGRADED else RecommendationResult.Kind.COMPLETE
        return RecommendationResult(games = ranked, kind = kind)
    }

    /** Returns null when discovery fails, so the caller falls back to native graph neighbors. */
    private suspend fun discover(
        query: String,
        budget: Duration,
        metrics: Metrics,
    ): List<IgdbGame>? {
        val response =
            withTimeoutOrNull(budget) {
                igdb.execute(MULTIQUERY_PATH, query, minOf(SLOT_WAIT, budget / 2).inWholeMilliseconds)
            }
        if (response == null) {
            metrics.discoveryFailure = "timeout"
            return null
        }
        metrics.record(response)
        if (response !is UpstreamOutcome.Ok) {
            metrics.discoveryFailure = response.status.toString()
            return null
        }
        return try {
            RecommendationQueries
                .parseMultiquery(response.body)
                .values
                .flatMap(RecommendationQueries::parseGames)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            metrics.discoveryFailure = "malformed"
            null
        }
    }

    private fun cached(gameId: Int): CacheEntry? = synchronized(cache) { cache[gameId] }

    private fun freshResult(gameId: Int): RecommendationResult? =
        cached(gameId)?.takeIf { clock.millis() < it.freshUntil }?.result

    private fun staleFallback(gameId: Int): RecommendationResult? {
        val entry = cached(gameId) ?: return null
        if (entry.result.kind != RecommendationResult.Kind.COMPLETE || clock.millis() >= entry.usableUntil) return null
        return entry.result.copy(kind = RecommendationResult.Kind.STALE)
    }

    private fun store(
        gameId: Int,
        result: RecommendationResult,
    ) {
        val now = clock.millis()
        val entry =
            when {
                result.failure == null && result.kind == RecommendationResult.Kind.COMPLETE ->
                    CacheEntry(result, now + FRESH.inWholeMilliseconds, now + STALE_IF_ERROR.inWholeMilliseconds)
                result.failure == null && result.kind == RecommendationResult.Kind.DEGRADED ->
                    CacheEntry(result, now + DEGRADED.inWholeMilliseconds, now + DEGRADED.inWholeMilliseconds)
                // An unknown ID stays unknown, so it is remembered longer; random-ID floods then cost IGDB once.
                result.failure == UpstreamFailure.NOT_FOUND ->
                    CacheEntry(result, now + NOT_FOUND.inWholeMilliseconds, now + NOT_FOUND.inWholeMilliseconds)
                // Keep the expired complete entry for later stale-if-error responses; do not cache failures.
                else -> return
            }
        synchronized(cache) { cache[gameId] = entry }
    }

    // A rate limit is reported as such; any other source failure means IGDB is unavailable.
    private fun sourceFailure(failure: UpstreamFailure) =
        if (failure == UpstreamFailure.RATE_LIMITED) failure else UpstreamFailure.UNAVAILABLE

    private class Metrics {
        var igdbRequests = 0
        var payloadBytes = 0
        var candidates = 0
        var scoringMicros = 0L
        var discoveryFailure: String? = null
        var error: String? = null

        fun record(response: UpstreamOutcome) {
            igdbRequests++
            payloadBytes += (response as? UpstreamOutcome.Ok)?.body?.size ?: 0
        }

        fun event(
            gameId: Int,
            result: RecommendationResult,
            durationMillis: Long,
        ) = "{\"event\":\"recommendations\",\"game_id\":$gameId,\"status\":${result.status}," +
            "\"kind\":\"${result.kind.name.lowercase()}\",\"igdb_requests\":$igdbRequests," +
            "\"payload_bytes\":$payloadBytes,\"candidates\":$candidates,\"results\":${result.games.size}," +
            "\"scoring_us\":$scoringMicros,\"duration_ms\":$durationMillis" +
            (discoveryFailure?.let { ",\"discovery_failure\":\"$it\"" } ?: "") +
            (error?.let { ",\"error\":\"$it\"" } ?: "") + "}"
    }

    companion object {
        const val DEFAULT_LIMIT = 24
        const val MULTIQUERY_PATH = "/v4/multiquery"
        private const val MAX_CACHE_ENTRIES = 512
        private const val LOAD_FACTOR = 0.75f
        private val FRESH = 24.hours
        private val STALE_IF_ERROR = 7.days
        private val DEGRADED = 2.minutes
        private val NOT_FOUND = 1.hours

        // Clients time out after five seconds, so a complete answer must fit well inside that.
        private val SOURCE_TIMEOUT = 3.5.seconds
        private val TOTAL_BUDGET = 4.5.seconds
        private val MIN_DISCOVERY = 800.milliseconds
        private val SLOT_WAIT = 1.seconds
    }
}
