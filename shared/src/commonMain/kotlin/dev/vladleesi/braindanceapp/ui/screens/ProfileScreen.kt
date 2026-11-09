// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_activity
import dev.vladleesi.braindanceapp.resources.profile_open_source_title
import dev.vladleesi.braindanceapp.resources.profile_reviews
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.resources.profile_screen_version
import dev.vladleesi.braindanceapp.resources.profile_settings
import dev.vladleesi.braindanceapp.resources.profile_sign_in
import dev.vladleesi.braindanceapp.resources.profile_statistics
import dev.vladleesi.braindanceapp.resources.profile_support_copy
import dev.vladleesi.braindanceapp.resources.profile_support_description
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.resources.profile_support_star
import dev.vladleesi.braindanceapp.ui.components.ComingSoonLabel
import dev.vladleesi.braindanceapp.ui.components.OpenSourceSection
import dev.vladleesi.braindanceapp.ui.components.OpenSourceSectionTexts
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.kmpsharekit.ShareKit
import org.jetbrains.compose.resources.stringResource

private const val GITHUB_URL = "https://github.com/vladleesi/braindance"

@Composable
fun ProfileScreen(
    onBackButtonPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val uriHandler = LocalUriHandler.current
    val nativeSharingSupported = ShareKit.isSupported()

    // Compose's common Clipboard API does not yet expose a plain-text ClipEntry factory.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(bottom = Dimens.small),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                SpacerStatusBarInsets()
                TopAppBar(
                    title = stringResource(Res.string.profile_screen_title),
                    titleStyle = MaterialTheme.typography.h2,
                    showBackButton = true,
                    onBackButtonPressed = onBackButtonPressed,
                )
                ProfileComingSoon(modifier = Modifier.padding(horizontal = Dimens.medium))
            }
            OpenSourceSection(
                texts =
                    OpenSourceSectionTexts(
                        title = stringResource(Res.string.profile_open_source_title),
                        description = stringResource(Res.string.profile_support_description),
                        starLabel = stringResource(Res.string.profile_support_star),
                        shareLabel =
                            stringResource(
                                if (nativeSharingSupported) {
                                    Res.string.profile_support_share
                                } else {
                                    Res.string.profile_support_copy
                                },
                            ),
                        versionLabel = stringResource(Res.string.profile_screen_version, BuildKonfig.APP_VERSION),
                    ),
                onOpenRepository = { uriHandler.openUri(GITHUB_URL) },
                onShare = {
                    if (nativeSharingSupported) {
                        ShareKit.shareText(GITHUB_URL)
                    } else {
                        clipboard.setText(AnnotatedString(GITHUB_URL))
                    }
                },
                modifier =
                    Modifier
                        .padding(horizontal = Dimens.medium)
                        .padding(top = Dimens.large),
            )
        }
    }
}

/** Account features planned for Profile, listed as rows that are not interactive yet. */
@Composable
private fun ProfileComingSoon(modifier: Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CornerShapes.medium,
        color = MaterialTheme.colors.primary,
    ) {
        Column(modifier = Modifier.padding(vertical = Dimens.small)) {
            ComingSoonRow(stringResource(Res.string.profile_sign_in))
            ComingSoonRow(stringResource(Res.string.profile_statistics))
            ComingSoonRow(stringResource(Res.string.profile_activity))
            ComingSoonRow(stringResource(Res.string.profile_reviews))
            ComingSoonRow(stringResource(Res.string.profile_settings))
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
            ProfileScreen(onBackButtonPressed = {}, modifier = Modifier.fillMaxSize())
        }
    }
}
