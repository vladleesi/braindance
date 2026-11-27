// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.externalLink
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.data_attribution_gamerpower
import dev.vladleesi.braindanceapp.resources.data_attribution_igdb
import dev.vladleesi.braindanceapp.resources.footer_licenses
import dev.vladleesi.braindanceapp.resources.footer_privacy
import dev.vladleesi.braindanceapp.resources.footer_support
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

// WCAG 2.2's minimum target size (2.5.8), for links a pointer usually reaches.
private val CompactLinkHeight = 24.dp

/*
 * Small text links for credits and legal pages, as a footer would hold them: Profile's About section and the desktop
 * sidebar show them. Each opens a page outside the app.
 */

/**
 * A small link that opens its page with [onClick]. The whole text is the target, at least the platform's minimum
 * touch size, or 24 dp tall when [compact], for the desktop sidebar, where a pointer is the usual input: the target
 * size WCAG 2.2 asks for at least. A pointer underlines it, and the keyboard shows the focus ring.
 */
@Composable
fun FooterLink(
    text: AnnotatedString,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val targetSize =
        if (compact) {
            Modifier.heightIn(
                min = CompactLinkHeight,
            )
        } else {
            Modifier.minimumInteractiveComponentSize()
        }
    Box(
        modifier =
            modifier
                .then(targetSize)
                .clip(CornerShapes.small)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small)
                .pointerHoverIcon(PointerIcon.Hand)
                .externalLink(onClick)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = if (compact) Dimens.tiny + Dimens.micro else Dimens.small),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            // The sidebar's links are footnotes, so they take the smallest label size.
            style = if (compact) MaterialTheme.typography.overline else MaterialTheme.typography.caption,
            color = secondaryText,
            textDecoration = if (hovered) TextDecoration.Underline else null,
        )
    }
}

/** The dot between footer links, which screen readers skip. */
@Composable
fun FooterSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.caption,
        color = secondaryText,
        modifier = Modifier.clearAndSetSemantics {},
    )
}

/**
 * The providers' credits, as their terms ask: small, but each a full touch target. They share a line when it fits.
 * [compact] puts each on its own line, with [FooterLink]'s compact height, for the narrow desktop sidebar.
 */
@Composable
fun DataCredits(
    links: ProjectLinks,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val igdb = credit(stringResource(Res.string.data_attribution_igdb, IGDB_NAME), IGDB_NAME, accent)
    val gamerPower =
        credit(stringResource(Res.string.data_attribution_gamerpower, GAMERPOWER_NAME), GAMERPOWER_NAME, accent)
    if (compact) {
        Column(modifier = modifier) {
            FooterLink(text = igdb, onClick = links.openIgdb, compact = true)
            FooterLink(text = gamerPower, onClick = links.openGamerPower, compact = true)
        }
    } else {
        FlowRow(modifier = modifier, itemVerticalAlignment = Alignment.CenterVertically) {
            FooterLink(text = igdb, onClick = links.openIgdb)
            FooterSeparator()
            FooterLink(text = gamerPower, onClick = links.openGamerPower)
        }
    }
}

/**
 * The links a site's footer would hold, compact for the desktop sidebar: privacy, licenses, and support on one line
 * when it fits, then the data credits. Space alone separates them, so a wrapped line never starts or ends with a
 * separator. Phones, tablets, and narrower windows reach the same pages from Profile.
 */
@Composable
fun FooterLinks(
    links: ProjectLinks,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        FlowRow(itemVerticalAlignment = Alignment.CenterVertically) {
            FooterLink(
                text = AnnotatedString(stringResource(Res.string.footer_privacy)),
                onClick = links.openPrivacyPolicy,
                compact = true,
            )
            FooterLink(
                text = AnnotatedString(stringResource(Res.string.footer_licenses)),
                onClick = links.openLicenses,
                compact = true,
            )
            FooterLink(
                text = AnnotatedString(stringResource(Res.string.footer_support)),
                onClick = links.emailSupport,
                compact = true,
            )
        }
        DataCredits(links = links, compact = true)
    }
}

/** A credit sentence with the provider's [name] in [linkColor]. Translations may place the name anywhere. */
private fun credit(
    sentence: String,
    name: String,
    linkColor: Color,
): AnnotatedString {
    val start = sentence.lastIndexOf(name)
    if (start < 0) return AnnotatedString(sentence)
    return buildAnnotatedString {
        append(sentence.substring(0, start))
        withStyle(SpanStyle(color = linkColor)) { append(name) }
        append(sentence.substring(start + name.length))
    }
}

@Preview(widthDp = 232)
@Composable
internal fun FooterLinksPreview() {
    BraindanceTheme {
        FooterLinks(links = PreviewProjectLinks, modifier = Modifier.background(navBar).padding(Dimens.medium))
    }
}
