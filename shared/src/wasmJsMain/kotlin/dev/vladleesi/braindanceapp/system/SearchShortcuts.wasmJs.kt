// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent

/**
 * Listens on the window, which receives the keys Compose leaves unhandled. "/" is typed text while a field has the
 * focus, so it only counts outside fields. Ctrl+K and ⌘K are never text. A handled key does not reach the browser,
 * which would otherwise open its quick find or address bar search. Keys pressed while an input method is composing
 * text are left alone.
 */
@Composable
internal actual fun SearchShortcuts(onShortcut: () -> Unit) {
    val currentOnShortcut by rememberUpdatedState(onShortcut)
    DisposableEffect(Unit) {
        val listener: (Event) -> Unit = { event ->
            val key = event as? KeyboardEvent
            if (key != null && isSearchShortcut(key)) {
                key.preventDefault()
                currentOnShortcut()
            }
        }
        window.addEventListener("keydown", listener)
        onDispose { window.removeEventListener("keydown", listener) }
    }
}

private fun isSearchShortcut(event: KeyboardEvent): Boolean {
    if (event.defaultPrevented || event.isComposing || event.altKey) return false
    return if (event.ctrlKey || event.metaKey) {
        !event.shiftKey && event.key.equals("k", ignoreCase = true)
    } else {
        event.key == "/" && !isEditingText()
    }
}
