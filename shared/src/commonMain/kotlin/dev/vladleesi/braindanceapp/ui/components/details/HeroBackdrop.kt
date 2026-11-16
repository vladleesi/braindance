// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest

// Ink over the screenshot. It is lightest behind the top bar, where the artwork shows most, at least 78% from the
// title down, which keeps white text above 4.5:1 even over a white screenshot, and opaque at the bottom edge, so the
// hero fades into the page with no seam. The stops are fractions of the hero's height, so one brush fits every size.
private const val TOP_SCRIM_ALPHA = 0.55f
private const val TITLE_SCRIM_STOP = 0.4f
private const val TITLE_SCRIM_ALPHA = 0.78f
private val Scrim =
    Brush.verticalGradient(
        0f to background.copy(alpha = TOP_SCRIM_ALPHA),
        TITLE_SCRIM_STOP to background.copy(alpha = TITLE_SCRIM_ALPHA),
        1f to background,
    )

// Without a screenshot, a faint lift from the card surface into the canvas still sets the hero apart.
private val Fallback = Brush.verticalGradient(listOf(surface, background))

/**
 * The hero's background: a game screenshot under an Ink scrim, or a quiet gradient when there is none ([imageUrl] is
 * null). It is decorative and draws behind the hero, so the hero never changes size while the image loads; the image
 * fades in when it arrives. There is no blur: a small screenshot drawn large is already soft, and the scrim is a
 * single cheap gradient draw, so scrolling never re-renders an effect.
 */
@Composable
fun HeroBackdrop(
    imageUrl: String?,
    modifier: Modifier = Modifier,
) {
    if (imageUrl == null) {
        Box(modifier = modifier.background(Fallback))
        return
    }
    AsyncImage(
        model = rememberCachedImageRequest(data = imageUrl, imageCacheKey = null),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier =
            modifier
                .clipToBounds()
                .drawWithContent {
                    drawContent()
                    drawRect(Scrim)
                },
    )
}

@Preview
@Composable
internal fun HeroBackdropPreview() {
    BraindanceTheme {
        HeroBackdrop(imageUrl = null, modifier = Modifier.fillMaxWidth().height(240.dp))
    }
}
