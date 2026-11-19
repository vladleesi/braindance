// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset

// Links open inside the app here; there are no browser tabs to open them in.
internal actual fun Modifier.browserLink(
    path: String,
    contains: ((Offset) -> Boolean)?,
): Modifier = this

// Android and iOS have no middle click or browser tabs.
actual fun Modifier.externalLink(open: () -> Unit): Modifier = this
