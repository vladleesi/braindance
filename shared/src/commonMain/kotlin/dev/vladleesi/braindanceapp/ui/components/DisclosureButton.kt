// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import ChevronLeft
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import dev.vladleesi.braindanceapp.utils.placeholder

private const val CHEVRON_UP_ROTATION = 90f

internal enum class DisclosureLayout {
    Vertical,
    Inline,
}

/** Reveals existing content. Use [DisclosureLayout.Inline] for items that join a horizontal or wrapping row. */
@Composable
internal fun DisclosureButton(
    label: String,
    expanded: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    layout: DisclosureLayout = DisclosureLayout.Vertical,
    placeholder: Boolean = false,
    style: TextStyle = MaterialTheme.typography.caption,
    color: Color = white,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Row(
        modifier =
            modifier
                .placeholder(placeholder, CornerShapes.pill)
                .clip(CornerShapes.pill)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.pill, Dimens.micro)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = !placeholder,
                    role = Role.Button,
                    onClick = onClick,
                ).pointerHoverIcon(if (placeholder) PointerIcon.Default else PointerIcon.Hand)
                .padding(start = Dimens.tiny, end = Dimens.small, top = Dimens.tiny, bottom = Dimens.tiny),
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val iconModifier =
            Modifier
                .background(if (hovered) secondaryVariant else secondary, CircleShape)
                .padding(Dimens.tiny)
                .size(Dimens.compact)
        when (layout) {
            DisclosureLayout.Vertical ->
                Icon(
                    imageVector = ChevronLeft,
                    contentDescription = null,
                    tint = accent,
                    modifier = iconModifier.rotate(if (expanded) CHEVRON_UP_ROTATION else -CHEVRON_UP_ROTATION),
                )

            DisclosureLayout.Inline ->
                Canvas(iconModifier) {
                    val armLength = Dimens.tiny.toPx()
                    val stroke = Dimens.one.toPx()
                    val horizontal = Offset(armLength, 0f)
                    drawLine(accent, center - horizontal, center + horizontal, stroke, StrokeCap.Round)
                    if (!expanded) {
                        val vertical = Offset(0f, armLength)
                        drawLine(accent, center - vertical, center + vertical, stroke, StrokeCap.Round)
                    }
                }
        }
        Text(
            text = label,
            style = style.copy(fontWeight = FontWeight.SemiBold),
            color = if (hovered) accent else color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
internal fun DisclosureButtonPreview() {
    BraindanceTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
            DisclosureButton("Read more", expanded = false, onClick = {})
            DisclosureButton("Show less", expanded = true, onClick = {})
            DisclosureButton("Show 3 more", expanded = false, onClick = {}, layout = DisclosureLayout.Inline)
            DisclosureButton("Show fewer", expanded = true, onClick = {}, layout = DisclosureLayout.Inline)
        }
    }
}
