// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data

import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
import io.ktor.client.plugins.ResponseException
import kotlinx.coroutines.CancellationException
import kotlinx.io.IOException

/** Why a backend request failed, in terms the UI can explain. Transport details stay in the data layer. */
sealed interface DataError {
    /** The backend could not be reached. */
    data object Network : DataError

    /** The backend did not answer in time. */
    data object Timeout : DataError

    /** The backend has no such item. */
    data object NotFound : DataError

    /** The backend asked the client to slow down. */
    data object RateLimited : DataError

    /** The backend or one of its upstreams failed. */
    data object Server : DataError

    /** Anything else, such as a response this client cannot read. */
    data object Unknown : DataError
}

/** The result of a repository call: a value, or a [DataError] with the [cause] kept for logging. */
sealed interface Outcome<out T> {
    data class Success<T>(
        val value: T,
    ) : Outcome<T>

    data class Failure(
        val error: DataError,
        val cause: Throwable? = null,
    ) : Outcome<Nothing>
}

/** Runs a backend call and maps its failure to a [DataError]. Cancellation is rethrown, never reported as a failure. */
@Suppress("TooGenericExceptionCaught")
suspend fun <T> safeCall(block: suspend () -> T): Outcome<T> =
    try {
        Outcome.Success(block())
    } catch (e: CancellationException) {
        // A request timeout also surfaces as a cancellation; only the caller's own cancellation is rethrown.
        if (e.isTimeout()) Outcome.Failure(DataError.Timeout, e) else throw e
    } catch (e: Exception) {
        Outcome.Failure(e.toDataError(), e)
    }

/** Maps a successful value, keeping a failure as it is. */
inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> =
    when (this) {
        is Outcome.Success -> Outcome.Success(transform(value))
        is Outcome.Failure -> this
    }

private const val NOT_FOUND = 404
private const val TOO_MANY_REQUESTS = 429
private const val FIRST_SERVER_ERROR = 500
private const val LAST_SERVER_ERROR = 599
private val serverErrors = FIRST_SERVER_ERROR..LAST_SERVER_ERROR

internal fun Throwable.toDataError(): DataError =
    when {
        isTimeout() -> DataError.Timeout
        this is ResponseException ->
            when (response.status.value) {
                NOT_FOUND -> DataError.NotFound
                TOO_MANY_REQUESTS -> DataError.RateLimited
                in serverErrors -> DataError.Server
                else -> DataError.Unknown
            }
        this is IOException -> DataError.Network
        else -> DataError.Unknown
    }

private fun Throwable.isTimeout(): Boolean =
    generateSequence(this) { it.cause }.any {
        it is HttpRequestTimeoutException || it is ConnectTimeoutException || it is SocketTimeoutException
    }
