// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Github
import Share
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_activity
import dev.vladleesi.braindanceapp.resources.profile_bluesky_handle
import dev.vladleesi.braindanceapp.resources.profile_development_status
import dev.vladleesi.braindanceapp.resources.profile_link_bluesky
import dev.vladleesi.braindanceapp.resources.profile_link_discord
import dev.vladleesi.braindanceapp.resources.profile_link_discord_description
import dev.vladleesi.braindanceapp.resources.profile_link_github
import dev.vladleesi.braindanceapp.resources.profile_link_github_description
import dev.vladleesi.braindanceapp.resources.profile_link_reddit
import dev.vladleesi.braindanceapp.resources.profile_link_threads
import dev.vladleesi.braindanceapp.resources.profile_link_x
import dev.vladleesi.braindanceapp.resources.profile_reddit_handle
import dev.vladleesi.braindanceapp.resources.profile_reviews
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.resources.profile_screen_version
import dev.vladleesi.braindanceapp.resources.profile_section_account
import dev.vladleesi.braindanceapp.resources.profile_section_community
import dev.vladleesi.braindanceapp.resources.profile_settings
import dev.vladleesi.braindanceapp.resources.profile_sign_in
import dev.vladleesi.braindanceapp.resources.profile_social_handle
import dev.vladleesi.braindanceapp.resources.profile_statistics
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.ui.components.ComingSoonLabel
import dev.vladleesi.braindanceapp.ui.components.CommunityLinks
import dev.vladleesi.braindanceapp.ui.components.LinkDivider
import dev.vladleesi.braindanceapp.ui.components.LinkRow
import dev.vladleesi.braindanceapp.ui.components.LinkRowTextInset
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.profile.AboutCard
import dev.vladleesi.braindanceapp.ui.components.profile.ProfileSection
import dev.vladleesi.braindanceapp.ui.components.rememberCommunityLinks
import dev.vladleesi.braindanceapp.ui.components.rememberProjectLinks
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.icons.BlueskyLogo
import dev.vladleesi.braindanceapp.ui.style.icons.DiscordLogo
import dev.vladleesi.braindanceapp.ui.style.icons.RedditLogo
import dev.vladleesi.braindanceapp.ui.style.icons.ThreadsLogo
import dev.vladleesi.braindanceapp.ui.style.icons.XLogo
import org.jetbrains.compose.resources.stringResource

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
 * About with the app version, support, the legal pages, and the data credits. Rows of
 * one list read best at a readable width, so the page is a single centered column, its title included, instead of
 * rows stretched across a wide window.
 */
@Composable
internal fun ProfileContent(
    links: ProjectLinks,
    community: CommunityLinks,
    versionLabel: String,
    onBackButtonPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val gutter = pageGutter()
    Box(modifier = modifier.fillMaxSize()) {
        val scrollState = rememberScrollState()
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = Dimens.medium),
        ) {
            SpacerStatusBarInsets()
            TopAppBar(
                title = stringResource(Res.string.profile_screen_title),
                titleStyle = MaterialTheme.typography.h2,
                tabBarColor = background,
                showBackButton = true,
                onBackButtonPressed = onBackButtonPressed,
                maxContentWidth = Dimens.readableTextWidth,
            )
            Column(
                modifier =
                    Modifier
                        .pageColumn(gutter, maxContentWidth = Dimens.readableTextWidth)
                        .padding(top = Dimens.small),
                verticalArrangement = Arrangement.spacedBy(Dimens.large),
            ) {
                AccountSection()
                CommunitySection(links = links, community = community)
                AboutCard(
                    links = links,
                    onOpenReleases = community.openReleases,
                    versionLabel = versionLabel,
                    statusLabel = stringResource(Res.string.profile_development_status),
                )
            }
        }
        PageScrollbar(scrollState)
    }
}

/** Account features planned for Profile, listed as rows that are not interactive yet. */
@Composable
private fun AccountSection() {
    ProfileSection(title = stringResource(Res.string.profile_section_account)) {
        ComingSoonRow(stringResource(Res.string.profile_sign_in))
        LinkDivider()
        ComingSoonRow(stringResource(Res.string.profile_statistics))
        LinkDivider()
        ComingSoonRow(stringResource(Res.string.profile_activity))
        LinkDivider()
        ComingSoonRow(stringResource(Res.string.profile_reviews))
        LinkDivider()
        ComingSoonRow(stringResource(Res.string.profile_settings))
    }
}

/**
 * Ways to support and follow the project; every row but sharing opens a page outside the app. Sharing is left out of
 * the Web desktop layout.
 */
@Composable
private fun CommunitySection(
    links: ProjectLinks,
    community: CommunityLinks,
) {
    val handle = stringResource(Res.string.profile_social_handle)
    ProfileSection(title = stringResource(Res.string.profile_section_community)) {
        LinkRow(
            title = stringResource(Res.string.profile_link_discord),
            supportingText = stringResource(Res.string.profile_link_discord_description),
            imageVector = DiscordLogo,
            onClick = community.openDiscord,
        )
        LinkDivider(inset = LinkRowTextInset)
        LinkRow(
            title = stringResource(Res.string.profile_link_reddit),
            supportingText = stringResource(Res.string.profile_reddit_handle),
            imageVector = RedditLogo,
            onClick = community.openReddit,
        )
        LinkDivider(inset = LinkRowTextInset)
        LinkRow(
            title = stringResource(Res.string.profile_link_x),
            supportingText = handle,
            imageVector = XLogo,
            onClick = community.openX,
        )
        LinkDivider(inset = LinkRowTextInset)
        LinkRow(
            title = stringResource(Res.string.profile_link_bluesky),
            supportingText = stringResource(Res.string.profile_bluesky_handle),
            imageVector = BlueskyLogo,
            onClick = community.openBluesky,
        )
        LinkDivider(inset = LinkRowTextInset)
        LinkRow(
            title = stringResource(Res.string.profile_link_threads),
            supportingText = handle,
            imageVector = ThreadsLogo,
            onClick = community.openThreads,
        )
        LinkDivider(inset = LinkRowTextInset)
        LinkRow(
            title = stringResource(Res.string.profile_link_github),
            supportingText = stringResource(Res.string.profile_link_github_description),
            imageVector = Github,
            onClick = links.openRepository,
        )
        // Like content's share buttons, sharing the app is left out of the Web desktop layout (see showsShareActions).
        if (showsShareActions()) {
            LinkDivider(inset = LinkRowTextInset)
            LinkRow(
                title = stringResource(links.shareLabel),
                imageVector = Share,
                onClick = links.share,
                opensExternally = false,
            )
        }
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
                        emailSupport = {},
                        openPrivacyPolicy = {},
                        openLicenses = {},
                    ),
                community =
                    CommunityLinks(
                        openThreads = {},
                        openX = {},
                        openBluesky = {},
                        openReddit = {},
                        openDiscord = {},
                        openReleases = {},
                    ),
                versionLabel = "v1.0.0",
                onBackButtonPressed = {},
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
