// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier

/**
 * The one overlay drawn above the whole app, navigation included, such as the media viewer. It is part of the app's
 * own composition rather than a separate dialog window, so platform views inside it, such as a video player, behave
 * as anywhere else in the app.
 */
@Stable
internal class OverlayHost {
    internal var content by mutableStateOf<(@Composable () -> Unit)?>(null)
}

internal val LocalOverlayHost = staticCompositionLocalOf<OverlayHost?> { null }

/** Lays out the app's [content] with [host]'s overlay above it. */
@Composable
internal fun OverlayHostLayout(
    host: OverlayHost,
    content: @Composable () -> Unit,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        content()
        host.content?.invoke()
    }
}

/**
 * Shows [content] above the whole app while this is in the composition, and removes it when this leaves, such as when
 * its screen is closed. Without a host, as in a preview, the content is drawn in place.
 */
@Composable
internal fun FullWindowOverlay(content: @Composable () -> Unit) {
    val host = LocalOverlayHost.current
    if (host == null) {
        content()
        return
    }
    val currentContent by rememberUpdatedState(content)
    DisposableEffect(host) {
        val slot: @Composable () -> Unit = { currentContent() }
        host.content = slot
        onDispose { if (host.content === slot) host.content = null }
    }
}
