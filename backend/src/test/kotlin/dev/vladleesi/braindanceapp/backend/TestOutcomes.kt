// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.backend

/** The body of a successful outcome, or null for a failure; lets tests read results without casts. */
internal fun UpstreamOutcome.bodyOrNull(): ByteArray? = (this as? UpstreamOutcome.Ok)?.body
