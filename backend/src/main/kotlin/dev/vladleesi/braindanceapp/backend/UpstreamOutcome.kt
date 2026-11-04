// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

/** Why a request backed by IGDB or GamerPower has no data to return, with the HTTP status the API answers it with. */
enum class UpstreamFailure(
    val status: Int,
) {
    BAD_REQUEST(400),
    NOT_FOUND(404),
    RATE_LIMITED(429),
    UNAVAILABLE(502),
    BUSY(503),
    TIMEOUT(504),
}

/** The answer to a request backed by IGDB or GamerPower. [status] is the HTTP status the API responds with. */
sealed interface UpstreamOutcome {
    val status: Int

    /** Not a data class: generated equality would compare the byte array by reference. */
    class Ok(
        val body: ByteArray,
    ) : UpstreamOutcome {
        override val status: Int get() = OK_STATUS
    }

    data class Failed(
        val failure: UpstreamFailure,
    ) : UpstreamOutcome {
        override val status: Int get() = failure.status
    }

    private companion object {
        const val OK_STATUS = 200
    }
}
