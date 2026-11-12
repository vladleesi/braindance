// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.data_attribution_gamerpower
import dev.vladleesi.braindanceapp.resources.data_attribution_igdb
import dev.vladleesi.braindanceapp.resources.profile_support_copy
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.resources.share_app_message
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.kmpsharekit.ShareKit
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val REPOSITORY_URL = "https://github.com/vladleesi/braindance"
private const val APP_URL = "https://app.braindancegg.com/"
private const val IGDB_URL = "https://www.igdb.com/"
private const val GAMERPOWER_URL = "https://www.gamerpower.com/"

// Provider names are brands, so they are not translated; the attribution strings place them in a sentence.
private const val IGDB_NAME = "IGDB"
private const val GAMERPOWER_NAME = "GamerPower"

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
            openIgdb = { uriHandler.openExternalUrl(IGDB_URL) },
            openGamerPower = { uriHandler.openExternalUrl(GAMERPOWER_URL) },
        )
    }
}

/** The data attributions shared by Profile and the desktop sidebar: game data by IGDB, giveaways by GamerPower. */
@Composable
fun ProjectLinks.rememberDataAttributions(): List<DataAttribution> {
    val igdbLabel = stringResource(Res.string.data_attribution_igdb, IGDB_NAME)
    val gamerPowerLabel = stringResource(Res.string.data_attribution_gamerpower, GAMERPOWER_NAME)
    return remember(this, igdbLabel, gamerPowerLabel) {
        listOf(
            DataAttribution(label = igdbLabel, linkText = IGDB_NAME, onOpen = openIgdb),
            DataAttribution(label = gamerPowerLabel, linkText = GAMERPOWER_NAME, onOpen = openGamerPower),
        )
    }
}
