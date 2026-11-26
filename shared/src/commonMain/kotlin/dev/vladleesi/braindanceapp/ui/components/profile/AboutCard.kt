// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import dev.vladleesi.braindanceapp.resources.app_name
import dev.vladleesi.braindanceapp.resources.data_attribution_gamerpower
import dev.vladleesi.braindanceapp.resources.data_attribution_igdb
import dev.vladleesi.braindanceapp.resources.profile_contact_support
import dev.vladleesi.braindanceapp.resources.profile_licenses
import dev.vladleesi.braindanceapp.resources.profile_privacy_policy
import dev.vladleesi.braindanceapp.resources.profile_release_notes
import dev.vladleesi.braindanceapp.resources.profile_section_about
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.ui.components.GAMERPOWER_NAME
import dev.vladleesi.braindanceapp.ui.components.IGDB_NAME
import dev.vladleesi.braindanceapp.ui.components.LinkDivider
import dev.vladleesi.braindanceapp.ui.components.LinkRow
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.components.SUPPORT_EMAIL
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.icons.BraindanceWordmark
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

private val WordmarkHeight = Dimens.medium + Dimens.small

/**
 * The About section, the pattern settings guidelines recommend at the bottom of a settings page: the app's identity
 * and version first, then plain rows for support and the legal pages, and the data credits as a footnote under the
 * card. The rows have no icons, so the section reads as information rather than as another list of destinations.
 */
@Composable
fun AboutCard(
    links: ProjectLinks,
    onOpenReleases: () -> Unit,
    versionLabel: String,
    statusLabel: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        ProfileSection(title = stringResource(Res.string.profile_section_about)) {
            Column(
                modifier = Modifier.padding(Dimens.medium),
                verticalArrangement = Arrangement.spacedBy(Dimens.small),
            ) {
                // The real wordmark, as in the desktop sidebar: the brand's name is never retyped.
                Image(
                    imageVector = BraindanceWordmark,
                    contentDescription = stringResource(Res.string.app_name),
                    modifier = Modifier.height(WordmarkHeight),
                )
                ProfileVersionStatus(versionLabel = versionLabel, statusLabel = statusLabel)
            }
            LinkDivider()
            // The address is shown too, for people who write from another mail account than the system app's.
            LinkRow(
                title = stringResource(Res.string.profile_contact_support),
                supportingText = SUPPORT_EMAIL,
                imageVector = null,
                reserveIconSpace = false,
                onClick = links.emailSupport,
            )
            LinkDivider()
            LinkRow(
                title = stringResource(Res.string.profile_privacy_policy),
                imageVector = null,
                reserveIconSpace = false,
                onClick = links.openPrivacyPolicy,
            )
            LinkDivider()
            LinkRow(
                title = stringResource(Res.string.profile_licenses),
                imageVector = null,
                reserveIconSpace = false,
                onClick = links.openLicenses,
            )
            LinkDivider()
            LinkRow(
                title = stringResource(Res.string.profile_release_notes),
                imageVector = null,
                reserveIconSpace = false,
                onClick = onOpenReleases,
            )
        }
        // The providers' credits, as their terms ask: small, but each a full touch target.
        FlowRow(
            modifier = Modifier.padding(horizontal = Dimens.small),
            itemVerticalAlignment = Alignment.CenterVertically,
        ) {
            CreditLink(
                text = credit(stringResource(Res.string.data_attribution_igdb, IGDB_NAME), IGDB_NAME),
                onClick = links.openIgdb,
            )
            Text(
                text = "·",
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                modifier = Modifier.clearAndSetSemantics {},
            )
            CreditLink(
                text = credit(stringResource(Res.string.data_attribution_gamerpower, GAMERPOWER_NAME), GAMERPOWER_NAME),
                onClick = links.openGamerPower,
            )
        }
    }
}

/** A credit sentence with the provider's [name] in the link color; translations may place the name anywhere. */
private fun credit(
    sentence: String,
    name: String,
): AnnotatedString {
    val start = sentence.lastIndexOf(name)
    if (start < 0) return AnnotatedString(sentence)
    return buildAnnotatedString {
        append(sentence.substring(0, start))
        withStyle(SpanStyle(color = accent)) { append(name) }
        append(sentence.substring(start + name.length))
    }
}

/**
 * A credit that opens its provider's site. The whole text is the target, at least the platform's minimum touch size.
 * A pointer underlines it, and the keyboard shows the focus ring.
 */
@Composable
private fun CreditLink(
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

@Preview
@Composable
internal fun AboutCardPreview() {
    BraindanceTheme {
        AboutCard(
            links =
                ProjectLinks(
                    shareLabel = Res.string.profile_support_share,
                    openRepository = {},
                    share = {},
                    openIgdb = {},
                    openGamerPower = {},
                    emailSupport = {},
                    openPrivacyPolicy = {},
                    openLicenses = {},
                ),
            onOpenReleases = {},
            versionLabel = "v1.0.0",
            statusLabel = "In development",
            modifier = Modifier.padding(Dimens.medium),
        )
    }
}
