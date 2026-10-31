// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import ChevronLeft
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.horizontal_list_next
import dev.vladleesi.braindanceapp.resources.horizontal_list_previous
import dev.vladleesi.braindanceapp.ui.style.Dimens
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val ITEMS_PER_SCROLL = 3
private const val SCROLL_ANIMATION_DURATION_MILLIS = 450
private const val REVERSE_ICON_ROTATION_DEGREES = 180f

private val ButtonTouchTargetSize = 48.dp
private val ButtonCircleSize = 28.dp

@Composable
actual fun HorizontalListControls(
    listState: LazyListState,
    modifier: Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    Row(modifier = modifier) {
        HorizontalListButton(
            contentDescription = stringResource(Res.string.horizontal_list_previous),
            enabled = listState.canScrollBackward,
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollBy(
                        value = listState.scrollDistance(forward = false),
                        animationSpec =
                            tween(
                                durationMillis = SCROLL_ANIMATION_DURATION_MILLIS,
                                easing = FastOutSlowInEasing,
                            ),
                    )
                }
            },
        )
        HorizontalListButton(
            contentDescription = stringResource(Res.string.horizontal_list_next),
            enabled = listState.canScrollForward,
            onClick = {
                coroutineScope.launch {
                    listState.animateScrollBy(
                        value = listState.scrollDistance(forward = true),
                        animationSpec =
                            tween(
                                durationMillis = SCROLL_ANIMATION_DURATION_MILLIS,
                                easing = FastOutSlowInEasing,
                            ),
                    )
                }
            },
            rotateIcon = true,
        )
    }
}

/**
 * Keeps a full-size touch target while drawing hover, focus, and press feedback only inside the visible circle,
 * so the ripple neither overflows into the neighboring button nor gets cut by it.
 */
@Composable
private fun HorizontalListButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    rotateIcon: Boolean = false,
) {
    Box(
        modifier =
            Modifier
                .size(ButtonTouchTargetSize)
                .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
                .clickable(
                    enabled = enabled,
                    role = Role.Button,
                    interactionSource = null,
                    indication = ripple(bounded = false, radius = ButtonCircleSize / 2),
                    onClick = onClick,
                ),
        contentAlignment = Alignment.Center,
    ) {
        val contentAlpha = if (enabled) LocalContentAlpha.current else ContentAlpha.disabled
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            Icon(
                imageVector = ChevronLeft,
                contentDescription = contentDescription,
                modifier =
                    Modifier
                        .size(ButtonCircleSize)
                        .background(color = MaterialTheme.colors.secondary, shape = CircleShape)
                        .padding(Dimens.tiny)
                        .then(if (rotateIcon) Modifier.rotate(REVERSE_ICON_ROTATION_DEGREES) else Modifier),
            )
        }
    }
}

private fun LazyListState.scrollDistance(forward: Boolean): Float {
    val visibleItems = layoutInfo.visibleItemsInfo
    val itemStride =
        if (visibleItems.size > 1) {
            visibleItems[1].offset - visibleItems[0].offset
        } else {
            visibleItems.firstOrNull()?.size ?: 0
        }
    val direction = if (forward) 1 else -1

    return (direction * itemStride * ITEMS_PER_SCROLL).toFloat()
}
