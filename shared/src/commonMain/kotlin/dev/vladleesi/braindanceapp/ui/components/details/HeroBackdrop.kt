// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.system.prefersReducedMotion
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest

// The canvas color over the image. It is lightest behind the top bar, where the artwork shows most, at least 78% from
// the title down, which keeps the foreground text above 4.5:1 even over a screenshot in the opposite tone, and opaque
// at the bottom edge, so the hero fades into the page with no seam. The stops are fractions of the hero's height, so
// one brush fits every size.
private const val TOP_SCRIM_ALPHA = 0.55f
private const val TITLE_SCRIM_STOP = 0.4f
private const val TITLE_SCRIM_ALPHA = 0.78f

private fun scrim(canvas: Color): Brush =
    Brush.verticalGradient(
        0f to canvas.copy(alpha = TOP_SCRIM_ALPHA),
        TITLE_SCRIM_STOP to canvas.copy(alpha = TITLE_SCRIM_ALPHA),
        1f to canvas,
    )

// Where the image is cut when it is cropped. Wide windows crop its top and bottom: a center crop cuts the top, where
// key art and screenshots usually put faces, and keeps a bottom the scrim hides anyway. Anchoring a quarter of the way
// down keeps heads in view without showing the image's very top, often sky or a game's interface, behind the top bar.
// Phones crop the sides instead, so the anchor does not change them.
private val BackdropAlignment = BiasAlignment(horizontalBias = 0f, verticalBias = -0.5f)

// How far the image follows the page as it scrolls: it moves at 60% of the page's speed, so it reads as further away.
// The scrim stays with the hero, so the hero still fades into the page with no seam.
private const val PARALLAX_FACTOR = 0.4f

// Without an image, a faint lift from the card surface into the canvas still sets the hero apart.

/**
 * The hero's background: the image the API chose, key art or a screenshot, under an Ink scrim, or a quiet gradient
 * when there is none ([imageUrl] is null). A crop keeps its upper part, where faces usually are. It is decorative and
 * draws behind the hero, so the hero never changes size while the image loads; the image fades in when it arrives.
 * There is no blur: a small image drawn large is already soft, and the scrim is a single cheap gradient draw, so
 * scrolling never re-renders an effect.
 *
 * [scrolledPx] is how far the page has scrolled past the hero's top. The image moves with a parallax: slower than the
 * page, read only while drawing, so scrolling does not recompose. It stays still when the system asks for less motion.
 */
@Composable
fun HeroBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    scrolledPx: () -> Int = { 0 },
) {
    val canvas = background
    if (imageUrl == null) {
        val cardSurface = surface
        val fallback = remember(cardSurface, canvas) { Brush.verticalGradient(listOf(cardSurface, canvas)) }
        Box(modifier = modifier.background(fallback))
        return
    }
    val scrim = remember(canvas) { scrim(canvas) }
    val parallax = if (prefersReducedMotion()) 0f else PARALLAX_FACTOR
    Box(
        modifier =
            modifier
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    drawRect(scrim)
                },
    ) {
        AsyncImage(
            model = rememberCachedImageRequest(data = imageUrl, memoryCacheKey = null),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BackdropAlignment,
            // Shifted while drawing, without a layer of its own, so it also follows the hero's size at once.
            modifier =
                Modifier.fillMaxSize().drawWithContent {
                    translate(top = scrolledPx() * parallax) { this@drawWithContent.drawContent() }
                },
        )
    }
}

@Preview
@Composable
internal fun HeroBackdropPreview() {
    BraindanceTheme {
        HeroBackdrop(imageUrl = null, modifier = Modifier.fillMaxWidth().height(240.dp))
    }
}
