// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.white

private const val SEPARATOR = "·"
private const val ATTRIBUTION_TAG = "data-attribution"

private val AttributionLinkStyles =
    TextLinkStyles(
        style = SpanStyle(textDecoration = TextDecoration.Underline),
        focusedStyle = SpanStyle(color = white, background = secondary),
        hoveredStyle = SpanStyle(color = white),
        pressedStyle = SpanStyle(color = white),
    )

/**
 * A data source credited to its provider: [label] reads like "Game data by IGDB". Only [linkText], the provider name
 * inside [label], is the link; [onOpen] opens the provider.
 */
@Immutable
class DataAttribution(
    val label: String,
    val linkText: String,
    val onOpen: () -> Unit,
)

/**
 * The app version and the data attributions as quiet caption text, such as
 * "v1.0.0 · Game data by IGDB · Giveaways by GamerPower". The items wrap between entries, so a narrow sidebar or a
 * longer translation never splits a link. In each attribution only the provider name is a text link, so screen readers
 * announce it as a link, the keyboard can focus it, and pointers show a hand cursor over it;
 * [DataAttribution.onOpen] opens the provider.
 */
@Composable
fun VersionAttribution(
    versionLabel: String,
    attributions: List<DataAttribution>,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
        verticalArrangement = Arrangement.spacedBy(Dimens.micro),
    ) {
        AttributionItem(showSeparator = attributions.isNotEmpty()) {
            Text(text = versionLabel, style = MaterialTheme.typography.caption, color = secondaryText)
        }
        attributions.forEachIndexed { index, attribution ->
            AttributionItem(showSeparator = index < attributions.lastIndex) {
                AttributionLink(attribution)
            }
        }
    }
}

/** An entry followed by a separator dot that stays with it when the line wraps and is skipped by screen readers. */
@Composable
private fun AttributionItem(
    showSeparator: Boolean,
    content: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.tiny)) {
        content()
        if (showSeparator) {
            Text(
                text = SEPARATOR,
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                modifier = Modifier.clearAndSetSemantics {},
            )
        }
    }
}

@Composable
private fun AttributionLink(attribution: DataAttribution) {
    val open by rememberUpdatedState(attribution.onOpen)
    val text =
        remember(attribution.label, attribution.linkText) {
            buildAnnotatedString {
                val link = LinkAnnotation.Clickable(tag = ATTRIBUTION_TAG, styles = AttributionLinkStyles) { open() }
                val linkStart = attribution.label.indexOf(attribution.linkText)
                if (linkStart < 0) {
                    // A translation that omits the provider name still keeps a working link.
                    withLink(link) { append(attribution.label) }
                } else {
                    append(attribution.label.substring(0, linkStart))
                    withLink(link) { append(attribution.linkText) }
                    append(attribution.label.substring(linkStart + attribution.linkText.length))
                }
            }
        }
    Text(text = text, style = MaterialTheme.typography.caption, color = secondaryText)
}

@Preview
@Composable
internal fun VersionAttributionPreview() {
    BraindanceTheme {
        VersionAttribution(
            versionLabel = "v1.0.0",
            attributions =
                listOf(
                    DataAttribution(label = "Game data by IGDB", linkText = "IGDB", onOpen = {}),
                    DataAttribution(label = "Giveaways by GamerPower", linkText = "GamerPower", onOpen = {}),
                ),
            modifier = Modifier.background(navBar).padding(Dimens.medium),
        )
    }
}
