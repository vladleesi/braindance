// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import coil3.PlatformContext
import coil3.asImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.size.Dimension
import coil3.size.Scale
import coil3.size.Size
import kotlinx.coroutines.test.runTest
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.Image
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

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

    @Test
    fun aSmallDisplayLoadsTheSmallerCoverUnderItsOwnCacheKeys() =
        runTest {
            val request = croppedCoverRequest()

            val loaded = SmallerCoverInterceptor().intercept(RecordingChain(request, Size(153, 204))).request

            assertEquals("https://images.igdb.com/igdb/image/upload/t_cover_small_2x/co1.webp", loaded.data)
            assertEquals("$CACHE_KEY-t_cover_small_2x", loaded.memoryCacheKey)
            assertEquals("$CACHE_KEY-t_cover_small_2x", loaded.diskCacheKey)
        }

    @Test
    fun aSmallerCoverThatDoesNotFillTheDisplayIsReplacedByTheLargeOne() =
        runTest {
            val request = croppedCoverRequest()
            // A square cover: 264x264 at the smaller size, which is shorter than the display.
            val chain = RecordingChain(request, Size(250, 333), loadedWidthPx = 250, loadedHeightPx = 264)

            assertSame(request, SmallerCoverInterceptor().intercept(chain).request)
        }

    @Test
    fun aLargeDisplayAFittedImageOrAnOriginalSizeRequestLoadsTheCoverUnchanged() =
        runTest {
            val cropped = croppedCoverRequest()
            val fitted = cachedImageRequest(PlatformContext.INSTANCE, IMAGE_URL, CACHE_KEY, keepOriginalSize = false)

            val largeDisplay = SmallerCoverInterceptor().intercept(RecordingChain(cropped, Size(432, 576))).request
            val fittedImage = SmallerCoverInterceptor().intercept(RecordingChain(fitted, Size(153, 204))).request
            val originalSize = SmallerCoverInterceptor().intercept(RecordingChain(cropped, Size.ORIGINAL)).request

            assertSame(cropped, largeDisplay)
            assertSame(fitted, fittedImage)
            assertSame(cropped, originalSize)
        }

    // Covers are requested as WebP, which this platform decodes with Skia.
    @Test
    fun skiaDecodesWebp() {
        val image = Image.makeFromEncoded(TINY_WEBP.hexToByteArray())
        assertEquals(TINY_WEBP_SIDE_PX, image.width)
        assertEquals(TINY_WEBP_SIDE_PX, image.height)
    }

    // AsyncImage requests a cropped image with the fill scale.
    private fun croppedCoverRequest(): ImageRequest =
        cachedImageRequest(PlatformContext.INSTANCE, IMAGE_URL, CACHE_KEY, keepOriginalSize = false)
            .newBuilder()
            .scale(Scale.FILL)
            .build()

    /**
     * Ends the chain with an image for the request the interceptor passed on. The image has the display's size, as a
     * file with enough pixels decodes to, unless [loadedWidthPx] and [loadedHeightPx] give the size of a smaller file.
     */
    private class RecordingChain(
        override val request: ImageRequest,
        override val size: Size,
        private val loadedWidthPx: Int? = null,
        private val loadedHeightPx: Int? = null,
    ) : Interceptor.Chain {
        override fun withRequest(request: ImageRequest): Interceptor.Chain =
            RecordingChain(request, size, loadedWidthPx, loadedHeightPx)

        override fun withSize(size: Size): Interceptor.Chain =
            RecordingChain(request, size, loadedWidthPx, loadedHeightPx)

        override suspend fun proceed(): ImageResult {
            val widthPx = loadedWidthPx ?: (size.width as? Dimension.Pixels)?.px ?: ORIGINAL_SIDE_PX
            val heightPx = loadedHeightPx ?: (size.height as? Dimension.Pixels)?.px ?: ORIGINAL_SIDE_PX
            val bitmap = Bitmap().apply { allocN32Pixels(widthPx, heightPx) }
            return SuccessResult(image = bitmap.asImage(), request = request, dataSource = DataSource.NETWORK)
        }
    }

    private companion object {
        const val IMAGE_URL = "https://images.igdb.com/igdb/image/upload/t_720p/co1.webp"
        const val CACHE_KEY = "mgc-1"

        // A 4x4 grey lossy WebP made with `cwebp -q 50`.
        const val TINY_WEBP =
            "524946462c0000005745425056503820200000003001009d012a0400040002c04c25a400037000fee9492e2082ae779acb100000"
        const val TINY_WEBP_SIDE_PX = 4
        const val ORIGINAL_SIDE_PX = 8
    }
}
