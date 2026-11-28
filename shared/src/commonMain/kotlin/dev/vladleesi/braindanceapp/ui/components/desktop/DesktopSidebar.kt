// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import PanelLeftClose
import PanelLeftOpen
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import dev.vladleesi.braindanceapp.resources.sidebar_collapse_short
import dev.vladleesi.braindanceapp.resources.sidebar_expand
import dev.vladleesi.braindanceapp.resources.sidebar_help
import dev.vladleesi.braindanceapp.resources.sidebar_theme
import dev.vladleesi.braindanceapp.system.prefersReducedMotion
import dev.vladleesi.braindanceapp.ui.components.FullWindowOverlay
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
import dev.vladleesi.braindanceapp.ui.style.icons.SidebarHelp
import dev.vladleesi.braindanceapp.ui.style.icons.SidebarMoon
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.stringResource

/** The width of the expanded [DesktopSidebar]. The navigation scaffold insets its content by it. */
val DesktopSidebarWidth = 232.dp

/** The width of the collapsed [DesktopSidebar]: Material's navigation rail width. */
val DesktopSidebarCollapsedWidth = 80.dp

private val ItemMinHeight = 44.dp
private val ItemIconSize = 20.dp

// The rail's content width, in which each bottom control is centered.
private val SidebarControlSlotWidth = 48.dp
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

// The theme and help buttons, which the rail stacks above its toggle.
private const val UTILITY_BUTTON_COUNT = 2

/**
 * The desktop navigation sidebar: the brand, the top-level destinations, and at the bottom a help button that opens
 * the [links] a site's footer would hold (legal pages, support, and the data credits). Profile is the header's account
 * button. While Profile is open, [isProfileSelected] leaves every destination unselected. It renders the existing
 * routes and navigation state, and [onRouteClick] performs the navigation.
 *
 * [collapsed] narrows it to a rail of icons with short labels under the small mark. [onToggleCollapsed], when given,
 * shows the button that switches between the two, in the bottom corner of either, so it never moves. Without it, the
 * sidebar stays as it is, as in windows too narrow for the expanded one.
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
            animationSpec = if (prefersReducedMotion()) snap() else tween(LAYOUT_FADE_MILLIS),
            label = "Sidebar layout",
        ) { rail ->
            // The controls take a header's height for their first row and one more item height for each further row.
            val rows = controlRows(rail = rail, hasToggle = onToggleCollapsed != null)
            val controlsSpace = DesktopHeaderHeight + ItemMinHeight * (rows - 1)
            Column(
                modifier =
                    Modifier
                        .wrapContentWidth(Alignment.Start, unbounded = true)
                        .width(if (rail) DesktopSidebarCollapsedWidth else DesktopSidebarWidth)
                        .fillMaxHeight()
                        .padding(start = edgePadding(rail), end = edgePadding(rail), bottom = controlsSpace),
                horizontalAlignment = if (rail) Alignment.CenterHorizontally else Alignment.Start,
            ) {
                SidebarBrand(collapsed = rail)
                Column(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState())
                            .padding(top = Dimens.small),
                    verticalArrangement = Arrangement.spacedBy(if (rail) Dimens.small else Dimens.tiny),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    destinations(rail)
                }
            }
        }
        // The toggle keeps the same layout slot and keyboard focus while the destinations cross-fade.
        SidebarControls(
            collapsed = collapsed,
            width = width,
            onToggle = onToggleCollapsed,
            links = links,
            modifier = Modifier.align(Alignment.BottomStart),
        )
    }
}

/**
 * How many rows of [ItemMinHeight] the bottom controls take: one in the expanded sidebar, or the theme and help buttons
 * stacked above the toggle, when there is one, in the rail.
 */
private fun controlRows(
    rail: Boolean,
    hasToggle: Boolean,
): Int = if (rail) UTILITY_BUTTON_COUNT + (if (hasToggle) 1 else 0) else 1

/**
 * The sidebar's bottom controls, outside the cross-fade so they keep their place and the keyboard focus while the
 * layout changes. The expanded sidebar shows one row: a labeled collapse button, then theme and help. The rail stacks
 * theme and help above the toggle. While the width animates between the two, only the toggle shows. Help opens
 * [SidebarHelpPopup] above its button and returns the keyboard focus to it on close.
 */
@Composable
private fun SidebarControls(
    collapsed: Boolean,
    width: Dp,
    onToggle: (() -> Unit)?,
    links: ProjectLinks,
    modifier: Modifier = Modifier,
) {
    var helpOpen by remember { mutableStateOf(false) }
    val helpFocus = remember { FocusRequester() }
    val expanded = !collapsed && width >= DesktopSidebarWidth
    val stacked = collapsed && width <= DesktopSidebarCollapsedWidth
    val expandedBy = width - DesktopSidebarCollapsedWidth
    val expansion = (expandedBy / (DesktopSidebarWidth - DesktopSidebarCollapsedWidth)).coerceIn(0f, 1f)
    val verticalInset = (DesktopHeaderHeight - ItemMinHeight) / 2
    val controlInset = (SidebarControlSlotWidth - ItemMinHeight) / 2
    val slot = Modifier.padding(horizontal = controlInset)
    val utilitySlots: @Composable () -> Unit = {
        val control = if (stacked) slot else Modifier
        SidebarUtilityButton(
            icon = SidebarMoon,
            label = stringResource(Res.string.sidebar_theme),
            onClick = null,
            modifier = control,
        )
        SidebarUtilityButton(
            icon = SidebarHelp,
            label = stringResource(Res.string.sidebar_help),
            onClick = { helpOpen = true },
            modifier = control.focusRequester(helpFocus),
        )
    }
    Column(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.medium, vertical = verticalInset)
                .background(surface.copy(alpha = if (stacked) 1f else expansion), CornerShapes.large),
    ) {
        if (stacked) utilitySlots()
        if (onToggle != null || expanded) {
            Row(
                modifier = Modifier.fillMaxWidth().height(ItemMinHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val control = if (expanded) Modifier.weight(1f) else slot
                if (onToggle != null) {
                    val label = if (collapsed) Res.string.sidebar_expand else Res.string.sidebar_collapse
                    SidebarUtilityButton(
                        icon = if (collapsed) PanelLeftOpen else PanelLeftClose,
                        label = stringResource(label),
                        onClick = onToggle,
                        modifier = control,
                        expandedToggle = expanded,
                    )
                } else {
                    Spacer(control)
                }
                if (expanded) utilitySlots()
            }
        }
    }
    if (helpOpen && (stacked || expanded)) {
        val start = if (expanded) width - Dimens.medium - ItemMinHeight else Dimens.medium + controlInset
        // The popup opens just above the help button: in the rail, the toggle's row lies below it.
        val rowsThroughHelp = if (stacked && onToggle != null) 2 else 1
        val bottom = verticalInset + ItemMinHeight * rowsThroughHelp + Dimens.small
        val focusManager = LocalFocusManager.current
        val inputMode = LocalInputModeManager.current
        FullWindowOverlay {
            SidebarHelpPopup(links = links, start = start, bottom = bottom) {
                focusManager.clearFocus(force = true)
                helpOpen = false
                if (inputMode.inputMode == InputMode.Keyboard) helpFocus.requestFocus()
            }
        }
    }
}

/**
 * A 44 dp square button named by [label], disabled without [onClick]. As the [expandedToggle], it fills its row's width
 * and shows a short "Collapse" label beside the icon, which stays where the rail centers it.
 */
@Composable
private fun SidebarUtilityButton(
    icon: ImageVector,
    label: String,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    expandedToggle: Boolean = false,
) {
    Box(
        modifier =
            modifier
                .size(ItemMinHeight)
                .then(if (expandedToggle) Modifier.background(secondary, CornerShapes.large) else Modifier)
                .sidebarControl(onClick),
        contentAlignment = if (expandedToggle) Alignment.CenterStart else Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (onClick != null) secondaryText else secondaryText.copy(alpha = ContentAlpha.disabled),
            modifier =
                Modifier
                    // Keep the glyph centered in its rail slot as the toggle target grows wider.
                    .padding(start = if (expandedToggle) (SidebarControlSlotWidth - ItemIconSize) / 2 else 0.dp)
                    .size(ItemIconSize),
        )
        if (expandedToggle) {
            Text(
                text = stringResource(Res.string.sidebar_collapse_short),
                modifier =
                    Modifier
                        .padding(start = SidebarControlSlotWidth - Dimens.tiny, end = Dimens.tiny)
                        .clearAndSetSemantics {},
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * A sidebar button's target: the large shape, the theme ripple, the keyboard focus ring, and the hand pointer. Without
 * [onClick] the control is disabled, with neither the ring nor the hand.
 */
@Composable
internal fun Modifier.sidebarControl(onClick: (() -> Unit)?): Modifier {
    val shape = CornerShapes.large
    val interactionSource = if (onClick != null) remember { MutableInteractionSource() } else null
    val feedback =
        if (interactionSource != null) {
            Modifier.focusRing(interactionSource.isFocusVisible(), shape).pointerHoverIcon(PointerIcon.Hand)
        } else {
            Modifier
        }
    return clip(shape)
        .then(feedback)
        .clickable(
            enabled = onClick != null,
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick ?: {},
        )
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
internal fun SidebarControlsPreview() {
    BraindanceTheme {
        Box(Modifier.width(DesktopSidebarWidth).background(navBar)) {
            SidebarControls(
                collapsed = false,
                width = DesktopSidebarWidth,
                onToggle = {},
                links = PreviewProjectLinks,
            )
        }
    }
}

@Preview
@Composable
internal fun SidebarUtilityButtonPreview() {
    BraindanceTheme {
        Row(Modifier.background(surface)) {
            SidebarUtilityButton(icon = SidebarMoon, label = "Theme unavailable", onClick = null)
            SidebarUtilityButton(icon = SidebarHelp, label = "Help & links", onClick = {})
        }
    }
}

@Preview
@Composable
internal fun BrandAccentStripPreview() {
    BraindanceTheme {
        BrandAccentStrip(modifier = Modifier.background(navBar).padding(Dimens.medium))
    }
}
