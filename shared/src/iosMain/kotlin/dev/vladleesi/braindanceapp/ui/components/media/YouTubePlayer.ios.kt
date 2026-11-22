// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.UIKitInteropProperties
import androidx.compose.ui.viewinterop.UIKitView
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.braindanceapp.utils.toSafeExternalUrl
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.readValue
import platform.CoreGraphics.CGRectZero
import platform.Foundation.NSURL
import platform.UIKit.UIColor
import platform.UIKit.setAccessibilityLabel
import platform.WebKit.WKAudiovisualMediaTypeNone
import platform.WebKit.WKNavigationAction
import platform.WebKit.WKNavigationActionPolicy
import platform.WebKit.WKNavigationDelegateProtocol
import platform.WebKit.WKUIDelegateProtocol
import platform.WebKit.WKWebView
import platform.WebKit.WKWebViewConfiguration
import platform.WebKit.WKWebsiteDataStore
import platform.WebKit.WKWindowFeatures
import platform.darwin.NSObject

private const val BLANK_PAGE = "about:blank"

/**
 * The player page (see [playerPageHtml]) in one WKWebView that the player keeps for its whole life, loading each video
 * into it and unloading it when stopped. It plays inline and keeps no website data after it is released. Only the
 * app's own page loads and YouTube's player frame load in it; a link that would leave it, such as YouTube's logo,
 * opens in the YouTube app or the browser, and the page's failure address notifies [state]. The page has no message
 * handler to call.
 */
@OptIn(ExperimentalForeignApi::class)
@Composable
internal actual fun YouTubePlayer(
    video: GameMedia.YouTubeVideo?,
    title: String,
    coveredTop: Dp,
    state: MediaGalleryState,
    scrollState: ScrollState?,
) {
    val currentState by rememberUpdatedState(state)
    val uriHandler = LocalUriHandler.current
    val openLink by rememberUpdatedState<(String) -> Unit>({ url ->
        url.toSafeExternalUrl()?.let(uriHandler::openExternalUrl)
    })
    // WKWebView holds its delegates weakly; the composition keeps this one for the view's lifetime.
    val player =
        remember {
            PlayerController(
                onOpenLink = { url -> openLink(url) },
                onFailed = { failed -> currentState.onPlaybackFailed(failed) },
            )
        }
    UIKitView(
        factory = {
            val configuration =
                WKWebViewConfiguration().apply {
                    allowsInlineMediaPlayback = true
                    // The user pressed play to start a video, so it may play with sound.
                    mediaTypesRequiringUserActionForPlayback = WKAudiovisualMediaTypeNone
                    websiteDataStore = WKWebsiteDataStore.nonPersistentDataStore()
                }
            WKWebView(frame = CGRectZero.readValue(), configuration = configuration).apply {
                navigationDelegate = player
                UIDelegate = player
                backgroundColor = UIColor.blackColor
                scrollView.scrollEnabled = false
            }
        },
        modifier = Modifier.collapsedWhenStopped(video).fillMaxSize(),
        update = { view ->
            view.setAccessibilityLabel(title)
            player.show(view, video)
        },
        onRelease = { view ->
            player.show(view, null)
            view.stopLoading()
            view.navigationDelegate = null
            view.UIDelegate = null
        },
        properties = UIKitInteropProperties(isNativeAccessibilityEnabled = true),
    )
}

/**
 * Loads the shown video's page into the web view, lets the app's own page loads and YouTube's frame load, reports the
 * page's failure address to [onFailed], and sends every other link to [onOpenLink].
 */
private class PlayerController(
    private val onOpenLink: (String) -> Unit,
    private val onFailed: (GameMedia.YouTubeVideo) -> Unit,
) : NSObject(),
    WKNavigationDelegateProtocol,
    WKUIDelegateProtocol {
    // The video whose page is loaded, or null while the player is stopped.
    private var shown: GameMedia.YouTubeVideo? = null

    /** Loads [video]'s page into [view], or unloads the page when [video] is null, which ends playback. */
    fun show(
        view: WKWebView,
        video: GameMedia.YouTubeVideo?,
    ) {
        if (video == shown) return
        shown = video
        // A stopped player is hidden as well as unloaded, so no last frame stays over the poster.
        view.hidden = video == null
        if (video == null) {
            view.loadHTMLString("", baseURL = null)
        } else {
            view.loadHTMLString(video.playerPageHtml(), baseURL = NSURL(string = PLAYER_PAGE_BASE_URL))
        }
    }

    override fun webView(
        webView: WKWebView,
        decidePolicyForNavigationAction: WKNavigationAction,
        decisionHandler: (WKNavigationActionPolicy) -> Unit,
    ) {
        val inMainFrame = decidePolicyForNavigationAction.targetFrame?.mainFrame ?: true
        val url = decidePolicyForNavigationAction.request.URL?.absoluteString
        val allowed =
            when {
                // Frames inside the page are YouTube's player.
                !inMainFrame -> true
                // The app's own loads: a video's page at its base address, or the empty page that stops it.
                url == PLAYER_PAGE_BASE_URL || url == BLANK_PAGE -> true
                url == PLAYER_ERROR_URL -> {
                    shown?.let(onFailed)
                    false
                }
                else -> {
                    url?.let(onOpenLink)
                    false
                }
            }
        decisionHandler(
            if (allowed) {
                WKNavigationActionPolicy.WKNavigationActionPolicyAllow
            } else {
                WKNavigationActionPolicy.WKNavigationActionPolicyCancel
            },
        )
    }

    // A link that asks for a new window, such as "Watch on YouTube", opens outside the app instead.
    override fun webView(
        webView: WKWebView,
        createWebViewWithConfiguration: WKWebViewConfiguration,
        forNavigationAction: WKNavigationAction,
        windowFeatures: WKWindowFeatures,
    ): WKWebView? {
        forNavigationAction.request.URL
            ?.absoluteString
            ?.let(onOpenLink)
        return null
    }
}
