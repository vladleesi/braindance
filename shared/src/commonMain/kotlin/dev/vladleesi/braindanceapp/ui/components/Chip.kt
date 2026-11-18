// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import Check
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import dev.vladleesi.braindanceapp.utils.placeholder

private val ChipIconSize = 14.dp

/**
 * The app's chip: a pill with a short caption, one size everywhere, so rows of chips and their placeholders line up.
 *
 * - Without [onClick], it is information, such as a genre while there is nothing to browse by genre: it looks like
 *   the other chips at full contrast, as it is not an unavailable action, but has no press feedback and screen readers
 *   read it as text.
 * - With [selected], it is one choice among its siblings, such as a tab: the selected one has an accent outline and a
 *   check, and screen readers hear it as selected.
 * - Otherwise it is a button.
 *
 * Like other small controls it relies on Compose's extended touch area instead of a 48 dp layout size, which would
 * space rows of chips far apart. While [placeholder], a shimmer of exactly its size stands in for it.
 */
@Composable
fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean? = null,
    role: Role = if (selected != null) Role.Tab else Role.Button,
    accessibleText: String? = null,
    placeholder: Boolean = false,
) {
    val isSelected = selected == true
    val interactionSource = remember { MutableInteractionSource() }
    val interaction =
        when {
            onClick == null || placeholder -> Modifier
            selected != null ->
                Modifier.selectable(
                    selected = isSelected,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = role,
                    onClick = onClick,
                )
            else ->
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = role,
                    onClick = onClick,
                )
        }
    val pressable = onClick != null && !placeholder && !isSelected
    Row(
        modifier =
            modifier
                .placeholder(visible = placeholder, shape = CornerShapes.pill)
                .clip(CornerShapes.pill)
                .background(if (isSelected) surface else secondaryVariant)
                .then(if (isSelected) Modifier.border(Dimens.one, accent, CornerShapes.pill) else Modifier)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.pill, Dimens.micro)
                .then(interaction)
                .pointerHoverIcon(if (pressable) PointerIcon.Hand else PointerIcon.Default)
                .then(
                    if (accessibleText ==
                        null
                    ) {
                        Modifier
                    } else {
                        Modifier.semantics { contentDescription = accessibleText }
                    },
                ).padding(horizontal = Dimens.compact, vertical = Dimens.small),
        horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (isSelected) {
            Icon(imageVector = Check, contentDescription = null, tint = accent, modifier = Modifier.size(ChipIconSize))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = white,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview
@Composable
internal fun ChipPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.small)) {
                Chip(text = "Adventure")
                Chip(text = "Base game", selected = true, onClick = {})
                Chip(text = "Deluxe Edition", selected = false, onClick = {})
                Chip(text = "+2 more", onClick = {})
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Dimens.small)) {
                Chip(text = "Adventure", placeholder = true)
                Chip(text = "Shooter", placeholder = true)
            }
        }
    }
}
