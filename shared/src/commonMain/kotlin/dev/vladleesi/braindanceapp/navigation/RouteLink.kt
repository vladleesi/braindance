// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.navigation3.runtime.NavKey

/**
 * Gives this element the browser behavior of a link to [route]'s Web address (see [toWebPath]); apply it to elements
 * that open a screen, before their click handler. A click, tap, or Enter still runs the element's own action, such as
 * navigating inside the app. On the Web, a middle click, Ctrl-click (⌘-click on macOS), or Shift-click opens the
 * address in a new browser tab or window instead. On Android and iOS it changes nothing.
 */
fun Modifier.routeLink(route: NavKey): Modifier = browserLink(route.toWebPath())

/**
 * [routeLink] for a link inside a text, such as a `LinkAnnotation` span: only presses at positions where [contains]
 * is true, in this element's coordinates, count as presses on the link. Apply it to the text that holds the link.
 */
fun Modifier.routeTextLink(
    route: NavKey,
    contains: (Offset) -> Boolean,
): Modifier = browserLink(route.toWebPath(), contains)

/**
 * Gives an element that opens a page outside the app, such as a store, the browser's middle click: on the Web, a
 * middle click runs [open] like a click does, which opens the page in a new tab. On Android and iOS it changes
 * nothing. Apply it before the element's click handler.
 */
expect fun Modifier.externalLink(open: () -> Unit): Modifier

/**
 * The platform's link behavior for [path], an address of the Web app such as `/games/42`, for presses where
 * [contains] is true; see [routeLink].
 */
internal expect fun Modifier.browserLink(
    path: String,
    contains: ((Offset) -> Boolean)? = null,
): Modifier
