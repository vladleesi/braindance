// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import Check
import PanelLeftClose
import PanelLeftOpen
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routeLink
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.app_name
import dev.vladleesi.braindanceapp.resources.mark_echo_on_ink
import dev.vladleesi.braindanceapp.resources.sidebar_collapse
import dev.vladleesi.braindanceapp.resources.sidebar_expand
import dev.vladleesi.braindanceapp.resources.sidebar_help
import dev.vladleesi.braindanceapp.resources.sidebar_theme_menu
import dev.vladleesi.braindanceapp.system.ThemeMode
import dev.vladleesi.braindanceapp.system.prefersReducedMotion
import dev.vladleesi.braindanceapp.ui.components.BrandWordmark
import dev.vladleesi.braindanceapp.ui.components.FullWindowOverlay
import dev.vladleesi.braindanceapp.ui.components.PreviewProjectLinks
import dev.vladleesi.braindanceapp.ui.components.ProjectLinks
import dev.vladleesi.braindanceapp.ui.components.THEME_SELECTION_MILLIS
import dev.vladleesi.braindanceapp.ui.components.ThemeSelector
import dev.vladleesi.braindanceapp.ui.components.icon
import dev.vladleesi.braindanceapp.ui.components.label
import dev.vladleesi.braindanceapp.ui.components.selectedLine
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.divider
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.icons.SidebarHelp
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.void
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** The width of the expanded [DesktopSidebar]. The navigation scaffold insets its content by it. */
val DesktopSidebarWidth = 232.dp

/** The width of the collapsed [DesktopSidebar]: Material's navigation rail width. */
val DesktopSidebarCollapsedWidth = 80.dp

private val ItemMinHeight = 44.dp
private val ItemIconSize = 20.dp

private val SidebarThemeSelectorWidth = 104.dp
private val IndicatorWidth = 3.dp
private val IndicatorHeight = 20.dp
private val AccentSegmentWidth = 10.dp
private val StripPlatePadding = Dimens.tiny

// The theme menu holds three short choices, so it is far narrower and its rows lower than the help menu's.
private val ThemePopupWidth = 144.dp
private val ThemeRowHeight = 36.dp
private val AccentColors = listOf(neonCyan, hotMagenta, signalYellow)

// A collapsed item's icon sits in a pill like the navigation rail's on tablets.
private val CollapsedIndicatorWidth = 56.dp
private val CollapsedIndicatorHeight = 32.dp

// 26 dp tall is 160 dp wide: above the wordmark's 120 dp minimum, and it fits the sidebar's content width.
private val BrandHeight = 26.dp

// The kit's `mark-echo-on-ink.png`: its Ink tile holds the mark, so it reads the same on the dark and light chrome.
private val MarkSize = 40.dp

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
 * shows the button that switches between the two in either footer. Narrow windows can use a fixed rail.
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
    themeMode: ThemeMode,
    onChooseTheme: (ThemeMode) -> Unit,
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
    val edge = divider
    Box(
        modifier =
            modifier
                .width(width)
                .fillMaxHeight()
                .background(navBar)
                .drawBehind {
                    val stroke = Dimens.one.toPx()
                    val x = if (layoutDirection == LayoutDirection.Ltr) size.width - stroke / 2 else stroke / 2
                    drawLine(edge, Offset(x, 0f), Offset(x, size.height), stroke)
                }.clipToBounds(),
    ) {
        Crossfade(
            targetState = collapsed,
            animationSpec = if (prefersReducedMotion()) snap() else tween(LAYOUT_FADE_MILLIS),
            label = "Sidebar layout",
        ) { rail ->
            val rows = controlRows(rail = rail, hasToggle = onToggleCollapsed != null)
            val controlsSpace =
                Dimens.one + Dimens.small + ItemMinHeight * rows + Dimens.tiny * (rows - 1) + Dimens.medium
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
            themeMode = themeMode,
            onChooseTheme = onChooseTheme,
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

/** The collapse button keeps its focus while the destination layouts cross-fade. */
@Suppress("LongParameterList")
@Composable
private fun SidebarControls(
    collapsed: Boolean,
    width: Dp,
    onToggle: (() -> Unit)?,
    themeMode: ThemeMode,
    onChooseTheme: (ThemeMode) -> Unit,
    links: ProjectLinks,
    modifier: Modifier = Modifier,
) {
    var helpOpen by remember { mutableStateOf(false) }
    val helpFocus = remember { FocusRequester() }
    val expanded = !collapsed && width >= DesktopSidebarWidth
    val stacked = collapsed && width <= DesktopSidebarCollapsedWidth
    Box(modifier.fillMaxWidth().padding(horizontal = Dimens.medium, vertical = Dimens.medium)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.fillMaxWidth().height(Dimens.one).background(divider))
            Spacer(Modifier.height(Dimens.small))
            val help: @Composable () -> Unit = {
                SidebarUtilityButton(
                    icon = SidebarHelp,
                    label = stringResource(Res.string.sidebar_help),
                    onClick = { helpOpen = true },
                    modifier = Modifier.focusRequester(helpFocus),
                )
            }
            if (stacked) {
                val start = (width - ItemMinHeight) / 2
                val rows = controlRows(rail = true, hasToggle = onToggle != null)
                val bottom = Dimens.medium + (ItemMinHeight + Dimens.tiny) * rows
                SidebarThemeMenu(themeMode, onChooseTheme, start = start, bottom = bottom)
                Spacer(Modifier.height(Dimens.tiny))
                help()
                if (onToggle != null) Spacer(Modifier.height(ItemMinHeight + Dimens.tiny))
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().height(ItemMinHeight),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.size(ItemMinHeight))
                    if (expanded) {
                        ThemeSelector(themeMode, onChooseTheme, Modifier.width(SidebarThemeSelectorWidth))
                        help()
                    }
                }
            }
        }
        if (onToggle != null) {
            SidebarUtilityButton(
                icon = if (collapsed) PanelLeftOpen else PanelLeftClose,
                label = stringResource(if (collapsed) Res.string.sidebar_expand else Res.string.sidebar_collapse),
                onClick = onToggle,
                modifier = Modifier.align(Alignment.BottomStart).padding(start = Dimens.micro),
            )
        }
    }
    if (helpOpen && (stacked || expanded)) {
        val start = if (expanded) width - Dimens.medium - ItemMinHeight else (width - ItemMinHeight) / 2
        val toggleSpace = if (stacked && onToggle != null) ItemMinHeight + Dimens.tiny else 0.dp
        val bottom = Dimens.medium + ItemMinHeight + toggleSpace + Dimens.small
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

@Composable
private fun SidebarThemeMenu(
    mode: ThemeMode,
    onChoose: (ThemeMode) -> Unit,
    start: Dp,
    bottom: Dp,
) {
    var open by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val inputMode = LocalInputModeManager.current
    val label = stringResource(Res.string.sidebar_theme_menu, stringResource(mode.label))
    val close = {
        focusManager.clearFocus(force = true)
        open = false
        if (inputMode.inputMode == InputMode.Keyboard) focus.requestFocus()
    }
    Box(
        modifier =
            Modifier
                .size(ItemMinHeight)
                .background(if (MaterialTheme.colors.isLight) secondary else surface, CornerShapes.large)
                .border(Dimens.one, secondaryVariant, CornerShapes.large)
                .focusRequester(focus)
                .semantics { contentDescription = label }
                .sidebarControl { open = true },
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = mode,
            animationSpec = if (prefersReducedMotion()) snap() else tween(THEME_SELECTION_MILLIS),
            label = "Theme icon",
        ) { choice ->
            Icon(choice.icon, contentDescription = null, tint = foreground, modifier = Modifier.size(ItemIconSize))
        }
    }
    if (open) {
        FullWindowOverlay {
            SidebarPopup(start = start, bottom = bottom, onClose = close, width = ThemePopupWidth) {
                SidebarThemeOptions(mode, label, onChoose, close)
            }
        }
    }
}

@Composable
private fun SidebarThemeOptions(
    mode: ThemeMode,
    label: String,
    onChoose: (ThemeMode) -> Unit,
    onClose: () -> Unit,
) {
    val selectedFocus = remember { FocusRequester() }
    LaunchedEffect(Unit) { selectedFocus.requestFocus() }
    Column(
        modifier =
            Modifier
                .sidebarMenuFocus(label, onClose)
                .padding(Dimens.tiny)
                .verticalScroll(rememberScrollState()),
    ) {
        ThemeMode.entries.forEach { choice ->
            val focusModifier = if (choice == mode) Modifier.focusRequester(selectedFocus) else Modifier
            Row(
                modifier =
                    focusModifier
                        .fillMaxWidth()
                        .heightIn(min = ThemeRowHeight)
                        .semantics { selected = choice == mode }
                        .sidebarControl {
                            onChoose(choice)
                            onClose()
                        }.padding(horizontal = Dimens.small, vertical = Dimens.tiny),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = choice.icon,
                    contentDescription = null,
                    modifier = Modifier.size(Dimens.medium),
                    tint = secondaryText,
                )
                Text(
                    text = stringResource(choice.label),
                    style = MaterialTheme.typography.subtitle1,
                    color = foreground,
                    modifier = Modifier.weight(1f).padding(horizontal = Dimens.small),
                )
                if (choice == mode) {
                    Icon(Check, contentDescription = null, modifier = Modifier.size(Dimens.compact), tint = foreground)
                }
            }
        }
    }
}

@Composable
private fun SidebarUtilityButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.size(ItemMinHeight).sidebarControl(onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = label, tint = secondaryText, modifier = Modifier.size(ItemIconSize))
    }
}

/** A sidebar button's target, with the theme ripple, keyboard focus ring, and hand pointer. */
@Composable
internal fun Modifier.sidebarControl(onClick: () -> Unit): Modifier {
    val shape = CornerShapes.large
    val interactionSource = remember { MutableInteractionSource() }
    return clip(shape)
        .focusRing(interactionSource.isFocusVisible(), shape)
        .pointerHoverIcon(PointerIcon.Hand)
        .clickable(
            interactionSource = interactionSource,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick,
        )
}

/** The wordmark or small mark, in a row as tall as the desktop header. */
@Composable
private fun SidebarBrand(collapsed: Boolean) {
    Box(modifier = Modifier.height(DesktopHeaderHeight), contentAlignment = Alignment.CenterStart) {
        if (collapsed) {
            Image(
                painter = painterResource(Res.drawable.mark_echo_on_ink),
                contentDescription = stringResource(Res.string.app_name),
                modifier = Modifier.size(MarkSize).clip(CornerShapes.small),
            )
        } else {
            BrandWordmark(BrandHeight, Modifier.padding(start = Dimens.compact))
        }
    }
}

private fun edgePadding(collapsed: Boolean) = if (collapsed) Dimens.small else Dimens.medium

/**
 * Three short bars in the brand colors: a small accent for headings, used sparingly. On a light surface they sit on an
 * ink plate, which the layout ignores, so the heading below stays where it is in either theme.
 */
@Composable
fun BrandAccentStrip(modifier: Modifier = Modifier) {
    val plate =
        if (MaterialTheme.colors.isLight) {
            Modifier
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    val overhang = StripPlatePadding.roundToPx()
                    layout(placeable.width, placeable.height - 2 * overhang) { placeable.place(0, -overhang) }
                }.background(void, CornerShapes.pill)
                .padding(horizontal = Dimens.small, vertical = StripPlatePadding)
        } else {
            Modifier
        }
    Row(modifier = modifier.then(plate), horizontalArrangement = Arrangement.spacedBy(Dimens.micro)) {
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
 * One destination row. The selected row has a contrasting surface and a thin indicator. Hover and press use the
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
    val selectedFill = if (MaterialTheme.colors.isLight) secondaryVariant else secondary
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = ItemMinHeight)
                .clip(CornerShapes.small)
                .background(if (selected) selectedFill else Color.Transparent)
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
 * destination fills, over its short label with a line under it. The whole item is the target.
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
    val selectedFill = if (MaterialTheme.colors.isLight) secondaryVariant else secondary
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
                    .background(if (selected) selectedFill else Color.Transparent, CornerShapes.pill),
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
            themeMode = ThemeMode.System,
            onChooseTheme = {},
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
            themeMode = ThemeMode.System,
            onChooseTheme = {},
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
                themeMode = ThemeMode.System,
                onChooseTheme = {},
                links = PreviewProjectLinks,
            )
        }
    }
}

@Preview
@Composable
internal fun SidebarThemeMenuPreview() {
    BraindanceTheme(darkTheme = false) {
        SidebarThemeMenu(ThemeMode.System, {}, start = Dimens.medium, bottom = Dimens.extraLarge)
    }
}

@Preview
@Composable
internal fun SidebarThemeOptionsPreview() {
    BraindanceTheme {
        SidebarPopup(start = Dimens.medium, bottom = Dimens.extraLarge, onClose = {}) {
            SidebarThemeOptions(ThemeMode.System, "Theme", {}, {})
        }
    }
}

@Preview
@Composable
internal fun SidebarUtilityButtonPreview() {
    BraindanceTheme {
        Row(Modifier.background(surface)) {
            SidebarUtilityButton(icon = PanelLeftClose, label = "Collapse sidebar", onClick = {})
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
