// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.ui.Modifier

// Links open inside the app here; there are no browser tabs to open them in.
internal actual fun Modifier.browserLink(path: String): Modifier = this
