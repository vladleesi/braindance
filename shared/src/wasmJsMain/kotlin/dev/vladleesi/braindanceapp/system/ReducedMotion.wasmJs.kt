// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.window
import org.w3c.dom.events.Event

private const val REDUCED_MOTION_QUERY = "(prefers-reduced-motion: reduce)"

/** Follows the setting while the page is open, so changing it takes effect without a reload. */
@Composable
internal actual fun prefersReducedMotion(): Boolean {
    val query = remember { window.matchMedia(REDUCED_MOTION_QUERY) }
    var reduced by remember { mutableStateOf(query.matches) }
    DisposableEffect(query) {
        val listener: (Event) -> Unit = { reduced = query.matches }
        query.addEventListener("change", listener)
        onDispose { query.removeEventListener("change", listener) }
    }
    return reduced
}
