// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

enum class DelayDuration(
    val duration: Duration,
) {
    ExtraShort(150L.milliseconds),
    Short(200L.milliseconds),
    Medium(400L.milliseconds),
    Large(700L.milliseconds),
    ExtraLarge(2_000L.milliseconds),
}
