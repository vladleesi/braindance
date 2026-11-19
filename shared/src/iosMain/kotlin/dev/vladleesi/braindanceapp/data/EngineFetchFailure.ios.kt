// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data

// The Darwin engine reports every transport failure as an Exception.
internal actual fun Throwable.isEngineFetchFailure(): Boolean = false
