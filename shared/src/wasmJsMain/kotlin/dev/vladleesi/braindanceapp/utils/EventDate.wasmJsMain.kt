// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import kotlin.js.ExperimentalWasmJsInterop

internal actual fun formatEventDate(
    epochSeconds: Long,
    timeOnly: Boolean,
): String = formatBrowserDate(epochSeconds.toDouble(), timeOnly)

@OptIn(ExperimentalWasmJsInterop::class)
private fun formatBrowserDate(
    epochSeconds: Double,
    timeOnly: Boolean,
): String =
    js(
        """new Intl.DateTimeFormat(undefined, timeOnly ? {timeStyle: 'short'} :
        {dateStyle: 'medium', timeStyle: 'short'}).format(new Date(epochSeconds * 1000))""",
    )
