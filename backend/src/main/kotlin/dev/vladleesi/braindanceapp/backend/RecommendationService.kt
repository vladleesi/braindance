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
import java.util.concurrent.TimeUnit

/** Outcome of a recommendation request. Only [Kind.COMPLETE] results may be cached publicly. */
internal data class RecommendationResult(
    val status: Int,
    val games: List<Recommendation> = emptyList(),
    val kind: Kind = Kind.COMPLETE,
) {
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
        val now = clock.millis()
        cached(gameId)?.takeIf { now < it.freshUntil }?.let { return it.result }
        // Waiters can cancel independently; the shared computation is bounded by its own timeouts.
        val computation =
            inFlight.computeIfAbsent(gameId) {
                scope.async(start = CoroutineStart.LAZY) {
                    try {
                        refresh(gameId)
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
        val started = System.nanoTime()
        val metrics = Metrics()
        val computed =
            try {
                compute(gameId, started, metrics)
            } catch (e: CancellationException) {
                if (e is TimeoutCancellationException) RecommendationResult(GATEWAY_TIMEOUT) else throw e
            } catch (e: Exception) {
                metrics.error = e::class.simpleName
                RecommendationResult(BAD_GATEWAY)
            }
        val result =
            if (computed.status !in setOf(OK, NOT_FOUND)) {
                staleFallback(gameId) ?: computed
            } else {
                computed
            }
        store(gameId, result)
        log(metrics.event(gameId, result, TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)))
        return result
    }

    private suspend fun compute(
        gameId: Int,
        started: Long,
        metrics: Metrics,
    ): RecommendationResult {
        val sourceResponse =
            withTimeout(SOURCE_TIMEOUT_MILLIS) {
                igdb.execute(MULTIQUERY_PATH, RecommendationQueries.sourceQuery(gameId), SLOT_WAIT_MILLIS)
            }
        metrics.record(sourceResponse)
        if (sourceResponse.status != OK) return RecommendationResult(errorStatus(sourceResponse.status))
        val graph =
            RecommendationQueries.parseSourceGraph(
                RecommendationQueries.parseMultiquery(checkNotNull(sourceResponse.body)),
            ) ?: return RecommendationResult(NOT_FOUND)

        val discoveryQuery =
            RecommendationQueries.discoveryQuery(graph, RecommendationRanking.retrievalKeywords(graph))
        val elapsed = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started)
        val budget = (TOTAL_BUDGET_MILLIS - elapsed).coerceAtLeast(MIN_DISCOVERY_MILLIS)
        // Without any usable discovery filter the graph neighbors are the complete candidate set.
        val discovered = if (discoveryQuery == null) emptyList() else discover(discoveryQuery, budget, metrics)
        val scoringStarted = System.nanoTime()
        val pool = RecommendationRanking.candidatePool(graph, discovered.orEmpty())
        val ranked = RecommendationRanking.rank(graph, pool)
        metrics.scoringMicros = TimeUnit.NANOSECONDS.toMicros(System.nanoTime() - scoringStarted)
        metrics.candidates = pool.size
        val kind = if (discovered == null) RecommendationResult.Kind.DEGRADED else RecommendationResult.Kind.COMPLETE
        return RecommendationResult(OK, ranked, kind)
    }

    /** Returns null when discovery fails, so the caller falls back to native graph neighbors. */
    private suspend fun discover(
        query: String,
        budgetMillis: Long,
        metrics: Metrics,
    ): List<IgdbGame>? {
        val response =
            withTimeoutOrNull(budgetMillis) {
                igdb.execute(MULTIQUERY_PATH, query, SLOT_WAIT_MILLIS.coerceAtMost(budgetMillis / 2))
            }
        if (response == null) {
            metrics.discoveryFailure = "timeout"
            return null
        }
        metrics.record(response)
        if (response.status != OK) {
            metrics.discoveryFailure = response.status.toString()
            return null
        }
        return try {
            RecommendationQueries
                .parseMultiquery(checkNotNull(response.body))
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
                result.status == OK && result.kind == RecommendationResult.Kind.COMPLETE ->
                    CacheEntry(result, now + FRESH_MILLIS, now + STALE_IF_ERROR_MILLIS)
                result.status == OK && result.kind == RecommendationResult.Kind.DEGRADED ->
                    CacheEntry(result, now + DEGRADED_MILLIS, now + DEGRADED_MILLIS)
                result.status == NOT_FOUND -> CacheEntry(result, now + DEGRADED_MILLIS, now + DEGRADED_MILLIS)
                // Keep the expired complete entry for later stale-if-error responses; do not cache failures.
                else -> return
            }
        synchronized(cache) { cache[gameId] = entry }
    }

    private fun errorStatus(status: Int) = if (status == TOO_MANY_REQUESTS) TOO_MANY_REQUESTS else BAD_GATEWAY

    private class Metrics {
        var igdbRequests = 0
        var payloadBytes = 0
        var candidates = 0
        var scoringMicros = 0L
        var discoveryFailure: String? = null
        var error: String? = null

        fun record(response: ApiResult) {
            igdbRequests++
            payloadBytes += response.body?.size ?: 0
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
        private const val FRESH_MILLIS = 24 * 60 * 60 * 1000L
        private const val STALE_IF_ERROR_MILLIS = 7 * 24 * 60 * 60 * 1000L
        private const val DEGRADED_MILLIS = 2 * 60 * 1000L

        // Clients time out after five seconds, so a complete answer must fit well inside that.
        private const val SOURCE_TIMEOUT_MILLIS = 3500L
        private const val TOTAL_BUDGET_MILLIS = 4500L
        private const val MIN_DISCOVERY_MILLIS = 800L
        private const val SLOT_WAIT_MILLIS = 1000L

        private const val OK = 200
        private const val NOT_FOUND = 404
        private const val TOO_MANY_REQUESTS = 429
        private const val BAD_GATEWAY = 502
        private const val GATEWAY_TIMEOUT = 504
    }
}
