// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data

import io.ktor.client.engine.js.JsError

// Ktor's fetch engine rejects with `Error("Fail to fetch", JsError(reason))` (Ktor 3.6.1, compatibility/Utils.kt).
internal actual fun Throwable.isEngineFetchFailure(): Boolean = this is Error && cause is JsError
