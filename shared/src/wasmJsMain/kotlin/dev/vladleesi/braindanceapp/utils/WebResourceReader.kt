// SPDX-License-Identifier: MPL-2.0

@file:OptIn(ExperimentalWasmJsInterop::class, ExperimentalResourceApi::class)

package dev.vladleesi.braindanceapp.utils

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.vladleesi.braindanceapp.logger.BLogger
import kotlinx.browser.document
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.await
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.MissingResourceException
import org.jetbrains.compose.resources.ResourceReader
import org.khronos.webgl.ArrayBuffer
import org.khronos.webgl.Int8Array
import org.khronos.webgl.toByteArray
import org.w3c.dom.url.URL
import org.w3c.fetch.Response
import kotlin.js.Promise

/**
 * Reads the app's Compose resources (fonts, strings, and icons) for the Web app, provided through
 * `LocalResourceReader` (see [WebAppHost]).
 *
 * Compose's own Web reader keeps a copy of every file in the browser's Cache Storage. It clears that cache on every
 * page load and then reads each string through it one at a time, so the first screen showed no text until dozens of
 * cache lookups had finished. This reader requests each file once at its fingerprinted address
 * ([versionedResourceUrl]), which the browser keeps as an immutable file and the page preloads, and holds string
 * files in memory, so every further string is a slice of bytes already loaded.
 */
internal object WebResourceReader : ResourceReader {
    // The files are loaded for the page's lifetime, so the requests belong to no screen.
    private val scope = CoroutineScope(SupervisorJob())

    // String files, read whole once; strings are slices of them.
    private val stringFiles = mutableMapOf<String, Deferred<ByteArray>>()

    /** Whether any resource failed to load, so the app should start without waiting for the rest. */
    var hasFailedRead by mutableStateOf(false)
        private set

    override suspend fun read(path: String): ByteArray = load(path)

    override suspend fun readPart(
        path: String,
        offset: Long,
        size: Long,
    ): ByteArray {
        val file = stringFiles.getOrPut(path) { scope.async { load(path) } }
        val bytes =
            try {
                file.await()
            } catch (e: MissingResourceException) {
                // A later read requests the file again instead of repeating this failure.
                if (stringFiles[path] === file) stringFiles.remove(path)
                throw e
            }
        val end = offset + size
        if (offset < 0 || size < 0 || end > bytes.size) throw MissingResourceException(path)
        return bytes.copyOfRange(offset.toInt(), end.toInt())
    }

    override fun getUri(path: String): String = URL(versionedResourceUrl(path), document.baseURI).href

    private suspend fun load(path: String): ByteArray =
        try {
            val response = fetchResource(versionedResourceUrl(path)).await<Response>()
            if (!response.ok) throw MissingResourceException(path)
            Int8Array(response.arrayBuffer().await<ArrayBuffer>()).toByteArray()
        } catch (e: CancellationException) {
            throw e
        } catch (e: MissingResourceException) {
            hasFailedRead = true
            throw e
        } catch (
            @Suppress("TooGenericExceptionCaught") e: Throwable,
        ) {
            // A network failure rejects the request with a JavaScript error, which the exception cannot carry.
            BLogger.warning(throwable = e, message = "Failed to load the resource $path")
            hasFailedRead = true
            throw MissingResourceException(path)
        }
}

// A same-origin GET in CORS mode, like the page's `<link rel="preload" as="fetch" crossorigin>`, so the preload
// serves it.
private fun fetchResource(url: String): Promise<Response> = js("fetch(url)")

/**
 * Deletes the Cache Storage copy that Compose's own reader kept, which [WebResourceReader] no longer reads, so
 * returning visitors do not keep it. Failures are ignored: the browser may not offer Cache Storage, and nothing
 * depends on it.
 */
internal fun deleteComposeResourceCache(): Unit =
    js("{ if (self.caches) caches.delete('compose_web_resources_cache').catch(() => {}); }")
