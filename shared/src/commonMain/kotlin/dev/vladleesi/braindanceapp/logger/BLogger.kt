// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.logger

import io.github.aakira.napier.DebugAntilog
import io.github.aakira.napier.Napier

object BLogger {
    init {
        Napier.base(DebugAntilog("Braindance"))
    }

    fun debug(
        throwable: Throwable? = null,
        tag: String? = null,
        message: String,
    ) {
        Napier.d(message = message, throwable = throwable, tag = tag)
    }

    fun warning(
        throwable: Throwable? = null,
        tag: String? = null,
        message: String,
    ) {
        Napier.w(message = message, throwable = throwable, tag = tag)
    }

    fun error(
        throwable: Throwable? = null,
        tag: String? = null,
        message: String,
    ) {
        Napier.e(message = message, throwable = throwable, tag = tag)
    }
}
