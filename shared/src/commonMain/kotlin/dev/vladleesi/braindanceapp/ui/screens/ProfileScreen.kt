// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_open_source_title
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.resources.profile_screen_version
import dev.vladleesi.braindanceapp.resources.profile_support_copy
import dev.vladleesi.braindanceapp.resources.profile_support_description
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.resources.profile_support_star
import dev.vladleesi.braindanceapp.ui.components.OpenSourceSection
import dev.vladleesi.braindanceapp.ui.components.OpenSourceSectionTexts
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.kmpsharekit.ShareKit
import org.jetbrains.compose.resources.stringResource

private const val GITHUB_URL = "https://github.com/vladleesi/braindance"

@Composable
fun ProfileScreen(modifier: Modifier) {
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
                    .padding(horizontal = Dimens.medium)
                    .padding(bottom = Dimens.small),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                SpacerTopBarWithStatusBarInsets()
                Text(
                    modifier = Modifier.semantics { heading() },
                    text = stringResource(Res.string.profile_screen_title),
                    style = MaterialTheme.typography.h2,
                )
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
                modifier = Modifier.padding(top = Dimens.large),
            )
        }
    }
}

@Preview(name = "Profile", widthDp = 360, heightDp = 800)
@Composable
internal fun ProfileScreenPreview() {
    BraindanceTheme {
        Surface(color = MaterialTheme.colors.background) {
            ProfileScreen(Modifier.fillMaxSize())
        }
    }
}
