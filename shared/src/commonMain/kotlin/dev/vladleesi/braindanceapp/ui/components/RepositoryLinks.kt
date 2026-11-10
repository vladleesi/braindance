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
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.kmpsharekit.ShareKit
import org.jetbrains.compose.resources.StringResource

private const val REPOSITORY_URL = "https://github.com/vladleesi/braindance"

/**
 * Opening and sharing the project's GitHub repository. [shareLabel] names the share action: the system share sheet
 * where the platform supports it, otherwise copying the link.
 */
@Immutable
class RepositoryLinks(
    val shareLabel: StringResource,
    val openRepository: () -> Unit,
    val share: () -> Unit,
)

/** The repository actions shared by Profile and the desktop sidebar. */
@Composable
fun rememberRepositoryLinks(): RepositoryLinks {
    val uriHandler = LocalUriHandler.current

    // Compose's common Clipboard API does not yet expose a plain-text ClipEntry factory.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    return remember(uriHandler, clipboard) {
        val nativeSharingSupported = ShareKit.isSupported()
        RepositoryLinks(
            shareLabel =
                if (nativeSharingSupported) Res.string.profile_support_share else Res.string.profile_support_copy,
            openRepository = { uriHandler.openExternalUrl(REPOSITORY_URL) },
            share = {
                if (nativeSharingSupported) {
                    ShareKit.shareText(REPOSITORY_URL)
                } else {
                    clipboard.setText(AnnotatedString(REPOSITORY_URL))
                }
            },
        )
    }
}
