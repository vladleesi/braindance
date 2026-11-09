// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import coil3.PlatformContext
import coil3.size.Size
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

// Runs on iOS because Android host tests have no Coil PlatformContext without Robolectric.
class CachedImageRequestTest {
    @Test
    fun iosKeepsTheDownloadedSizeBecauseItsDownscalingIsUnfiltered() =
        runTest {
            val request = cachedImageRequest(PlatformContext.INSTANCE, IMAGE_URL, CACHE_KEY)

            assertEquals(Size.ORIGINAL, request.sizeResolver.size())
        }

    @Test
    fun smoothlyScalingPlatformsLeaveTheSizeToTheLayout() {
        val request = cachedImageRequest(PlatformContext.INSTANCE, IMAGE_URL, CACHE_KEY, keepOriginalSize = false)

        // AsyncImage supplies its constraints only when the request defines no size.
        assertNull(request.defined.sizeResolver)
    }

    @Test
    fun bothCachesUseTheGivenKey() {
        val request = cachedImageRequest(PlatformContext.INSTANCE, IMAGE_URL, CACHE_KEY)

        assertEquals(CACHE_KEY, request.memoryCacheKey)
        assertEquals(CACHE_KEY, request.diskCacheKey)
    }

    private companion object {
        const val IMAGE_URL = "https://images.igdb.com/igdb/image/upload/t_720p/co1.jpg"
        const val CACHE_KEY = "mgc-1"
    }
}
