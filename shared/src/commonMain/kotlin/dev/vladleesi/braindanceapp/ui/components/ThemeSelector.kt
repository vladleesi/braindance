// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.theme_dark
import dev.vladleesi.braindanceapp.resources.theme_light
import dev.vladleesi.braindanceapp.resources.theme_system
import dev.vladleesi.braindanceapp.system.ThemeMode
import dev.vladleesi.braindanceapp.system.prefersReducedMotion
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.icons.SidebarMonitor
import dev.vladleesi.braindanceapp.ui.style.icons.SidebarMoon
import dev.vladleesi.braindanceapp.ui.style.icons.SidebarSun
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val CompactSegmentHeight = 32.dp
private val LabeledSegmentHeight = 48.dp
internal const val THEME_SELECTION_MILLIS = 200

// The selected segment sits inside the container's padding, so its corners are the container's radius less that
// padding. Using the same radius would leave a wider gap at the corners than along the sides.
private val SegmentShape = RoundedCornerShape(Dimens.medium - Dimens.tiny)

// Settles without bouncing past the container's edge, so the pill glides and slows into place.
private const val SELECTION_DAMPING = 0.9f

internal val ThemeMode.label: StringResource
    get() =
        when (this) {
            ThemeMode.Light -> Res.string.theme_light
            ThemeMode.System -> Res.string.theme_system
            ThemeMode.Dark -> Res.string.theme_dark
        }

internal val ThemeMode.icon: ImageVector
    get() =
        when (this) {
            ThemeMode.Light -> SidebarSun
            ThemeMode.System -> SidebarMonitor
            ThemeMode.Dark -> SidebarMoon
        }

@Composable
internal fun ThemeSelector(
    selectedMode: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier,
    showLabels: Boolean = false,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .background(if (MaterialTheme.colors.isLight) secondary else surface, CornerShapes.large)
                .border(Dimens.one, secondaryVariant, CornerShapes.large)
                .padding(Dimens.tiny),
    ) {
        val segmentWidth = maxWidth / ThemeMode.entries.size
        val selectedOffset by animateDpAsState(
            targetValue = segmentWidth * selectedMode.ordinal,
            animationSpec =
                if (prefersReducedMotion()) {
                    snap()
                } else {
                    spring(dampingRatio = SELECTION_DAMPING, stiffness = Spring.StiffnessMediumLow)
                },
            label = "Theme selection",
        )
        Box(Modifier.matchParentSize()) {
            Box(
                Modifier
                    .offset { IntOffset(selectedOffset.roundToPx(), 0) }
                    .width(segmentWidth)
                    .fillMaxHeight()
                    .background(if (MaterialTheme.colors.isLight) secondaryText else secondaryVariant, SegmentShape),
            )
        }
        Row(Modifier.fillMaxWidth().selectableGroup()) {
            ThemeMode.entries.forEach { mode ->
                ThemeSegment(
                    mode = mode,
                    selected = mode == selectedMode,
                    onSelect = { onSelect(mode) },
                    showLabel = showLabels,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun ThemeSegment(
    mode: ThemeMode,
    selected: Boolean,
    onSelect: () -> Unit,
    showLabel: Boolean,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val label = stringResource(mode.label)
    // The light theme's selection is a slate pill, the color of the app's other icons, so its icon and label are
    // the light surface's color.
    val selectedColor = if (MaterialTheme.colors.isLight) surface else foreground
    val color by animateColorAsState(
        targetValue = if (selected) selectedColor else secondaryText,
        animationSpec = if (prefersReducedMotion()) snap() else tween(THEME_SELECTION_MILLIS),
        label = "Theme segment color",
    )
    Column(
        modifier =
            modifier
                .heightIn(min = if (showLabel) LabeledSegmentHeight else CompactSegmentHeight)
                .clip(SegmentShape)
                .focusRing(interactionSource.isFocusVisible(), SegmentShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.RadioButton,
                    onClick = onSelect,
                ).padding(vertical = Dimens.tiny),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = mode.icon,
            contentDescription = if (showLabel) null else label,
            tint = color,
            modifier = Modifier.size(Dimens.medium),
        )
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.caption,
                color = color,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
internal fun ThemeSelectorPreview() {
    BraindanceTheme {
        ThemeSelector(ThemeMode.System, {}, Modifier.width(112.dp))
    }
}

@Preview
@Composable
internal fun ThemeSegmentPreview() {
    BraindanceTheme(darkTheme = false) {
        ThemeSegment(ThemeMode.Light, selected = true, onSelect = {}, showLabel = true, Modifier.width(88.dp))
    }
}
