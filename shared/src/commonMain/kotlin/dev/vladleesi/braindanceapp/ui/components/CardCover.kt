// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import dev.vladleesi.braindanceapp.utils.shimmerEffect
import dev.vladleesi.braindanceapp.utils.toContentDescription

/*
 * Building blocks shared by the game and giveaway cards on every platform. The caller sizes a card, for example a
 * fixed width in a mobile carousel or a weighted cell in a desktop row; the artwork keeps its aspect ratio inside it.
 */

/** IGDB covers are portrait 3:4; game cards keep that ratio so covers are never stretched. */
const val COVER_ASPECT_RATIO = 3f / 4f

private const val HOVER_IMAGE_SCALE = 1.04f
private const val HOVER_ANIMATION_MILLIS = 150
private const val MISSING_COVER_TITLE_LINES = 4

/**
 * Card artwork at a fixed [aspectRatio], decoded at its displayed size and memory-cached under [memoryCacheKey]. It
 * shows the card's [interactionSource] state: the theme ripple for press, a slight zoom on mouse hover, and the focus
 * ring for keyboard focus. [overlay] draws over the image.
 *
 * Some games have no cover, and an image can fail to load; the tile then shows [contentDescription], the card's
 * title, so it never looks as if it were still loading.
 */
@Composable
fun CardCover(
    imageUrl: String,
    memoryCacheKey: String,
    contentDescription: String,
    aspectRatio: Float,
    interactionSource: MutableInteractionSource,
    modifier: Modifier = Modifier,
    overlay: @Composable BoxScope.() -> Unit = {},
) {
    val hovered by interactionSource.collectIsHoveredAsState()
    var failed by remember(imageUrl) { mutableStateOf(false) }
    // Idle unless a mouse hovers the card, so touch scrolling never animates it.
    val imageScale by animateFloatAsState(
        targetValue = if (hovered) HOVER_IMAGE_SCALE else 1f,
        animationSpec = tween(HOVER_ANIMATION_MILLIS),
    )
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(CornerShapes.small)
                .background(secondaryVariant)
                .indication(interactionSource, LocalIndication.current)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small, Dimens.micro),
    ) {
        if (imageUrl.isBlank() || failed) {
            MissingCoverTitle(title = contentDescription)
        } else {
            AsyncImage(
                model = rememberCachedImageRequest(data = imageUrl, memoryCacheKey = memoryCacheKey),
                contentDescription = contentDescription.toContentDescription(),
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = imageScale
                            scaleY = imageScale
                        },
                onError = { failed = true },
            )
        }
        overlay()
    }
}

/** The title in place of missing artwork: quiet, centered, and cut short when the tile is small. */
@Composable
private fun BoxScope.MissingCoverTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.caption,
        color = secondaryText,
        textAlign = TextAlign.Center,
        maxLines = MISSING_COVER_TITLE_LINES,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.align(Alignment.Center).padding(Dimens.small),
    )
}

/**
 * Makes a whole card one click target, so its title opens it too. The [CardCover] that shares [interactionSource]
 * shows the press, hover, and focus feedback.
 */
fun Modifier.cardClickable(
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
): Modifier =
    pointerHoverIcon(PointerIcon.Hand)
        .clickable(interactionSource = interactionSource, indication = null, role = Role.Button, onClick = onClick)

/** A loading placeholder with the geometry of a [CardCover]. */
@Composable
fun CardCoverSkeleton(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(aspectRatio)
                .clip(CornerShapes.small)
                .shimmerEffect(),
    )
}

/** A loading placeholder for one line of [style] text, [widthFraction] of the card wide. */
@Composable
fun TextLineSkeleton(
    style: TextStyle,
    widthFraction: Float,
    modifier: Modifier = Modifier,
) {
    Text(
        text = "",
        style = style,
        modifier =
            modifier
                .fillMaxWidth(widthFraction)
                .clip(CornerShapes.small)
                .shimmerEffect(),
    )
}
