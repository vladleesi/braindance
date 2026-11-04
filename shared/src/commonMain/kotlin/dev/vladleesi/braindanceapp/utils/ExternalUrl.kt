// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import androidx.compose.ui.platform.UriHandler
import dev.vladleesi.braindanceapp.logger.BLogger
import io.ktor.http.URLProtocol
import io.ktor.http.parseUrl

private const val HTTPS_PREFIX = "https://"

/**
 * Returns this link when it is safe to open outside the app, otherwise null. Store and giveaway links come from
 * third-party data, so only absolute `https` links with a host and no credentials are allowed; `javascript:`, `data:`,
 * plain `http`, and empty values are rejected.
 */
fun String?.toSafeExternalUrl(): String? {
    val candidate = this?.trim()?.takeIf { it.startsWith(HTTPS_PREFIX, ignoreCase = true) } ?: return null
    val url = parseUrl(candidate) ?: return null
    val safe = url.protocol == URLProtocol.HTTPS && url.host.isNotEmpty() && url.user == null
    return candidate.takeIf { safe }
}

/** Opens [url] in the system handler; a link no installed app can open is logged instead of crashing the app. */
fun UriHandler.openExternalUrl(url: String) {
    try {
        openUri(url)
    } catch (e: IllegalArgumentException) {
        BLogger.warning(throwable = e, message = "No app can open the external link")
    }
}
