// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.media

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ZoomStateTest {
    // A 16:9 screenshot on a phone-sized portrait screen: fitted to the width, with bars above and below.
    private fun phoneState() =
        ZoomState().apply {
            containerSize = Size(400f, 800f)
            imageSize = Size(1280f, 720f)
        }

    @Test
    fun theImageFitsInsideTheContainerAtItsOwnShape() {
        assertEquals(Size(400f, 225f), ZoomMath.fittedSize(Size(1280f, 720f), Size(400f, 800f)))
        assertEquals(Size(400f, 800f), ZoomMath.fittedSize(Size.Unspecified, Size(400f, 800f)))
    }

    @Test
    fun fillCoversTheContainerAndCanStillBePannedToEveryEdge() {
        val state = phoneState()
        assertEquals(800f / 225f, state.fillScale, 0.001f)
        assertTrue(state.canFill)

        state.toggleFill()
        assertTrue(state.isFilled)

        // The filled image is 1422 wide in a 400 wide container: it pans 511 either way, no further.
        state.panBy(Offset(10_000f, 10_000f))
        assertEquals(Offset((400f * state.fillScale - 400f) / 2f, 0f), state.offset)

        state.toggleFill()
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset, "the whole image again, centered")
    }

    @Test
    fun fillIsOfferedOnlyWhenItVisiblyChangesTheImage() {
        val nearlySameShape =
            ZoomState().apply {
                containerSize = Size(1600f, 880f)
                imageSize = Size(1920f, 1080f)
            }

        assertFalse(nearlySameShape.canFill, "a 16:9 image on a screen about as wide fills only a few pixels more")
    }

    @Test
    fun theFittedImageNeitherPansNorZoomsOut() {
        val state = phoneState()

        state.panBy(Offset(50f, 50f))
        state.zoomBy(0.5f)

        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
        assertFalse(state.isZoomed)
    }

    @Test
    fun zoomingKeepsThePointUnderTheFingersInPlace() {
        val focus = Offset(100f, 50f)
        val offset = ZoomMath.offsetKeepingFocus(Offset.Zero, from = 1f, to = 2f, focus = focus)

        // The content point under the focus was at focus / 1; at scale 2 it is drawn at 2 * focus + offset.
        assertEquals(focus, focus * 2f + offset)
    }

    @Test
    fun zoomIsLimited() {
        val state = phoneState()

        state.zoomBy(100f)

        assertEquals(maxOf(4f, state.fillScale), state.scale)
        state.zoomBy(Float.NaN)
        state.zoomBy(-1f)
        assertEquals(maxOf(4f, state.fillScale), state.scale, "invalid factors change nothing")
    }

    @Test
    fun doubleTapZoomsInAndThenOutToTheWholeImage() {
        val state = phoneState()

        state.toggleZoom(Offset.Zero)
        assertTrue(state.isZoomed)

        state.toggleZoom(Offset(30f, 30f))
        assertEquals(1f, state.scale)
        assertEquals(Offset.Zero, state.offset)
    }
}
