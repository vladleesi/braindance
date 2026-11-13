// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.toWebPath
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_support_copy
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.resources.share_app_message
import dev.vladleesi.braindanceapp.resources.share_game_message
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.kmpsharekit.ShareKit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val REPOSITORY_URL = "https://github.com/vladleesi/braindance"
private const val APP_URL = "https://app.braindancegg.com/"
private const val IGDB_URL = "https://www.igdb.com/"
private const val GAMERPOWER_URL = "https://www.gamerpower.com/"
private const val RELEASES_URL = "$REPOSITORY_URL/releases"
private const val THREADS_URL = "https://www.threads.com/@braindancegg"
private const val X_URL = "https://x.com/braindancegg"

// Provider names are brands, so they are not translated; the attribution strings place them in a sentence.
internal const val IGDB_NAME = "IGDB"
internal const val GAMERPOWER_NAME = "GamerPower"

/**
 * The project's promotion actions. [openRepository] opens the GitHub repository; [share] recommends the Braindance
 * Web app with a short message and its link, through the system share sheet where the platform supports it, otherwise
 * by copying the same message. [shareLabel] names whichever of the two [share] does. [openIgdb] and [openGamerPower]
 * open the providers of the game data and the giveaways for the data attributions.
 */
@Immutable
class ProjectLinks(
    val shareLabel: StringResource,
    val openRepository: () -> Unit,
    val share: () -> Unit,
    val openIgdb: () -> Unit,
    val openGamerPower: () -> Unit,
)

/** A share action for one piece of content: [label] names what [share] does on this platform. */
@Immutable
class ShareAction(
    val label: StringResource,
    val share: () -> Unit,
)

/** Where people follow the project: its Threads and X profiles and the GitHub release notes. Each opens its page. */
@Immutable
class CommunityLinks(
    val openThreads: () -> Unit,
    val openX: () -> Unit,
    val openReleases: () -> Unit,
)

/** The project actions shared by Profile and the desktop sidebar. */
@Composable
fun rememberProjectLinks(): ProjectLinks {
    val uriHandler = LocalUriHandler.current
    val shareMessage = stringResource(Res.string.share_app_message, APP_URL)

    val shareApp = rememberShareAction(shareMessage)

    return remember(uriHandler, shareApp) {
        ProjectLinks(
            shareLabel = shareApp.label,
            openRepository = { uriHandler.openExternalUrl(REPOSITORY_URL) },
            share = shareApp.share,
            openIgdb = { uriHandler.openExternalUrl(IGDB_URL) },
            openGamerPower = { uriHandler.openExternalUrl(GAMERPOWER_URL) },
        )
    }
}

/** Shares the game [gameId] ("[gameName]") with a link to its page in the Web app. */
@Composable
fun rememberShareGameAction(
    gameId: Int,
    gameName: String,
): ShareAction {
    val gameUrl = APP_URL.trimEnd('/') + GameDetailsRoute(gameId).toWebPath()
    val message = stringResource(Res.string.share_game_message, gameName, gameUrl)
    return rememberShareAction(message)
}

/** Shares [message] through the system share sheet where the platform supports it, otherwise copies it. */
@Composable
private fun rememberShareAction(message: String): ShareAction {
    // Compose's common Clipboard API does not yet expose a plain-text ClipEntry factory.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    return remember(clipboard, message) {
        val nativeSharingSupported = ShareKit.isSupported()
        ShareAction(
            label = if (nativeSharingSupported) Res.string.profile_support_share else Res.string.profile_support_copy,
            share = {
                if (nativeSharingSupported) {
                    ShareKit.shareText(message)
                } else {
                    clipboard.setText(AnnotatedString(message))
                }
            },
        )
    }
}

/** The community links used by the desktop sidebar. */
@Composable
fun rememberCommunityLinks(): CommunityLinks {
    val uriHandler = LocalUriHandler.current
    return remember(uriHandler) {
        CommunityLinks(
            openThreads = { uriHandler.openExternalUrl(THREADS_URL) },
            openX = { uriHandler.openExternalUrl(X_URL) },
            openReleases = { uriHandler.openExternalUrl(RELEASES_URL) },
        )
    }
}
