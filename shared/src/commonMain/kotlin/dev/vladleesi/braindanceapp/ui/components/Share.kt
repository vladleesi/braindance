// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import Check
import Share
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.shareUrl
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.share_compilation_message
import dev.vladleesi.braindanceapp.resources.share_content_description
import dev.vladleesi.braindanceapp.resources.share_copy_link_description
import dev.vladleesi.braindanceapp.resources.share_event_message
import dev.vladleesi.braindanceapp.resources.share_game_message
import dev.vladleesi.braindanceapp.resources.share_giveaway_message
import dev.vladleesi.braindanceapp.resources.share_link_copied
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.system.hasTouchScreen
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith70Alpha
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.kmpsharekit.ShareKit
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private const val TAG = "Share"

// How long a share button shows that it copied the link instead of sharing it.
private const val COPIED_FEEDBACK_MILLIS = 2_000L

/** What a [ShareAction] did. */
enum class ShareResult {
    /** Opened the system share sheet, where people pick a target or cancel. */
    Shared,

    /** Copied the message, because there is no share sheet or it could not open. */
    Copied,
}

/** Shares one message: [usesShareSheet] tells whether [share] opens the system share sheet or copies the message. */
@Immutable
class ShareAction(
    val usesShareSheet: Boolean,
    val share: () -> ShareResult,
)

/**
 * The system share sheet: Android's chooser, iOS's activity sheet, or the Web Share API. [share] throws
 * [IllegalStateException] when the sheet cannot open, such as without a visible screen to present from or in a browser
 * without a secure context or a user gesture, and [UnsupportedOperationException] in a browser without Web Share.
 * Tests provide their own through [LocalShareSheet], since the real sheet needs a running app.
 */
interface ShareSheet {
    fun isSupported(): Boolean

    fun share(text: String)
}

private object PlatformShareSheet : ShareSheet {
    override fun isSupported(): Boolean = ShareKit.isSupported()

    override fun share(text: String) = ShareKit.shareText(text)
}

/** The [ShareSheet] that share actions use. */
val LocalShareSheet = staticCompositionLocalOf<ShareSheet> { PlatformShareSheet }

/**
 * Whether share buttons for content are shown. Touch-screen devices always show them: Android and iOS, and on the Web
 * phones, tablets, and foldables at any width, whose browsers often hide the address bar and offer a share sheet.
 * Narrow windows, in the standard layout, follow mobile conventions and show them too. Only the Web desktop layout on
 * a device without a touch screen leaves them out, because its address bar already holds the link of every screen.
 */
@Composable
fun showsShareActions(): Boolean {
    val touchScreen = remember { hasTouchScreen() }
    return touchScreen || desktopLayout() == null
}

/** Opens the sheet with [text], or returns false when it could not open; nothing has been shown then. */
private fun ShareSheet.tryShare(text: String): Boolean =
    try {
        share(text)
        true
    } catch (e: IllegalStateException) {
        BLogger.warning(e, TAG, "The share sheet did not open; copying instead.")
        false
    } catch (e: UnsupportedOperationException) {
        BLogger.warning(e, TAG, "This browser has no share sheet; copying instead.")
        false
    }

/**
 * Shares [message] through the [ShareSheet]. Where there is none, or it fails to open, the message is copied instead.
 * Cancelling the sheet is not an error. Browsers only open the sheet from a click, so call [ShareAction.share] directly
 * from one.
 */
@Composable
internal fun rememberShareAction(message: String): ShareAction {
    val shareSheet = LocalShareSheet.current

    // Compose's common Clipboard API does not yet expose a plain-text ClipEntry factory.
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    return remember(shareSheet, clipboard, message) {
        val usesShareSheet = shareSheet.isSupported()
        ShareAction(
            usesShareSheet = usesShareSheet,
            share = {
                if (usesShareSheet && shareSheet.tryShare(message)) {
                    ShareResult.Shared
                } else {
                    clipboard.setText(AnnotatedString(message))
                    ShareResult.Copied
                }
            },
        )
    }
}

/**
 * The share button for [target]: it shares a short message with the content's link ([shareUrl]), or copies it where
 * there is no share sheet and shows a check mark for a moment. It belongs in a screen's top bar, not on cards or list
 * rows. It renders only when [visible], by default in layouts that [showsShareActions], and not at all for content
 * without a valid link. [backgroundColor] gives the icon a circular backdrop, as over artwork.
 */
@Composable
fun ShareButton(
    target: ShareTarget,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color.Transparent,
    visible: Boolean = showsShareActions(),
) {
    if (!visible) return
    val url = remember(target) { target.shareUrl() } ?: return
    val action = rememberShareAction(stringResource(target.shareMessageResource, target.name, url))

    var copied by remember(action) { mutableStateOf(false) }
    LaunchedEffect(copied) {
        if (copied) {
            delay(COPIED_FEEDBACK_MILLIS)
            copied = false
        }
    }

    val description =
        when {
            copied -> stringResource(Res.string.share_link_copied)
            action.usesShareSheet -> stringResource(Res.string.share_content_description, target.name)
            else -> stringResource(Res.string.share_copy_link_description, target.name)
        }
    val hasBackdrop = backgroundColor != Color.Transparent
    val backdrop = if (hasBackdrop) Modifier.background(backgroundColor, CircleShape) else Modifier
    IconButton(
        onClick = { copied = action.share() == ShareResult.Copied },
        modifier =
            modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .then(backdrop)
                // Screen readers announce the change to "Link copied".
                .semantics { liveRegion = LiveRegionMode.Polite },
    ) {
        Icon(
            imageVector = if (copied) Check else Share,
            contentDescription = description,
            tint = white,
            // Both icons at the share icon's size, so the button does not change when it shows the check mark.
            modifier = Modifier.size(Share.defaultWidth),
        )
    }
}

private val ShareTarget.shareMessageResource: StringResource
    get() =
        when (this) {
            is ShareTarget.Game -> Res.string.share_game_message
            is ShareTarget.Event -> Res.string.share_event_message
            is ShareTarget.Giveaway -> Res.string.share_giveaway_message
            is ShareTarget.CompilationList -> Res.string.share_compilation_message
        }

@Preview
@Composable
internal fun ShareButtonPreview() {
    BraindanceTheme {
        Row(modifier = Modifier.background(background)) {
            ShareButton(target = ShareTarget.Game(id = 1, name = "Sample game"), visible = true)
            ShareButton(
                target = ShareTarget.CompilationList(Compilation.Popular, name = "Popular Right Now"),
                backgroundColor = overlayBlackWith70Alpha,
                visible = true,
            )
        }
    }
}
