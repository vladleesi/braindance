// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import Github
import Share
import Star
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white

data class OpenSourceSectionTexts(
    val title: String,
    val description: String,
    val starLabel: String,
    val shareLabel: String,
    val versionLabel: String,
)

@Composable
fun OpenSourceSection(
    texts: OpenSourceSectionTexts,
    onOpenRepository: () -> Unit,
    onShare: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CornerShapes.medium,
        color = MaterialTheme.colors.primary,
    ) {
        Column(
            modifier = Modifier.padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.compact),
        ) {
            OpenSourceHeader(title = texts.title, description = texts.description)

            OpenSourceActions(
                versionLabel = texts.versionLabel,
                starLabel = texts.starLabel,
                shareLabel = texts.shareLabel,
                onOpenRepository = onOpenRepository,
                onShare = onShare,
            )
        }
    }
}

@Composable
private fun OpenSourceHeader(
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.medium),
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.body2,
                color = white,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = description,
                style = MaterialTheme.typography.caption,
                color = secondaryText,
            )
        }
        Icon(imageVector = Github, contentDescription = null, tint = white)
    }
}

@Composable
private fun OpenSourceActions(
    versionLabel: String,
    starLabel: String,
    shareLabel: String,
    onOpenRepository: () -> Unit,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        FlowRow(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedButton(
                onClick = onOpenRepository,
                shape = RoundedCornerShape(percent = 50),
                border = BorderStroke(Dimens.one, white.copy(alpha = 0.2f)),
                contentPadding = PaddingValues(horizontal = Dimens.medium, vertical = Dimens.small),
                colors =
                    ButtonDefaults.outlinedButtonColors(
                        backgroundColor = Color.Transparent,
                        contentColor = white,
                    ),
            ) {
                Icon(
                    imageVector = Star,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = LocalContentColor.current,
                )
                Spacer(Modifier.width(Dimens.small))
                Text(starLabel, color = LocalContentColor.current)
            }

            TextButton(
                onClick = onShare,
                colors = ButtonDefaults.textButtonColors(contentColor = white),
            ) {
                Icon(
                    imageVector = Share,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = LocalContentColor.current,
                )
                Spacer(Modifier.width(Dimens.small))
                Text(shareLabel, color = LocalContentColor.current)
            }
        }

        Text(
            text = versionLabel,
            style = MaterialTheme.typography.subtitle2,
            color = secondaryText,
        )
    }
}

@Preview
@Composable
internal fun OpenSourceSectionPreview() {
    BraindanceTheme {
        OpenSourceSection(
            texts =
                OpenSourceSectionTexts(
                    title = "Braindance is open source",
                    description = "Like the project? Support it on GitHub.",
                    starLabel = "Star on GitHub",
                    shareLabel = "Share",
                    versionLabel = "v1.0.0",
                ),
            onOpenRepository = {},
            onShare = {},
        )
    }
}
