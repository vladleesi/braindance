// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey

/**
 * Gives this element the browser behavior of a link to [route]'s Web address (see [toWebPath]); apply it to elements
 * that open a screen, before their click handler. A click, tap, or Enter still runs the element's own action, such as
 * navigating inside the app. On the Web, a middle click, Ctrl-click (⌘-click on macOS), or Shift-click opens the
 * address in a new browser tab or window instead. On Android and iOS it changes nothing.
 */
fun Modifier.routeLink(route: NavKey): Modifier = browserLink(route.toWebPath())

/** The platform's link behavior for [path], an address of the Web app such as `/games/42`; see [routeLink]. */
internal expect fun Modifier.browserLink(path: String): Modifier
