// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import Bell
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.coming_soon
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.disabledText
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import org.jetbrains.compose.resources.stringResource

/*
 * Placeholders for planned features. They are visibly marked or disabled, never clickable without an effect, and
 * hold no state, so they can be replaced one by one as the features arrive.
 */

/** The width a [ComingSoonCard] keeps on its own, which suits its couple of lines. */
val ComingSoonCardMaxWidth = 560.dp

/** The "Coming soon" [Tag]. */
@Composable
fun ComingSoonLabel(modifier: Modifier = Modifier) {
    Tag(text = stringResource(Res.string.coming_soon), modifier = modifier)
}

/**
 * A preview of a planned feature: its [title] and [description] under a [ComingSoonLabel]. [action], when given,
 * sits at the end, for example a link to the destination that will host the feature.
 *
 * On its own the card keeps a [maxWidth] that suits its couple of lines. Where it takes the place of content that
 * fills the page, such as a feed in the desktop layout, or of a cell in a grid, pass [Dp.Unspecified] so it fills its
 * space like the content it stands for.
 */
@Composable
fun ComingSoonCard(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
    maxWidth: Dp = ComingSoonCardMaxWidth,
    action: (@Composable () -> Unit)? = null,
) {
    SurfaceCard(modifier = modifier.widthIn(max = maxWidth).fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(Dimens.medium),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
            ) {
                ComingSoonLabel()
                Text(
                    text = title,
                    modifier = Modifier.semantics { heading() },
                    style = MaterialTheme.typography.subtitle1,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.caption,
                    color = secondaryText,
                )
            }
            action?.invoke()
        }
    }
}

/** A disabled icon button for a planned feature; [label] names it for accessibility and says it is coming soon. */
@Composable
fun ComingSoonAction(
    imageVector: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = {}, modifier = modifier, enabled = false) {
        Icon(
            imageVector = imageVector,
            contentDescription = label,
            tint = disabledText,
        )
    }
}

/** Non-interactive chips showing the filters or categories a planned feature will offer. */
@Composable
fun PlaceholderChips(
    labels: List<String>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        labels.forEach { label ->
            Text(
                text = label,
                modifier =
                    Modifier
                        .background(secondary, CircleShape)
                        .padding(horizontal = Dimens.compact, vertical = Dimens.small),
                style = MaterialTheme.typography.subtitle2,
                color = secondaryText,
            )
        }
    }
}

@Preview
@Composable
internal fun ComingSoonPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            ComingSoonAction(imageVector = Bell, label = "Notifications")
            PlaceholderChips(labels = listOf("Platform", "Genre", "Release date"))
            ComingSoonCard(title = "Find your next game", description = "Recommendations based on what you play.")
        }
    }
}
