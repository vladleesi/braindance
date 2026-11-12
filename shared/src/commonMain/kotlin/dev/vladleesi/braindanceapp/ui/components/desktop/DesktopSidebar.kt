// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.app_name
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.ui.components.UserAvatar
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

/** The width of [DesktopSidebar]; the navigation scaffold insets its content by it. */
val DesktopSidebarWidth = 232.dp

private val ItemMinHeight = 44.dp
private val ItemIconSize = 20.dp
private val ProfileAvatarSize = 24.dp
private val IndicatorWidth = 3.dp
private val IndicatorHeight = 20.dp
private val AccentSegmentWidth = 10.dp
private val AccentColors = listOf(neonCyan, hotMagenta, signalYellow)

/**
 * The desktop navigation sidebar: the brand, the top-level destinations, and the profile entry at the bottom, which
 * leads to everything else (community links, credits, version). It renders the existing routes and navigation state;
 * [onRouteClick] and [onProfileClick] perform the navigation.
 */
@Composable
fun DesktopSidebar(
    selectedRoute: NavKey,
    isProfileSelected: Boolean,
    onRouteClick: (BottomBarRoute) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .width(DesktopSidebarWidth)
                .fillMaxHeight()
                .background(navBar)
                .padding(Dimens.medium),
    ) {
        SidebarBrand(modifier = Modifier.padding(horizontal = Dimens.compact, vertical = Dimens.medium))
        // Scrolls on its own in short windows or with enlarged text, so every destination stays reachable.
        Column(
            modifier =
                Modifier
                    .weight(1f)
                    .padding(top = Dimens.medium)
                    .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
        ) {
            BottomBarRoute.routes.forEach { route ->
                SidebarItem(
                    label = stringResource(route.label),
                    // The profile opens on top of the current tab, so only one row is highlighted at a time.
                    selected = route == selectedRoute && !isProfileSelected,
                    onClick = { onRouteClick(route) },
                ) { contentColor ->
                    Icon(
                        imageVector = route.imageVector,
                        contentDescription = null,
                        tint = contentColor,
                        modifier = Modifier.size(ItemIconSize),
                    )
                }
            }
        }
        SidebarItem(
            label = stringResource(Res.string.profile_screen_title),
            selected = isProfileSelected,
            onClick = onProfileClick,
            modifier = Modifier.padding(top = Dimens.small),
        ) {
            // A little larger than the icons, so the picture reads as a picture; the item keeps its icon-slot size.
            UserAvatar(size = ProfileAvatarSize, modifier = Modifier.requiredSize(ProfileAvatarSize))
        }
    }
}

@Composable
private fun SidebarBrand(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.tiny)) {
        Text(
            text = stringResource(Res.string.app_name),
            style = MaterialTheme.typography.h4,
            maxLines = 1,
        )
        BrandAccentStrip()
    }
}

/** Three short bars in the brand colors: a small accent for headings, used sparingly. */
@Composable
fun BrandAccentStrip(modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(Dimens.micro)) {
        AccentColors.forEach { color ->
            Box(
                modifier =
                    Modifier
                        .size(width = AccentSegmentWidth, height = Dimens.micro)
                        .background(color, CornerShapes.tiny),
            )
        }
    }
}

/**
 * One destination row. The selected row has a contrasting surface and a thin cyan indicator; hover and press use the
 * theme ripple, and keyboard focus draws an outline that a click does not leave behind.
 */
@Composable
private fun SidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (contentColor: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) white else secondaryText
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ItemMinHeight)
                .clip(CornerShapes.small)
                .background(if (selected) secondary else Color.Transparent)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small)
                .pointerHoverIcon(PointerIcon.Hand)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(end = Dimens.compact),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = IndicatorWidth, height = IndicatorHeight)
                    .background(if (selected) neonCyan else Color.Transparent, CornerShapes.tiny),
        )
        Box(
            modifier = Modifier.padding(start = Dimens.small).size(ItemIconSize),
            contentAlignment = Alignment.Center,
        ) {
            icon(contentColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.subtitle1,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = Dimens.compact),
        )
    }
}

@Preview
@Composable
internal fun DesktopSidebarPreview() {
    BraindanceTheme {
        DesktopSidebar(
            selectedRoute = BottomBarRoute.HomeRoute,
            isProfileSelected = false,
            onRouteClick = {},
            onProfileClick = {},
        )
    }
}

@Preview
@Composable
internal fun BrandAccentStripPreview() {
    BraindanceTheme {
        BrandAccentStrip(modifier = Modifier.background(navBar).padding(Dimens.medium))
    }
}
