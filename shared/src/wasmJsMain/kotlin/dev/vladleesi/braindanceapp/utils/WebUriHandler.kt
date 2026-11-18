// SPDX-License-Identifier: MPL-2.0

@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.vladleesi.braindanceapp.utils

import androidx.compose.ui.platform.UriHandler

/**
 * Opens links from the Web app as a page's own links would. Web pages open in a new tab without access back to this
 * one (`noopener`), so an outside page cannot redirect the app. A `mailto:` address goes to the system's mail app from
 * this tab instead: opened in a new tab, it would leave that tab empty.
 */
internal object WebUriHandler : UriHandler {
    override fun openUri(uri: String) {
        if (uri.startsWith(MAILTO_SCHEME, ignoreCase = true)) openInThisTab(uri) else openInNewTab(uri)
    }

    private const val MAILTO_SCHEME = "mailto:"
}

// The browser hands a mail address to the mail app and keeps this page.
private fun openInThisTab(uri: String): Unit = js("{ window.location.assign(uri); }")

private fun openInNewTab(uri: String): Unit = js("{ window.open(uri, '_blank', 'noopener'); }")
