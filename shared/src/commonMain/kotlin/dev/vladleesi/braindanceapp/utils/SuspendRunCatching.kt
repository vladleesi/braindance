// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import kotlinx.coroutines.CancellationException

/**
 * Like [runCatching] for suspending work, but rethrows [CancellationException], so a cancelled load stops instead of
 * being reported as a failure.
 */
@Suppress("TooGenericExceptionCaught")
suspend inline fun <T> suspendRunCatching(block: () -> T): Result<T> =
    try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }
