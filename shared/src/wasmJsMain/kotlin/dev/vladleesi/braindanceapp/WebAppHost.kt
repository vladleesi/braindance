// SPDX-License-Identifier: MPL-2.0

@file:OptIn(ExperimentalResourceApi::class, InternalComposeUiApi::class, ExperimentalWasmJsInterop::class)

package dev.vladleesi.braindanceapp

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.platform.LocalPlatformPrefetchScheduler
import androidx.compose.ui.platform.LocalUriHandler
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.allPluralStringResources
import dev.vladleesi.braindanceapp.resources.allStringResources
import dev.vladleesi.braindanceapp.ui.components.PageAutoscrollHost
import dev.vladleesi.braindanceapp.ui.style.AppFonts
import dev.vladleesi.braindanceapp.utils.WebPrefetchScheduler
import dev.vladleesi.braindanceapp.utils.WebResourceReader
import dev.vladleesi.braindanceapp.utils.WebUriHandler
import dev.vladleesi.braindanceapp.utils.deleteComposeResourceCache
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.LocalResourceReader
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.preloadFont
import org.jetbrains.compose.resources.stringResource

/**
 * Hosts [content] in the Web app: it reads resources with [WebResourceReader], runs lazy layouts' prefetching with
 * [WebPrefetchScheduler], opens links with [WebUriHandler], adds middle-click autoscrolling with [PageAutoscrollHost],
 * and shows [content] only once its fonts and strings are loaded.
 *
 * On the Web, Compose loads fonts and strings asynchronously and draws empty text, then text in a fallback font, until
 * they arrive. The page's loading screen (`#boot` in index.html) therefore stays up until every string and the
 * typography's fonts are in Compose's caches; from then on, every screen composes with its text from the first frame.
 * If a resource fails to load, the app starts anyway with what it has.
 */
@Composable
fun WebAppHost(content: @Composable () -> Unit) {
    val prefetchScheduler = remember { WebPrefetchScheduler() }
    CompositionLocalProvider(
        LocalResourceReader provides WebResourceReader,
        LocalPlatformPrefetchScheduler provides prefetchScheduler,
        LocalUriHandler provides WebUriHandler,
    ) {
        if (rememberStartupResourcesLoaded()) {
            PageAutoscrollHost(content)
            // Runs once this composition is applied. Compose draws it in the same animation frame, before the browser
            // paints, so that paint shows the content without the loading screen.
            SideEffect(::removeLoadingScreen)
            LaunchedEffect(Unit) { deleteComposeResourceCache() }
        }
    }
}

/**
 * Loads every string and the typography's fonts into Compose's resource caches and reports whether they are all
 * there, or whether loading failed. Each string is decoded once; reading the one bundled strings file takes one
 * request, which the page preloads.
 */
@Composable
private fun rememberStartupResourcesLoaded(): Boolean {
    var loaded by remember { mutableStateOf(false) }
    if (loaded) return true
    val fontsLoaded = AppFonts.map { font -> preloadFont(font.resource, font.weight).value }.all { it != null }
    val stringsLoaded =
        Res.allStringResources.values
            .map { resource ->
                stringResource(resource)
            }.all(String::isNotEmpty)
    val pluralsLoaded =
        Res.allPluralStringResources.values
            .map { resource -> pluralStringResource(resource, quantity = 1) }
            .all(String::isNotEmpty)
    val ready = (fontsLoaded && stringsLoaded && pluralsLoaded) || WebResourceReader.hasFailedRead
    // Stops composing the loaders; the caches keep what they loaded.
    if (ready) SideEffect { loaded = true }
    return ready
}

private fun removeLoadingScreen(): Unit = js("document.getElementById('boot')?.remove()")
