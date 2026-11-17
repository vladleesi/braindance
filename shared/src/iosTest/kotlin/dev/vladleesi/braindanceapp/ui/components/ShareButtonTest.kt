// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTouchHeightIsEqualTo
import androidx.compose.ui.test.assertTouchWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.screens.CompilationContent
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.viewmodels.PagedListState
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalTestApi::class)
class ShareButtonTest {
    @Test
    fun aFullListSharesItsLinkFromTheTopBar() =
        runComposeUiTest {
            val shareSheet = RecordingShareSheet()
            setContent {
                Providers(shareSheet = shareSheet) {
                    CompilationContent<GameCardModel>(
                        title = "Popular Right Now",
                        state = PagedListState.Empty,
                        onBack = {},
                        onLoadMore = {},
                        onRetry = {},
                        itemKey = GameCardModel::id,
                        cardWidth = PagedGridCardWidth.Games,
                        skeleton = {},
                        shareTarget = ShareTarget.CompilationList(Compilation.Popular, "Popular Right Now"),
                    ) {}
                }
            }

            onNodeWithContentDescription("Share Popular Right Now")
                // The 20 dp icon gets the full 48 dp touch target.
                .assertTouchWidthIsEqualTo(MIN_TOUCH_TARGET)
                .assertTouchHeightIsEqualTo(MIN_TOUCH_TARGET)
                .performClick()

            assertEquals(
                listOf("Popular Right Now on Braindance: https://app.braindancegg.com/popular"),
                shareSheet.shared,
            )
        }

    @Test
    fun aGiveawaySharesItsBraindancePage() =
        runComposeUiTest {
            val shareSheet = RecordingShareSheet()
            setContent {
                Providers(shareSheet = shareSheet) {
                    ShareButton(target = ShareTarget.Giveaway(7, "Sample giveaway"), visible = true)
                }
            }

            onNodeWithContentDescription("Share Sample giveaway").performClick()

            assertEquals(
                listOf(
                    "Free to claim: Sample giveaway. Details on Braindance: https://app.braindancegg.com/giveaway/7",
                ),
                shareSheet.shared,
            )
        }

    @Test
    fun withoutAShareSheetTheButtonCopiesTheLinkAndSaysSo() =
        runComposeUiTest {
            val clipboard = RecordingClipboard()
            setContent {
                Providers(shareSheet = RecordingShareSheet(supported = false), clipboard = clipboard) {
                    ShareButton(target = ShareTarget.Game(42, "Sample game"), visible = true)
                }
            }

            onNodeWithContentDescription("Copy link to Sample game").performClick()

            assertEquals("Check out Sample game on Braindance: https://app.braindancegg.com/games/42", clipboard.text)
            onNodeWithContentDescription("Link copied").assertExists()
        }

    @Test
    fun aShareSheetThatFailsToOpenFallsBackToCopying() =
        runComposeUiTest {
            val clipboard = RecordingClipboard()
            val failingSheet = RecordingShareSheet(failure = IllegalStateException("No window to present from"))
            setContent {
                Providers(shareSheet = failingSheet, clipboard = clipboard) {
                    ShareButton(
                        target = ShareTarget.CompilationList(Compilation.Giveaways, "Latest Giveaways"),
                        visible = true,
                    )
                }
            }

            onNodeWithContentDescription("Share Latest Giveaways").performClick()

            assertEquals("Latest Giveaways on Braindance: https://app.braindancegg.com/giveaways", clipboard.text)
            onNodeWithContentDescription("Link copied").assertExists()
        }

    @Test
    fun layoutsThatHideSharingShowNoShareButton() =
        runComposeUiTest {
            setContent {
                Providers(shareSheet = RecordingShareSheet()) {
                    ShareButton(target = ShareTarget.Game(1, "Sample game"), visible = false)
                }
            }

            onNodeWithContentDescription("Share Sample game").assertDoesNotExist()
        }

    @Test
    fun contentWithoutAValidLinkHasNoShareButton() =
        runComposeUiTest {
            setContent {
                Providers(shareSheet = RecordingShareSheet()) {
                    ShareButton(target = ShareTarget.Giveaway(0, "Broken giveaway"), visible = true)
                }
            }

            onNodeWithContentDescription("Share Broken giveaway").assertDoesNotExist()
        }

    @Suppress("DEPRECATION") // The share action copies through the deprecated ClipboardManager.
    @Composable
    private fun Providers(
        shareSheet: ShareSheet,
        clipboard: ClipboardManager = RecordingClipboard(),
        content: @Composable () -> Unit,
    ) {
        CompositionLocalProvider(LocalShareSheet provides shareSheet, LocalClipboardManager provides clipboard) {
            BraindanceTheme(content = content)
        }
    }

    private class RecordingShareSheet(
        private val supported: Boolean = true,
        private val failure: Throwable? = null,
    ) : ShareSheet {
        val shared = mutableListOf<String>()

        override fun isSupported(): Boolean = supported

        override fun share(text: String) {
            failure?.let { throw it }
            shared += text
        }
    }

    @Suppress("DEPRECATION")
    private class RecordingClipboard : ClipboardManager {
        var text: String? = null

        override fun getText(): AnnotatedString? = text?.let(::AnnotatedString)

        override fun setText(annotatedString: AnnotatedString) {
            text = annotatedString.text
        }
    }

    private companion object {
        val MIN_TOUCH_TARGET = 48.dp
    }
}
