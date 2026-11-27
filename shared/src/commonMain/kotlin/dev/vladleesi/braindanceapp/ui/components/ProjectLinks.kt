// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalUriHandler
import dev.vladleesi.braindanceapp.navigation.APP_LINK_ORIGIN
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_support_copy
import dev.vladleesi.braindanceapp.resources.profile_support_share
import dev.vladleesi.braindanceapp.resources.share_app_message
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val REPOSITORY_URL = "https://github.com/vladleesi/braindance"
private const val APP_URL = "$APP_LINK_ORIGIN/"
private const val IGDB_URL = "https://www.igdb.com/"
private const val GAMERPOWER_URL = "https://www.gamerpower.com/"
private const val RELEASES_URL = "$REPOSITORY_URL/releases"
private const val THREADS_URL = "https://www.threads.com/@braindancegg"
private const val X_URL = "https://x.com/braindancegg"
private const val BLUESKY_URL = "https://bsky.app/profile/braindancegg.com"
private const val REDDIT_URL = "https://www.reddit.com/r/BraindanceGG/"
private const val DISCORD_URL = "https://discord.gg/XawnYAVdzj"
private const val PRIVACY_POLICY_URL = "https://braindancegg.com/privacy/"
private const val TERMS_URL = "https://braindancegg.com/terms/"

// The README's License section explains the app's license and links the third-party notices, as the site's footer does.
private const val LICENSES_URL = "$REPOSITORY_URL#-license"

/** The public address for questions and problems with the apps; Profile shows it. */
internal const val SUPPORT_EMAIL = "support@braindancegg.com"
private const val SUPPORT_MAIL_URL = "mailto:$SUPPORT_EMAIL"

// Provider names are brands, so they are not translated; the attribution strings place them in a sentence.
internal const val IGDB_NAME = "IGDB"
internal const val GAMERPOWER_NAME = "GamerPower"

/**
 * The project's promotion actions. [openRepository] opens the GitHub repository; [share] recommends the Braindance
 * Web app with a short message and its link, through the system share sheet where the platform supports it, otherwise
 * by copying the same message. [shareLabel] names whichever of the two [share] does. [openIgdb] and [openGamerPower]
 * open the providers of the game data and the giveaways for the data attributions. [emailSupport] starts an email to
 * [SUPPORT_EMAIL] in the system's mail app. Profile and the sidebar also link to the privacy policy, terms
 * and licenses.
 */
@Immutable
@Suppress("LongParameterList")
class ProjectLinks(
    val shareLabel: StringResource,
    val openRepository: () -> Unit,
    val share: () -> Unit,
    val openIgdb: () -> Unit,
    val openGamerPower: () -> Unit,
    val emailSupport: () -> Unit,
    val openPrivacyPolicy: () -> Unit,
    val openTerms: () -> Unit,
    val openLicenses: () -> Unit,
)

/**
 * Where people follow the project: its social profiles, subreddit, Discord community, and GitHub release notes.
 * Each opens its page outside the app.
 */
@Immutable
data class CommunityLinks(
    val openThreads: () -> Unit,
    val openX: () -> Unit,
    val openBluesky: () -> Unit,
    val openReddit: () -> Unit,
    val openDiscord: () -> Unit,
    val openReleases: () -> Unit,
)

/** Links that do nothing, for previews. */
internal val PreviewProjectLinks =
    ProjectLinks(
        shareLabel = Res.string.profile_support_share,
        openRepository = {},
        share = {},
        openIgdb = {},
        openGamerPower = {},
        emailSupport = {},
        openPrivacyPolicy = {},
        openTerms = {},
        openLicenses = {},
    )

/** The project actions shared by Profile and the desktop sidebar. */
@Composable
fun rememberProjectLinks(): ProjectLinks {
    val uriHandler = LocalUriHandler.current
    val shareMessage = stringResource(Res.string.share_app_message, APP_URL)

    val shareApp = rememberShareAction(shareMessage)

    return remember(uriHandler, shareApp) {
        ProjectLinks(
            shareLabel =
                if (shareApp.usesShareSheet) Res.string.profile_support_share else Res.string.profile_support_copy,
            openRepository = { uriHandler.openExternalUrl(REPOSITORY_URL) },
            share = { shareApp.share() },
            openIgdb = { uriHandler.openExternalUrl(IGDB_URL) },
            openGamerPower = { uriHandler.openExternalUrl(GAMERPOWER_URL) },
            emailSupport = { uriHandler.openExternalUrl(SUPPORT_MAIL_URL) },
            openPrivacyPolicy = { uriHandler.openExternalUrl(PRIVACY_POLICY_URL) },
            openTerms = { uriHandler.openExternalUrl(TERMS_URL) },
            openLicenses = { uriHandler.openExternalUrl(LICENSES_URL) },
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
            openBluesky = { uriHandler.openExternalUrl(BLUESKY_URL) },
            openReddit = { uriHandler.openExternalUrl(REDDIT_URL) },
            openDiscord = { uriHandler.openExternalUrl(DISCORD_URL) },
            openReleases = { uriHandler.openExternalUrl(RELEASES_URL) },
        )
    }
}
