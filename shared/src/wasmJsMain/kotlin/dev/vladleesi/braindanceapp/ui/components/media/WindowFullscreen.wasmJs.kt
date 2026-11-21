// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.browser.document
import org.w3c.dom.events.Event

/**
 * The browser's Fullscreen API for the page, where the browser allows it (`document.fullscreenEnabled`); some
 * browsers, such as Safari on iPhone, do not. Without it the viewer still fills the browser window. The browser leaves
 * fullscreen on Esc by itself, before the page sees the key, so the first Esc ends fullscreen and the next closes
 * the viewer.
 */
@Composable
internal actual fun rememberWindowFullscreen(): WindowFullscreen? {
    if (!fullscreenEnabled()) return null
    val fullscreen = remember { BrowserFullscreen() }
    DisposableEffect(fullscreen) {
        val onChange: (Event) -> Unit = { fullscreen.isActive = hasFullscreenElement() }
        document.addEventListener("fullscreenchange", onChange)
        onDispose {
            document.removeEventListener("fullscreenchange", onChange)
            if (fullscreen.entered && hasFullscreenElement()) exitFullscreen()
        }
    }
    return fullscreen
}

private class BrowserFullscreen : WindowFullscreen {
    override var isActive by mutableStateOf(hasFullscreenElement())

    // Whether this viewer requested fullscreen, so closing it leaves fullscreen the user started elsewhere alone.
    var entered = false

    override fun enter() {
        entered = true
        // Called from a click or key handler, which the browser requires for fullscreen.
        requestFullscreen()
    }

    override fun exit() {
        if (hasFullscreenElement()) exitFullscreen()
    }
}

private fun fullscreenEnabled(): Boolean = js("document.fullscreenEnabled === true")

private fun hasFullscreenElement(): Boolean = js("document.fullscreenElement != null")

// A rejected request, such as without a user action, leaves the page as it is.
private fun requestFullscreen(): Unit = js("{ document.documentElement.requestFullscreen().catch(function () {}); }")

private fun exitFullscreen(): Unit = js("{ document.exitFullscreen().catch(function () {}); }")
