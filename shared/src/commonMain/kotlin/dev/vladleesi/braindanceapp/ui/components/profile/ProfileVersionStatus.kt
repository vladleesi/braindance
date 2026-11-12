// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.statusGreen

private val StatusDotSize = 8.dp

// A tint of the badge color keeps its text readable on the dark background.
private const val BADGE_BACKGROUND_ALPHA = 0.16f

/**
 * The app version with a green dot at the start, and a pink badge for the development stage ([statusLabel]) at the
 * end. The badge wraps to its own line when the text is enlarged or translated at length. The dot is decoration.
 */
@Composable
fun ProfileVersionStatus(
    versionLabel: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
        itemVerticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(StatusDotSize)
                        .background(statusGreen, CircleShape)
                        .clearAndSetSemantics {},
            )
            Text(text = versionLabel, style = MaterialTheme.typography.caption, color = secondaryText)
        }
        Text(
            text = statusLabel,
            style = MaterialTheme.typography.caption,
            color = hotMagenta,
            maxLines = 1,
            modifier =
                Modifier
                    .background(hotMagenta.copy(alpha = BADGE_BACKGROUND_ALPHA), CircleShape)
                    .padding(horizontal = Dimens.small, vertical = Dimens.micro),
        )
    }
}

@Preview
@Composable
internal fun ProfileVersionStatusPreview() {
    BraindanceTheme {
        ProfileVersionStatus(
            versionLabel = "v1.0.0",
            statusLabel = "In development",
            modifier = Modifier.padding(Dimens.medium),
        )
    }
}
