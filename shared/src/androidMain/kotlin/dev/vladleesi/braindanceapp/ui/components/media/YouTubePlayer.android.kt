// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.braindanceapp.utils.toSafeExternalUrl

/**
 * The player page (see [playerPageHtml]) in one WebView that the player keeps for its whole life, loading each video
 * into it and unloading it when stopped. It runs only the page's script and YouTube's: no JavaScript interface is
 * added, and the page cannot navigate itself away. A link that would leave it, such as YouTube's logo, opens in the
 * YouTube app or the browser; the page's failure address notifies [state]. The WebView pauses with the screen, as a
 * browser does for a background tab, and is destroyed once it has left the composition.
 */
@Composable
internal actual fun YouTubePlayer(
    video: GameMedia.YouTubeVideo?,
    title: String,
    coveredTop: Dp,
    state: MediaGalleryState,
    scrollState: ScrollState?,
) {
    val uriHandler = LocalUriHandler.current
    val currentState by rememberUpdatedState(state)
    val openLink by rememberUpdatedState<(String) -> Unit>({ url ->
        url.toSafeExternalUrl()?.let(uriHandler::openExternalUrl)
    })
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    AndroidView(
        factory = { context ->
            PlayerWebView(
                context = context,
                lifecycle = lifecycle,
                onOpenLink = { url -> openLink(url) },
                onFailed = { failed -> currentState.onPlaybackFailed(failed) },
            )
        },
        modifier =
            Modifier.collapsedWhenStopped(video).fillMaxSize().then(
                if (video !=
                    null
                ) {
                    Modifier.semantics { contentDescription = title }
                } else {
                    Modifier.clearAndSetSemantics {}
                },
            ),
        update = { view -> view.show(video) },
        onRelease = PlayerWebView::release,
    )
}

/**
 * A WebView for the player page that pauses with [lifecycle] until it is released. JavaScript runs only the app's own
 * player page and YouTube's player; no interface is exposed to it.
 */
@SuppressLint("SetJavaScriptEnabled", "ViewConstructor")
private class PlayerWebView(
    context: Context,
    private val lifecycle: Lifecycle,
    onOpenLink: (String) -> Unit,
    onFailed: (GameMedia.YouTubeVideo) -> Unit,
) : WebView(context) {
    // The video whose page is loaded, or null while the player is stopped.
    private var shown: GameMedia.YouTubeVideo? = null

    private val lifecycleObserver =
        LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> onPause()
                Lifecycle.Event.ON_RESUME -> onResume()
                else -> Unit
            }
        }

    init {
        // Without this, the WebView wraps its content, so the page's 100% height is 0 and the player stays empty.
        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        setBackgroundColor(Color.BLACK)
        settings.apply {
            javaScriptEnabled = true
            // The user pressed play to start a video, so it may play with sound.
            mediaPlaybackRequiresUserGesture = false
            allowFileAccess = false
            allowContentAccess = false
            setSupportMultipleWindows(false)
        }
        webViewClient =
            object : WebViewClient() {
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest,
                ): Boolean {
                    // The player's own frame may navigate; the page itself never leaves the player.
                    if (!request.isForMainFrame) return false
                    val url = request.url.toString()
                    if (url == PLAYER_ERROR_URL) shown?.let(onFailed) else onOpenLink(url)
                    return true
                }
            }
        lifecycle.addObserver(lifecycleObserver)
    }

    /**
     * Loads [video]'s page, or unloads the page and hides the view when [video] is null, which ends playback. Hiding
     * matters: a WebView shrunk to no size still draws its last frame over the preview.
     */
    fun show(video: GameMedia.YouTubeVideo?) {
        if (video == shown) return
        shown = video
        visibility = if (video == null) INVISIBLE else VISIBLE
        if (video == null) {
            loadUrl(BLANK_PAGE)
        } else {
            loadDataWithBaseURL(PLAYER_PAGE_BASE_URL, video.playerPageHtml(), "text/html", "utf-8", null)
        }
    }

    /**
     * Stops playback at once and destroys the WebView after the current frame. Compose is still detaching the view
     * when this runs; destroying it here makes it request a layout of a node that no longer has an owner.
     */
    fun release() {
        lifecycle.removeObserver(lifecycleObserver)
        shown = null
        stopLoading()
        onPause()
        webViewClient = WebViewClient()
        mainHandler.post { destroy() }
    }

    private companion object {
        const val BLANK_PAGE = "about:blank"
        val mainHandler = Handler(Looper.getMainLooper())
    }
}
