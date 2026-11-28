// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.FlowRow
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
import dev.vladleesi.braindanceapp.navigation.externalLink
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.data_attribution_gamerpower
import dev.vladleesi.braindanceapp.resources.data_attribution_igdb
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

/**
 * A small link that opens its page with [onClick]. A pointer underlines it, and the keyboard shows the focus ring.
 */
@Composable
private fun FooterLink(
    text: AnnotatedString,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    Box(
        modifier =
            Modifier
                .minimumInteractiveComponentSize()
                .clip(CornerShapes.small)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small)
                .pointerHoverIcon(PointerIcon.Hand)
                .externalLink(onClick)
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Button,
                    onClick = onClick,
                ).padding(horizontal = Dimens.small),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.caption,
            color = secondaryText,
            textDecoration = if (hovered) TextDecoration.Underline else null,
        )
    }
}

/** The dot between footer links, which screen readers skip. */
@Composable
private fun FooterSeparator() {
    Text(
        text = "·",
        style = MaterialTheme.typography.caption,
        color = secondaryText,
        modifier = Modifier.clearAndSetSemantics {},
    )
}

/**
 * The providers' credits, as their terms ask: small, but each a full touch target. They share a line when it fits.
 */
@Composable
fun DataCredits(
    links: ProjectLinks,
    modifier: Modifier = Modifier,
) {
    val igdb = credit(stringResource(Res.string.data_attribution_igdb, IGDB_NAME), IGDB_NAME, accent)
    val gamerPower =
        credit(stringResource(Res.string.data_attribution_gamerpower, GAMERPOWER_NAME), GAMERPOWER_NAME, accent)
    FlowRow(modifier = modifier, itemVerticalAlignment = Alignment.CenterVertically) {
        FooterLink(text = igdb, onClick = links.openIgdb)
        FooterSeparator()
        FooterLink(text = gamerPower, onClick = links.openGamerPower)
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

@Preview(widthDp = 360)
@Composable
internal fun DataCreditsPreview() {
    BraindanceTheme {
        DataCredits(links = PreviewProjectLinks, modifier = Modifier.background(navBar).padding(Dimens.medium))
    }
}
