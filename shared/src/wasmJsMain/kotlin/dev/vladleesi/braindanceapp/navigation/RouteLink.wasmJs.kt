// SPDX-License-Identifier: MPL-2.0

@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isMetaPressed
import androidx.compose.ui.input.pointer.isOutOfBounds
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.isShiftPressed
import androidx.compose.ui.input.pointer.isTertiaryPressed
import androidx.compose.ui.input.pointer.pointerInput

/** What a mouse press on a link does, following the browser's conventions for links. */
internal enum class LinkPress {
    /** The element's own action, such as navigating inside the app. */
    Activate,

    /** Opens the address in a new tab or window when the button is released over the link. */
    OpenInNewTab,

    /** Nothing: on Apple systems a Ctrl-click is a secondary click, which opens no screen. */
    Ignore,
}

/**
 * Classifies a mouse press on a link. A middle click and a Shift-click open a new tab or window, as does a Ctrl-click
 * on Windows and Linux or a ⌘-click on Apple systems.
 */
@Suppress("LongParameterList") // Named flags read more clearly here than a wrapper type.
internal fun linkPress(
    primary: Boolean,
    middle: Boolean,
    ctrl: Boolean,
    meta: Boolean,
    shift: Boolean,
    isApplePlatform: Boolean,
): LinkPress =
    when {
        middle -> LinkPress.OpenInNewTab
        !primary -> LinkPress.Activate
        ctrl && isApplePlatform -> LinkPress.Ignore
        shift || (if (isApplePlatform) meta else ctrl) -> LinkPress.OpenInNewTab
        else -> LinkPress.Activate
    }

/**
 * The canvas that Compose draws in is not a link, so the browser cannot offer link behavior by itself. This modifier
 * watches mouse presses before the element's own click handler sees them. For a new tab, it opens the address with
 * `window.open` while the browser still handles the release, so the browser applies its own rules for the click (for
 * example, a background tab for a middle click in Chrome), and consumes the release so the element's click handler
 * does not also navigate.
 */
internal actual fun Modifier.browserLink(
    path: String,
    contains: ((Offset) -> Boolean)?,
): Modifier =
    pointerInput(path, contains) {
        handleLinkPresses(contains, middleClickOnly = false) { openInNewTab(webAddress(path)) }
    }

// A click on an external link already opens a new tab through WebUriHandler, so only the middle click needs adding.
actual fun Modifier.externalLink(open: () -> Unit): Modifier =
    pointerInput(open) { handleLinkPresses(contains = null, middleClickOnly = true, openInNewTab = open) }

private suspend fun PointerInputScope.handleLinkPresses(
    contains: ((Offset) -> Boolean)?,
    middleClickOnly: Boolean,
    openInNewTab: () -> Unit,
) {
    awaitEachGesture {
        // The initial pass runs before the element's own click handler, which listens in the main pass.
        val press = awaitPointerEvent(PointerEventPass.Initial)
        val change = press.changes.first()
        if (press.type != PointerEventType.Press || change.type != PointerType.Mouse) return@awaitEachGesture
        if (contains != null && !contains(change.position)) return@awaitEachGesture
        val middle = press.buttons.isTertiaryPressed
        val action = if (middleClickOnly && !middle) LinkPress.Activate else press.linkPress()
        when (action) {
            LinkPress.Activate -> Unit
            LinkPress.Ignore -> consumeRelease(press, contains)
            LinkPress.OpenInNewTab -> {
                // A middle press on a link belongs to the link, so the page's autoscroll (PageAutoscroll) leaves it.
                if (middle) change.consume()
                if (consumeRelease(press, contains)) openInNewTab()
            }
        }
    }
}

/**
 * Waits for the press's pointer to be released and consumes the release; returns whether it was over this element
 * and, with [contains], over the link.
 */
private suspend fun AwaitPointerEventScope.consumeRelease(
    press: PointerEvent,
    contains: ((Offset) -> Boolean)?,
): Boolean {
    val pointer = press.changes.first().id
    while (true) {
        val event = awaitPointerEvent(PointerEventPass.Initial)
        val change = event.changes.firstOrNull { it.id == pointer } ?: return false
        if (!change.pressed) {
            change.consume()
            return !change.isOutOfBounds(size, extendedTouchPadding) && contains?.invoke(change.position) != false
        }
    }
}

private fun PointerEvent.linkPress(): LinkPress =
    linkPress(
        primary = buttons.isPrimaryPressed,
        middle = buttons.isTertiaryPressed,
        ctrl = keyboardModifiers.isCtrlPressed,
        meta = keyboardModifiers.isMetaPressed,
        shift = keyboardModifiers.isShiftPressed,
        isApplePlatform = isApplePlatform,
    )

// The modifier keys for links depend on the operating system, not on the browser or the window size.
private val isApplePlatform: Boolean by lazy { isApplePlatform() }

private fun isApplePlatform(): Boolean = js("/Mac|iPhone|iPad|iPod/.test(navigator.platform)")

/** The absolute address of [path] under the page's base path, for example `/games/42`. */
private fun webAddress(path: String): String = js("new URL(path.replace(/^\\//, ''), document.baseURI).href")

private fun openInNewTab(url: String): Unit = js("{ window.open(url, '_blank', 'noopener'); }")
