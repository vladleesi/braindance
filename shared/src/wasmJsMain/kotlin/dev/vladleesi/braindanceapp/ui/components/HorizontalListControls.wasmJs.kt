// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import ChevronLeft
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.horizontal_list_next
import dev.vladleesi.braindanceapp.resources.horizontal_list_previous
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.utils.withCircleRippleEffect
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val ITEMS_PER_SCROLL = 3
private const val SCROLL_ANIMATION_DURATION_MILLIS = 450
private const val REVERSE_ICON_ROTATION_DEGREES = 180f

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

@Composable
private fun HorizontalListButton(
    contentDescription: String,
    enabled: Boolean,
    onClick: () -> Unit,
    rotateIcon: Boolean = false,
) {
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(48.dp),
    ) {
        Icon(
            imageVector = ChevronLeft,
            contentDescription = contentDescription,
            modifier =
                Modifier
                    .size(28.dp)
                    .withCircleRippleEffect(MaterialTheme.colors.secondary)
                    .padding(Dimens.tiny)
                    .then(if (rotateIcon) Modifier.rotate(REVERSE_ICON_ROTATION_DEGREES) else Modifier),
        )
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
