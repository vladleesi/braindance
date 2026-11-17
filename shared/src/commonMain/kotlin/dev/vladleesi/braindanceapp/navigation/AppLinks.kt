// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation3.runtime.NavKey
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow

/** The Web app's address. Android App Links and iOS Universal Links open the mobile app for links to it. */
const val APP_LINK_ORIGIN = "https://app.braindancegg.com"

// Links longer than this are not app addresses; the bound keeps parsing cheap for arbitrary input.
private const val MAX_APP_LINK_LENGTH = 2048

/**
 * The screen a link to the Web app opens, or null for anything else. Only `https` links to [APP_LINK_ORIGIN] itself
 * count: no other scheme, host, port, or user information. The path is read like a browser address
 * ([webPathToRoute]), so a link opens the same screen in every app; a query or fragment is ignored. The link is only
 * ever turned into a route, never opened or followed.
 */
fun appLinkRoute(url: String?): NavKey? {
    if (url == null || url.length > MAX_APP_LINK_LENGTH) return null
    // Scheme and host are case-insensitive; the path is not.
    if (!url.startsWith(APP_LINK_ORIGIN, ignoreCase = true)) return null
    val rest = url.substring(APP_LINK_ORIGIN.length)
    // Anything but the end, a path, a query, or a fragment would make the origin's host part of a longer host name or
    // add a port.
    if (rest.isNotEmpty() && rest.first() !in "/?#") return null
    val path = rest.substringBefore('?').substringBefore('#')
    return webPathToRoute(path)
}

/**
 * Links to the Web app that the system handed to a running app, such as an App Link on Android or a Universal Link
 * on iOS. The host passes them to [open]; [BraindanceApp][dev.vladleesi.braindanceapp.BraindanceApp] opens each one
 * once. A link that arrives before the app's screen is composed waits for it; of several waiting links, only the
 * newest is opened, as the user last asked for it.
 */
class AppLinkInbox {
    private val links = Channel<String>(Channel.CONFLATED)

    internal val received: Flow<String> = links.receiveAsFlow()

    fun open(url: String) {
        links.trySend(url)
    }
}

/**
 * Opens each link from [inbox] like an address typed into the browser: a tab's link switches to that tab, any other
 * screen opens on top of the current tab. Links this app does not serve leave the current screen as it is.
 */
@Composable
internal fun AppLinkHandler(
    inbox: AppLinkInbox?,
    navigator: Navigator,
) {
    inbox ?: return
    LaunchedEffect(inbox, navigator) {
        inbox.received.collect { url ->
            appLinkRoute(url)?.let(navigator::openLink)
        }
    }
}
