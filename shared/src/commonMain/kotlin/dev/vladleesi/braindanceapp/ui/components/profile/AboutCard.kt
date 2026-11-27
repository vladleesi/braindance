// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.profile

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.app_name
import dev.vladleesi.braindanceapp.resources.profile_contact_support
import dev.vladleesi.braindanceapp.resources.profile_licenses
import dev.vladleesi.braindanceapp.resources.profile_privacy_policy
import dev.vladleesi.braindanceapp.resources.profile_release_notes
import dev.vladleesi.braindanceapp.resources.profile_section_about
import dev.vladleesi.braindanceapp.ui.components.DataCredits
import dev.vladleesi.braindanceapp.ui.components.LinkDivider
import dev.vladleesi.braindanceapp.ui.components.LinkRow
import dev.vladleesi.braindanceapp.ui.components.PreviewProjectLinks
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.components.SUPPORT_EMAIL
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.icons.BraindanceWordmark
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
        DataCredits(links = links, modifier = Modifier.padding(horizontal = Dimens.small))
    }
}

@Preview
@Composable
internal fun AboutCardPreview() {
    BraindanceTheme {
        AboutCard(
            links = PreviewProjectLinks,
            onOpenReleases = {},
            versionLabel = "v1.0.0",
            statusLabel = "In development",
            modifier = Modifier.padding(Dimens.medium),
        )
    }
}
