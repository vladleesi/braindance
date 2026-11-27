// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import PanelLeftClose
import PanelLeftOpen
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routeLink
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.app_name
import dev.vladleesi.braindanceapp.resources.sidebar_collapse
import dev.vladleesi.braindanceapp.resources.sidebar_expand
import dev.vladleesi.braindanceapp.ui.components.FooterLinks
import dev.vladleesi.braindanceapp.ui.components.PreviewProjectLinks
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.components.selectedLine
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.icons.BraindanceMark
import dev.vladleesi.braindanceapp.ui.style.icons.BraindanceWordmark
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

/** The width of the expanded [DesktopSidebar]. The navigation scaffold insets its content by it. */
val DesktopSidebarWidth = 232.dp

/** The width of the collapsed [DesktopSidebar]: Material's navigation rail width. */
val DesktopSidebarCollapsedWidth = 80.dp

private val ItemMinHeight = 44.dp
private val ItemIconSize = 20.dp
private val IndicatorWidth = 3.dp
private val IndicatorHeight = 20.dp
private val AccentSegmentWidth = 10.dp
private val AccentColors = listOf(neonCyan, hotMagenta, signalYellow)

// A collapsed item's icon sits in a pill like the navigation rail's on tablets.
private val CollapsedIndicatorWidth = 56.dp
private val CollapsedIndicatorHeight = 32.dp

// 26 dp tall is 160 dp wide: above the wordmark's 120 dp minimum, and it fits the sidebar's content width.
private val BrandHeight = 26.dp

// The small mark the brand kit asks for at 48 px and under, with clear space around it in the brand row.
private val MarkSize = 32.dp

// How long the two layouts take to cross-fade while the scaffold animates the sidebar's width.
private const val LAYOUT_FADE_MILLIS = 200

/**
 * The desktop navigation sidebar: the brand, the top-level destinations, and at the bottom the [links] a site's footer
 * would hold (legal pages, support, and the data credits). Profile is the header's account button. While Profile is
 * open, [isProfileSelected] leaves every destination unselected. It renders the existing routes and navigation state,
 * and [onRouteClick] performs the navigation.
 *
 * [collapsed] narrows it to a rail of icons with short labels under the small mark, without the links, which Profile
 * also lists. [onToggleCollapsed], when given, shows the button that switches between the two, in the bottom corner
 * of either, so it never moves. Without it, the sidebar stays as it is, as in windows too narrow for the expanded one.
 * [width] is the sidebar's current width, which the scaffold animates between the two. The layouts cross-fade inside
 * it, each at its own width, so nothing reflows while it moves. The brand row is as tall as the desktop header, so the
 * logo and the search field share a line.
 */
@Suppress("LongParameterList")
@Composable
fun DesktopSidebar(
    selectedRoute: NavKey,
    isProfileSelected: Boolean,
    onRouteClick: (BottomBarRoute) -> Unit,
    links: ProjectLinks,
    modifier: Modifier = Modifier,
    collapsed: Boolean = false,
    onToggleCollapsed: (() -> Unit)? = null,
    width: Dp = if (collapsed) DesktopSidebarCollapsedWidth else DesktopSidebarWidth,
) {
    // The toggle's row, as tall as the header's, keeps the bottom of either layout free.
    val bottomSpace = if (onToggleCollapsed != null) DesktopHeaderHeight else Dimens.medium
    val destinations: @Composable (collapsed: Boolean) -> Unit = { compact ->
        BottomBarRoute.routes.forEach { route ->
            // The profile opens on top of the current tab, so only one destination is highlighted at a time.
            val selected = route == selectedRoute && !isProfileSelected
            val icon: @Composable (Color) -> Unit = { contentColor ->
                Icon(
                    imageVector = route.imageVector,
                    contentDescription = null,
                    tint = contentColor,
                    modifier = Modifier.size(ItemIconSize),
                )
            }
            val label = stringResource(route.label)
            val itemModifier = Modifier.routeLink(route)
            if (compact) {
                CollapsedSidebarItem(label, selected, { onRouteClick(route) }, itemModifier, icon)
            } else {
                SidebarItem(label, selected, { onRouteClick(route) }, itemModifier, icon)
            }
        }
    }
    Box(
        modifier =
            modifier
                .width(width)
                .fillMaxHeight()
                .background(navBar)
                .clipToBounds(),
    ) {
        Crossfade(
            targetState = collapsed,
            animationSpec = tween(LAYOUT_FADE_MILLIS),
            label = "Sidebar layout",
        ) { rail ->
            Column(
                modifier =
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .width(if (rail) DesktopSidebarCollapsedWidth else DesktopSidebarWidth)
                        .fillMaxHeight()
                        .padding(start = edgePadding(rail), end = edgePadding(rail), bottom = bottomSpace),
                horizontalAlignment = if (rail) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                SidebarBrand(collapsed = rail)
                // Scrolls on its own in short windows or with enlarged text, so every destination stays reachable.
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .padding(top = Dimens.small)
                            .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(if (rail) Dimens.small else Dimens.tiny),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    destinations(rail)
                }
                if (!rail) {
                    // Under the destinations, as YouTube's sidebar does, instead of a footer bar that would take height
                    // from every page. The links' own inset lines their text up with the destinations' icons.
                    FooterLinks(links = links, modifier = Modifier.padding(top = Dimens.small))
                }
            }
        }
        onToggleCollapsed?.let { onToggle ->
            // One button outside the cross-fade, so it keeps the keyboard focus while the sidebar changes.
            Box(
                modifier =
                    Modifier
                        .align(Alignment.BottomStart)
                        .height(DesktopHeaderHeight)
                        .padding(start = Dimens.medium),
                contentAlignment = Alignment.CenterStart,
            ) {
                SidebarToggle(collapsed = collapsed, onClick = onToggle)
            }
        }
    }
}

/** The wordmark, or the small mark in the collapsed sidebar, in a row as tall as the desktop header. */
@Composable
private fun SidebarBrand(collapsed: Boolean) {
    // The real wordmark with its trail, as in the landing site's header. The name is never retyped.
    Box(modifier = Modifier.height(DesktopHeaderHeight), contentAlignment = Alignment.CenterStart) {
        Image(
            imageVector = if (collapsed) BraindanceMark else BraindanceWordmark,
            contentDescription = stringResource(Res.string.app_name),
            modifier =
                if (collapsed) {
                    Modifier.size(MarkSize)
                } else {
                    Modifier.padding(start = Dimens.compact).height(BrandHeight)
                },
        )
    }
}

private fun edgePadding(collapsed: Boolean) = if (collapsed) Dimens.small else Dimens.medium

/** The button that collapses or expands the sidebar, named for what it does. */
@Composable
private fun SidebarToggle(
    collapsed: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier.pointerHoverIcon(PointerIcon.Hand)) {
        Icon(
            imageVector = if (collapsed) PanelLeftOpen else PanelLeftClose,
            contentDescription =
                stringResource(if (collapsed) Res.string.sidebar_expand else Res.string.sidebar_collapse),
            tint = secondaryText,
            modifier = Modifier.size(ItemIconSize),
        )
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
                        .background(color, CornerShapes.pill),
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
    val contentColor = if (selected) foreground else secondaryText
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
                    .background(if (selected) accent else Color.Transparent, CornerShapes.pill),
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

/**
 * One destination in the collapsed sidebar, as the navigation rail shows it: the icon in a pill, which the selected
 * destination fills, over its short label with the cyan line under it. The whole item is the target.
 */
@Composable
private fun CollapsedSidebarItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: @Composable (contentColor: Color) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val contentColor = if (selected) foreground else secondaryText
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .clip(CornerShapes.small)
                .focusRing(interactionSource.isFocusVisible(), CornerShapes.small)
                .pointerHoverIcon(PointerIcon.Hand)
                .selectable(
                    selected = selected,
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    role = Role.Tab,
                    onClick = onClick,
                ).padding(vertical = Dimens.tiny),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
    ) {
        Box(
            modifier =
                Modifier
                    .size(width = CollapsedIndicatorWidth, height = CollapsedIndicatorHeight)
                    .background(if (selected) secondary else Color.Transparent, CornerShapes.pill),
            contentAlignment = Alignment.Center,
        ) {
            icon(contentColor)
        }
        Text(
            text = label,
            style = MaterialTheme.typography.caption,
            color = contentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = if (selected) Modifier.selectedLine(accent) else Modifier,
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
            links = PreviewProjectLinks,
            onToggleCollapsed = {},
        )
    }
}

@Preview
@Composable
internal fun CollapsedDesktopSidebarPreview() {
    BraindanceTheme {
        DesktopSidebar(
            selectedRoute = BottomBarRoute.DiscoverRoute,
            isProfileSelected = false,
            onRouteClick = {},
            links = PreviewProjectLinks,
            collapsed = true,
            onToggleCollapsed = {},
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
