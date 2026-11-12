// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.void

private val DefaultAvatarSize = 32.dp
private val AvatarBackground = Brush.linearGradient(listOf(neonCyan, hotMagenta))

// The figure is drawn as fractions of the avatar's width, so it scales with any size.
private const val HEAD_CENTER_Y = 0.40f
private const val HEAD_RADIUS = 0.17f
private const val SHOULDERS_LEFT = 0.18f
private const val SHOULDERS_TOP = 0.64f
private const val SHOULDERS_WIDTH = 0.64f
private const val SHOULDERS_HEIGHT = 0.60f
private const val CENTER = 0.5f

/**
 * A placeholder profile picture: an abstract person, a round head above rounded shoulders, in the brand colors on a
 * round gradient. It stands in until accounts exist, and is the single place to swap in the signed-in user's picture.
 * Pass [contentDescription] when the avatar is the only label of a button; leave it null when text names it already.
 */
@Composable
fun UserAvatar(
    modifier: Modifier = Modifier,
    size: Dp = DefaultAvatarSize,
    contentDescription: String? = null,
) {
    Canvas(
        modifier =
            modifier
                .size(size)
                .clip(CircleShape)
                .semantics { if (contentDescription != null) this.contentDescription = contentDescription },
    ) {
        drawRect(brush = AvatarBackground)
        val width = this.size.width
        drawCircle(color = void, radius = width * HEAD_RADIUS, center = Offset(width * CENTER, width * HEAD_CENTER_Y))
        // The shoulders are an oval wider than the avatar's lower half; the circular clip cuts them to shape.
        drawOval(
            color = void,
            topLeft = Offset(width * SHOULDERS_LEFT, width * SHOULDERS_TOP),
            size = Size(width * SHOULDERS_WIDTH, width * SHOULDERS_HEIGHT),
        )
    }
}

@Preview
@Composable
internal fun UserAvatarPreview() {
    BraindanceTheme {
        UserAvatar(size = 96.dp, modifier = Modifier.background(void).padding(Dimens.medium))
    }
}
