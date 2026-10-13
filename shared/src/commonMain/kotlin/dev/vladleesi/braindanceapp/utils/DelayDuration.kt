// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

enum class DelayDuration(
    val millis: Long,
) {
    ExtraShort(150L),
    Short(200L),
    Medium(400L),
    Large(700L),
    ExtraLarge(2_000L),
}
