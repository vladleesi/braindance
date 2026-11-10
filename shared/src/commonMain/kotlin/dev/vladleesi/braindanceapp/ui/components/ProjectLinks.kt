// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_support_copy
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.resources.share_app_message
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.kmpsharekit.ShareKit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val REPOSITORY_URL = "https://github.com/vladleesi/braindance"
private const val APP_URL = "https://app.braindancegg.com/"

/**
 * The project's promotion actions. [openRepository] opens the GitHub repository; [share] recommends the Braindance
 * Web app with a short message and its link, through the system share sheet where the platform supports it, otherwise
 * by copying the same message. [shareLabel] names whichever of the two [share] does.
 */
@Immutable
class ProjectLinks(
    val shareLabel: StringResource,
    val openRepository: () -> Unit,
    val share: () -> Unit,
)

/** The project actions shared by Profile and the desktop sidebar. */
@Composable
fun rememberProjectLinks(): ProjectLinks {
    val uriHandler = LocalUriHandler.current
    val shareMessage = stringResource(Res.string.share_app_message, APP_URL)

    // Compose's common Clipboard API does not yet expose a plain-text ClipEntry factory.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    return remember(uriHandler, clipboard, shareMessage) {
        val nativeSharingSupported = ShareKit.isSupported()
        ProjectLinks(
            shareLabel =
                if (nativeSharingSupported) Res.string.profile_support_share else Res.string.profile_support_copy,
            openRepository = { uriHandler.openExternalUrl(REPOSITORY_URL) },
            share = {
                if (nativeSharingSupported) {
                    ShareKit.shareText(shareMessage)
                } else {
                    clipboard.setText(AnnotatedString(shareMessage))
                }
            },
        )
    }
}
