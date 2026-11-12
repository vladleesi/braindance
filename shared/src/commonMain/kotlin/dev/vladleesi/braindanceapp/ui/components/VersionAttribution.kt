// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
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

// A non-breaking space keeps the dot with the version if a long translation wraps the attribution.
private const val SEPARATOR = " · "
private const val ATTRIBUTION_TAG = "data-attribution"

private val AttributionLinkStyles =
    TextLinkStyles(
        style = SpanStyle(textDecoration = TextDecoration.Underline),
        focusedStyle = SpanStyle(color = white, background = secondary),
        hoveredStyle = SpanStyle(color = white),
        pressedStyle = SpanStyle(color = white),
    )

/**
 * The app version and the data attribution as one quiet caption line, such as "v1.0.0 · Powered by IGDB". The
 * attribution is a text link, so screen readers announce it as a link, the keyboard can focus it, and pointers show a
 * hand cursor; [onOpenDataSource] opens the data source.
 */
@Composable
fun VersionAttribution(
    versionLabel: String,
    attributionLabel: String,
    onOpenDataSource: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val openDataSource by rememberUpdatedState(onOpenDataSource)
    val text =
        remember(versionLabel, attributionLabel) {
            buildAnnotatedString {
                append(versionLabel)
                append(SEPARATOR)
                val link =
                    LinkAnnotation.Clickable(tag = ATTRIBUTION_TAG, styles = AttributionLinkStyles) { openDataSource() }
                withLink(link) { append(attributionLabel) }
            }
        }
    Text(
        text = text,
        style = MaterialTheme.typography.caption,
        color = secondaryText,
        modifier = modifier,
    )
}

@Preview
@Composable
internal fun VersionAttributionPreview() {
    BraindanceTheme {
        VersionAttribution(
            versionLabel = "v1.0.0",
            attributionLabel = "Powered by IGDB",
            onOpenDataSource = {},
            modifier = Modifier.background(navBar).padding(Dimens.medium),
        )
    }
}
