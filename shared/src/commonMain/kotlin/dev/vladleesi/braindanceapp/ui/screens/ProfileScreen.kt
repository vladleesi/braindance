// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Github
import Share
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.data_attribution_gamerpower
import dev.vladleesi.braindanceapp.resources.data_attribution_igdb
import dev.vladleesi.braindanceapp.resources.profile_activity
import dev.vladleesi.braindanceapp.resources.profile_development_status
import dev.vladleesi.braindanceapp.resources.profile_link_github
import dev.vladleesi.braindanceapp.resources.profile_link_github_description
import dev.vladleesi.braindanceapp.resources.profile_link_threads
import dev.vladleesi.braindanceapp.resources.profile_link_x
import dev.vladleesi.braindanceapp.resources.profile_release_notes
import dev.vladleesi.braindanceapp.resources.profile_reviews
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.resources.profile_screen_version
import dev.vladleesi.braindanceapp.resources.profile_section_about
import dev.vladleesi.braindanceapp.resources.profile_section_account
import dev.vladleesi.braindanceapp.resources.profile_section_community
import dev.vladleesi.braindanceapp.resources.profile_settings
import dev.vladleesi.braindanceapp.resources.profile_sign_in
import dev.vladleesi.braindanceapp.resources.profile_social_handle
import dev.vladleesi.braindanceapp.resources.profile_statistics
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.ui.components.ComingSoonLabel
import dev.vladleesi.braindanceapp.ui.components.CommunityLinks
import dev.vladleesi.braindanceapp.ui.components.GAMERPOWER_NAME
import dev.vladleesi.braindanceapp.ui.components.IGDB_NAME
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileDivider
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileLinkRow
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileRowTextInset
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileSection
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileVersionStatus
import dev.vladleesi.braindanceapp.ui.components.rememberCommunityLinks
import dev.vladleesi.braindanceapp.ui.components.rememberProjectLinks
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.icons.Database
import dev.vladleesi.braindanceapp.ui.style.icons.FileText
import dev.vladleesi.braindanceapp.ui.style.icons.Gift
import dev.vladleesi.braindanceapp.ui.style.icons.ThreadsLogo
import dev.vladleesi.braindanceapp.ui.style.icons.XLogo
import org.jetbrains.compose.resources.stringResource

// Wide windows keep the lists at a readable width instead of stretching them across the screen.
private val ContentMaxWidth = 640.dp

@Composable
fun ProfileScreen(
    onBackButtonPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ProfileContent(
        links = rememberProjectLinks(),
        community = rememberCommunityLinks(),
        versionLabel = stringResource(Res.string.profile_screen_version, BuildKonfig.APP_VERSION),
        onBackButtonPressed = onBackButtonPressed,
        modifier = modifier,
    )
}

/**
 * Profile, grouped the way a settings list is: the account features that are coming, then the community links, then
 * what the app is built on, with the app version and its development stage as a quiet footer at the bottom.
 */
@Composable
internal fun ProfileContent(
    links: ProjectLinks,
    community: CommunityLinks,
    versionLabel: String,
    onBackButtonPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(bottom = Dimens.medium),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                SpacerStatusBarInsets()
                TopAppBar(
                    title = stringResource(Res.string.profile_screen_title),
                    titleStyle = MaterialTheme.typography.h2,
                    showBackButton = true,
                    onBackButtonPressed = onBackButtonPressed,
                )
                Column(
                    modifier =
                        Modifier
                            .widthIn(max = ContentMaxWidth)
                            .padding(horizontal = Dimens.medium)
                            .padding(top = Dimens.small),
                    verticalArrangement = Arrangement.spacedBy(Dimens.large),
                ) {
                    AccountSection()
                    CommunitySection(links = links, community = community)
                    AboutSection(links = links, community = community)
                }
            }
            ProfileVersionStatus(
                versionLabel = versionLabel,
                statusLabel = stringResource(Res.string.profile_development_status),
                modifier =
                    Modifier
                        .widthIn(max = ContentMaxWidth)
                        // Lines up with the row content: the sections' own margin plus the row padding.
                        .padding(horizontal = Dimens.large)
                        .padding(top = Dimens.large),
            )
        }
    }
}

/** Account features planned for Profile, listed as rows that are not interactive yet. */
@Composable
private fun AccountSection() {
    ProfileSection(title = stringResource(Res.string.profile_section_account)) {
        ComingSoonRow(stringResource(Res.string.profile_sign_in))
        ProfileDivider()
        ComingSoonRow(stringResource(Res.string.profile_statistics))
        ProfileDivider()
        ComingSoonRow(stringResource(Res.string.profile_activity))
        ProfileDivider()
        ComingSoonRow(stringResource(Res.string.profile_reviews))
        ProfileDivider()
        ComingSoonRow(stringResource(Res.string.profile_settings))
    }
}

/** Ways to support and follow the project; every row but sharing opens a page outside the app. */
@Composable
private fun CommunitySection(
    links: ProjectLinks,
    community: CommunityLinks,
) {
    val handle = stringResource(Res.string.profile_social_handle)
    ProfileSection(title = stringResource(Res.string.profile_section_community)) {
        ProfileLinkRow(
            title = stringResource(Res.string.profile_link_github),
            supportingText = stringResource(Res.string.profile_link_github_description),
            imageVector = Github,
            onClick = links.openRepository,
        )
        ProfileDivider(inset = ProfileRowTextInset)
        ProfileLinkRow(
            title = stringResource(Res.string.profile_link_threads),
            supportingText = handle,
            imageVector = ThreadsLogo,
            onClick = community.openThreads,
        )
        ProfileDivider(inset = ProfileRowTextInset)
        ProfileLinkRow(
            title = stringResource(Res.string.profile_link_x),
            supportingText = handle,
            imageVector = XLogo,
            onClick = community.openX,
        )
        ProfileDivider(inset = ProfileRowTextInset)
        ProfileLinkRow(
            title = stringResource(links.shareLabel),
            imageVector = Share,
            onClick = links.share,
            opensExternally = false,
        )
    }
}

/** What changed in the app and the services its data comes from, credited as their terms ask. */
@Composable
private fun AboutSection(
    links: ProjectLinks,
    community: CommunityLinks,
) {
    ProfileSection(title = stringResource(Res.string.profile_section_about)) {
        ProfileLinkRow(
            title = stringResource(Res.string.profile_release_notes),
            imageVector = FileText,
            onClick = community.openReleases,
        )
        ProfileDivider(inset = ProfileRowTextInset)
        ProfileLinkRow(
            title = stringResource(Res.string.data_attribution_igdb, IGDB_NAME),
            imageVector = Database,
            onClick = links.openIgdb,
        )
        ProfileDivider(inset = ProfileRowTextInset)
        ProfileLinkRow(
            title = stringResource(Res.string.data_attribution_gamerpower, GAMERPOWER_NAME),
            imageVector = Gift,
            onClick = links.openGamerPower,
        )
    }
}

@Composable
private fun ComingSoonRow(title: String) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {}
                .padding(horizontal = Dimens.medium, vertical = Dimens.compact),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        Text(text = title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.subtitle1)
        ComingSoonLabel()
    }
}

@Preview(name = "Profile", widthDp = 360, heightDp = 800)
@Composable
internal fun ProfileScreenPreview() {
    BraindanceTheme {
        Surface(color = MaterialTheme.colors.background) {
            ProfileContent(
                links =
                    ProjectLinks(
                        shareLabel = Res.string.profile_support_share,
                        openRepository = {},
                        share = {},
                        openIgdb = {},
                        openGamerPower = {},
                    ),
                community = CommunityLinks(openThreads = {}, openX = {}, openReleases = {}),
                versionLabel = "v1.0.0",
                onBackButtonPressed = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
