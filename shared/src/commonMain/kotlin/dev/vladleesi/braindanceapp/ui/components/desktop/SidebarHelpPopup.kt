// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusManager
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.isTraversalGroup
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.externalLink
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.data_attribution_gamerpower
import dev.vladleesi.braindanceapp.resources.data_attribution_igdb
import dev.vladleesi.braindanceapp.resources.profile_contact_support
import dev.vladleesi.braindanceapp.resources.profile_licenses
import dev.vladleesi.braindanceapp.resources.profile_privacy_policy
import dev.vladleesi.braindanceapp.resources.sidebar_help
import dev.vladleesi.braindanceapp.resources.terms_of_use
import dev.vladleesi.braindanceapp.ui.components.GAMERPOWER_NAME
import dev.vladleesi.braindanceapp.ui.components.IGDB_NAME
import dev.vladleesi.braindanceapp.ui.components.PreviewProjectLinks
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.surface
import org.jetbrains.compose.resources.stringResource

private val HelpRowHeight = 36.dp
private val HelpMenuWidth = 232.dp

/**
 * The sidebar's help menu: support, the legal pages, and the data credits, each opening outside the app. It sits
 * [start] from the window's left edge and [bottom] above its bottom edge. A click outside it, Escape, or choosing a
 * link calls [onClose]. It takes the keyboard focus and keeps it inside until it closes, and the arrow keys move
 * between the links.
 */
@Composable
internal fun SidebarHelpPopup(
    links: ProjectLinks,
    start: Dp,
    bottom: Dp,
    onClose: () -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.matchParentSize().pointerInput(onClose) { detectTapGestures { onClose() } })
        Surface(
            modifier =
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = start, top = Dimens.medium, end = Dimens.medium, bottom = bottom)
                    .width(HelpMenuWidth),
            color = surface,
            shape = CornerShapes.large,
            border = BorderStroke(Dimens.one, secondary),
        ) {
            SidebarHelpLinks(links = links, onClose = onClose)
        }
    }
}

@Composable
private fun SidebarHelpLinks(
    links: ProjectLinks,
    onClose: () -> Unit,
) {
    val firstLink = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val title = stringResource(Res.string.sidebar_help)
    LaunchedEffect(Unit) { firstLink.requestFocus() }
    val entries =
        listOf(
            stringResource(Res.string.profile_contact_support) to links.emailSupport,
            stringResource(Res.string.profile_privacy_policy) to links.openPrivacyPolicy,
            stringResource(Res.string.terms_of_use) to links.openTerms,
            stringResource(Res.string.profile_licenses) to links.openLicenses,
            stringResource(Res.string.data_attribution_igdb, IGDB_NAME) to links.openIgdb,
            stringResource(Res.string.data_attribution_gamerpower, GAMERPOWER_NAME) to links.openGamerPower,
        )
    Column(
        modifier =
            Modifier
                .semantics {
                    paneTitle = title
                    isTraversalGroup = true
                }.onPreviewKeyEvent { event -> event.handleMenuKey(focusManager, onClose) }
                .focusProperties { onExit = { cancelFocusChange() } }
                .focusGroup()
                .padding(Dimens.tiny)
                .verticalScroll(rememberScrollState()),
    ) {
        entries.forEachIndexed { index, (label, open) ->
            val focusModifier = if (index == 0) Modifier.focusRequester(firstLink) else Modifier
            val select = {
                open()
                onClose()
            }
            Row(
                modifier =
                    focusModifier
                        .fillMaxWidth()
                        .heightIn(min = HelpRowHeight)
                        .externalLink(select)
                        .sidebarControl(select)
                        .padding(horizontal = Dimens.small, vertical = Dimens.tiny),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.caption,
                    color = foreground,
                    modifier = Modifier.weight(1f).padding(end = Dimens.small),
                )
                Icon(
                    imageVector = OpenInNew,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(Dimens.medium),
                )
            }
        }
    }
}

private fun KeyEvent.handleMenuKey(
    focusManager: FocusManager,
    onClose: () -> Unit,
): Boolean {
    if (type != KeyEventType.KeyDown) return false
    when (key) {
        Key.Escape -> onClose()
        Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Next)
        Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Previous)
        else -> return false
    }
    return true
}

@Preview(widthDp = 390, heightDp = 430)
@Composable
internal fun SidebarHelpPopupPreview() {
    BraindanceTheme {
        SidebarHelpPopup(links = PreviewProjectLinks, start = Dimens.medium, bottom = Dimens.large, onClose = {})
    }
}

@Preview(widthDp = 232)
@Composable
internal fun SidebarHelpLinksPreview() {
    BraindanceTheme {
        Surface(color = surface, shape = CornerShapes.large) {
            SidebarHelpLinks(links = PreviewProjectLinks, onClose = {})
        }
    }
}
