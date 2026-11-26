// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import Github
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.externalLink
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible

private val RowMinHeight = 56.dp
private val RowIconSize = 24.dp
private val ExternalMarkSize = 18.dp

/** Where a [LinkRow]'s text starts, so a [LinkDivider] under it can line up with the text. */
val LinkRowTextInset: Dp = Dimens.medium + RowIconSize + Dimens.medium

/** A hairline between two [LinkRow]s on a card, starting [inset] from the start edge. */
@Composable
fun LinkDivider(inset: Dp = Dimens.medium) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = inset)
                .height(Dimens.one)
                .background(secondary),
    )
}

/**
 * A row on a card that opens a page or runs an action, the settings-list pattern: a leading icon, the [title],
 * optional [supportingText] under it, and, when [opensExternally], a mark that the page opens outside the app. A row
 * without an icon keeps its place, so the titles of a list line up, unless [reserveIconSpace] is false for a list with
 * no icons at all. The whole row is the touch target, at least 56 dp
 * tall; keyboard focus draws an outline and pointers show a hand cursor. A row that is not [enabled] does nothing.
 */
@Composable
fun LinkRow(
    title: String,
    imageVector: ImageVector?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    opensExternally: Boolean = true,
    enabled: Boolean = true,
    reserveIconSpace: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .focusRing(interactionSource.isFocusVisible(), RectangleShape)
                .then(if (enabled) Modifier.pointerHoverIcon(PointerIcon.Hand) else Modifier)
                .then(if (opensExternally && enabled) Modifier.externalLink(onClick) else Modifier)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = Dimens.medium, vertical = Dimens.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.medium),
    ) {
        if (imageVector != null) {
            Icon(
                imageVector = imageVector,
                contentDescription = null,
                tint = secondaryText,
                modifier = Modifier.size(RowIconSize),
            )
        } else if (reserveIconSpace) {
            Spacer(modifier = Modifier.size(RowIconSize))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.subtitle1, color = white)
            if (supportingText != null) {
                Text(text = supportingText, style = MaterialTheme.typography.caption, color = secondaryText)
            }
        }
        if (opensExternally) {
            Icon(
                imageVector = OpenInNew,
                contentDescription = null,
                tint = secondaryText,
                modifier = Modifier.size(ExternalMarkSize),
            )
        }
    }
}

@Preview
@Composable
internal fun LinkRowPreview() {
    BraindanceTheme {
        SurfaceCard(modifier = Modifier.padding(Dimens.medium)) {
            Column {
                LinkRow(title = "Star on GitHub", supportingText = "Open source", imageVector = Github, onClick = {})
                LinkDivider(inset = LinkRowTextInset)
                LinkRow(title = "No icon", imageVector = null, onClick = {}, opensExternally = false)
            }
        }
    }
}
