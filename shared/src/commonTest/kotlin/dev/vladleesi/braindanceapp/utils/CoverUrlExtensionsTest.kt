// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CoverUrlExtensionsTest {
    @Test
    fun coverAddressUsesTheRequestedSizeOverHttpsAsWebp() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_720p/co1.webp",
            "//images.igdb.com/igdb/image/upload/t_thumb/co1.jpg".toCoverUrl(CoverSize.P_720),
        )
    }

    @Test
    fun anAddressThatIsNotJpegKeepsItsFormat() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_big/co1.png",
            "//images.igdb.com/igdb/image/upload/t_thumb/co1.png".toCoverUrl(CoverSize.COVER_BIG),
        )
    }

    @Test
    fun aDisplayGetsTheSmallestCoverSizeThatFillsIt() {
        // Desktop cards on a standard-density screen, then the largest display each size fills.
        assertEquals(CoverSize.COVER_SMALL_2X, smallerCoverSizeOrNull(displayWidthPx = 153, displayHeightPx = 204))
        assertEquals(CoverSize.COVER_SMALL_2X, smallerCoverSizeOrNull(displayWidthPx = 180, displayHeightPx = 240))
        assertEquals(CoverSize.COVER_BIG, smallerCoverSizeOrNull(displayWidthPx = 181, displayHeightPx = 240))
        assertEquals(CoverSize.COVER_BIG, smallerCoverSizeOrNull(displayWidthPx = 180, displayHeightPx = 241))
        assertEquals(CoverSize.COVER_BIG, smallerCoverSizeOrNull(displayWidthPx = 264, displayHeightPx = 352))
    }

    @Test
    fun aLargerDisplayKeepsTheLargeCover() {
        // A phone card at three pixels per dp, then each limit exceeded by one pixel.
        assertNull(smallerCoverSizeOrNull(displayWidthPx = 432, displayHeightPx = 576))
        assertNull(smallerCoverSizeOrNull(displayWidthPx = 265, displayHeightPx = 352))
        assertNull(smallerCoverSizeOrNull(displayWidthPx = 264, displayHeightPx = 353))
    }

    @Test
    fun onlyALargeCoverAddressChangesSize() {
        assertEquals(
            "https://images.igdb.com/igdb/image/upload/t_cover_small_2x/co1.webp",
            "https://images.igdb.com/igdb/image/upload/t_720p/co1.webp".largeCoverUrlAtOrNull(CoverSize.COVER_SMALL_2X),
        )
        assertNull("https://api.example.com/v1/giveaways/1/image".largeCoverUrlAtOrNull(CoverSize.COVER_BIG))
        assertNull(
            "https://images.igdb.com/igdb/image/upload/t_thumb/co1.webp".largeCoverUrlAtOrNull(CoverSize.COVER_BIG),
        )
    }
}
