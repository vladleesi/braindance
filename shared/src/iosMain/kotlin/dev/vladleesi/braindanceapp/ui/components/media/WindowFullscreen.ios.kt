// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.Composable

// The viewer already fills the screen; the system bars stay for the clock and the gestures they carry.
@Composable
internal actual fun rememberWindowFullscreen(): WindowFullscreen? = null
