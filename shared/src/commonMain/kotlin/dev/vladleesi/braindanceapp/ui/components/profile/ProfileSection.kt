// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.profile

import Github
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
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

/** Where a row's text starts, so a divider under a row with an icon can line up with its text. */
val ProfileRowTextInset: Dp = Dimens.medium + RowIconSize + Dimens.medium

/**
 * A titled group of related rows on one rounded surface, the settings-list pattern: a short, specific [title] above
 * the group names what the rows have in common. Separate the rows with [ProfileDivider].
 */
@Composable
fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            color = secondaryText,
            modifier = Modifier.padding(horizontal = Dimens.medium).semantics { heading() },
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CornerShapes.medium,
            color = MaterialTheme.colors.primary,
        ) {
            Column(content = content)
        }
    }
}

/** A hairline between two rows of a [ProfileSection], starting [inset] from the start edge. */
@Composable
fun ProfileDivider(inset: Dp = Dimens.medium) {
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
 * A row that opens a page or runs an action: a leading icon, the [title], optional [supportingText] under it, and,
 * when [opensExternally], a mark that the page opens outside the app. The whole row is the touch target, at least
 * 56 dp tall. Keyboard focus draws an outline and pointers show a hand cursor.
 */
@Composable
fun ProfileLinkRow(
    title: String,
    imageVector: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    opensExternally: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = RowMinHeight)
                .focusRing(interactionSource.isFocusVisible(), RectangleShape)
                .pointerHoverIcon(PointerIcon.Hand)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = Dimens.medium, vertical = Dimens.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.medium),
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            tint = secondaryText,
            modifier = Modifier.size(RowIconSize),
        )
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
internal fun ProfileSectionPreview() {
    BraindanceTheme {
        ProfileSection(title = "Community", modifier = Modifier.padding(Dimens.medium)) {
            ProfileLinkRow(
                title = "Star on GitHub",
                supportingText = "Braindance is open source",
                imageVector = Github,
                onClick = {},
            )
            ProfileDivider(inset = ProfileRowTextInset)
            ProfileLinkRow(title = "Share", imageVector = Github, onClick = {}, opensExternally = false)
        }
    }
}
