// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.desktop

import Bell
import X
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.navigation.Navigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.discover_search_clear
import dev.vladleesi.braindanceapp.resources.discover_search_placeholder
import dev.vladleesi.braindanceapp.resources.home_notifications_coming_soon
import dev.vladleesi.braindanceapp.system.SearchShortcuts
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ProfileButton
import dev.vladleesi.braindanceapp.ui.components.SearchBar
import dev.vladleesi.braindanceapp.ui.components.SearchFieldIcon
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.components.search.SearchQueryState
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.divider
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import org.jetbrains.compose.resources.stringResource

/** The height of [DesktopHeader], compact for a bar that is on every screen. The sidebar's brand row matches it. */
internal val DesktopHeaderHeight = 52.dp

// The search field of a compact header, a little lower than Discover's own field.
private val HeaderSearchHeight = 40.dp

// The key that focuses the search field, shown in the empty field as a hint.
private const val SEARCH_SHORTCUT_HINT = "/"

/**
 * The header of the Web desktop layout, above every screen and beside the sidebar or rail. The game search field
 * starts where the page column starts, at most Material's search bar width, and notifications and the account button
 * end where it ends. The header is chrome, so it has the darker navigation color. Each screen keeps its own title
 * and actions below it.
 *
 * The field shows [searchText]. Typing calls [onSearchTextChange], Enter [onSearch], the clear button
 * [onClearSearch], and Escape [onLeaveSearch]. [searchFocusRequester] moves the focus into the field, as the "/"
 * shortcut, whose hint the empty field shows, does. [isProfileSelected] marks the account button while Profile is open.
 */
@Suppress("LongParameterList")
@Composable
fun DesktopHeader(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onLeaveSearch: () -> Unit,
    onProfileClick: () -> Unit,
    isProfileSelected: Boolean,
    modifier: Modifier = Modifier,
    searchFocusRequester: FocusRequester = remember { FocusRequester() },
) {
    val gutter = pageGutter()
    val edge = divider
    var searchFocused by remember { mutableStateOf(false) }
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .height(DesktopHeaderHeight)
                .background(navBar)
                .drawBehind {
                    val stroke = Dimens.one.toPx()
                    val y = size.height - stroke / 2
                    drawLine(edge, Offset(0f, y), Offset(size.width, y), stroke)
                },
    ) {
        Row(
            modifier =
                Modifier
                    .pageWidth(gutter)
                    .fillMaxHeight()
                    // The last icon button's own inset completes the gutter, as in the screens' top bars.
                    .padding(start = gutter, end = gutter - Dimens.compact),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                SearchBar(
                    query = searchText,
                    onQueryChange = onSearchTextChange,
                    onSearch = { onSearch() },
                    placeholder = stringResource(Res.string.discover_search_placeholder),
                    fieldHeight = HeaderSearchHeight,
                    modifier =
                        Modifier
                            .widthIn(max = Dimens.readableTextWidth)
                            .focusRequester(searchFocusRequester)
                            .onFocusChanged { state -> searchFocused = state.isFocused }
                            .onPreviewKeyEvent { event ->
                                val leaves = event.key == Key.Escape && event.type == KeyEventType.KeyDown
                                if (leaves) onLeaveSearch()
                                leaves
                            },
                    trailingIcon = {
                        when {
                            searchText.isNotEmpty() ->
                                SearchFieldIcon(
                                    imageVector = X,
                                    label = stringResource(Res.string.discover_search_clear),
                                    onClick = onClearSearch,
                                )
                            !searchFocused -> ShortcutHint()
                        }
                    },
                )
            }
            ComingSoonAction(
                imageVector = Bell,
                label = stringResource(Res.string.home_notifications_coming_soon),
            )
            ProfileButton(onClick = onProfileClick, selected = isProfileSelected)
        }
    }
}

/**
 * The "/" key cap in the empty field. The dark theme outlines it; the light theme shows the bare "/" in the field's
 * darkest readable gray, since the field's own gray leaves no outline that shows. Screen readers skip it, as it
 * repeats what the keyboard already offers.
 */
@Composable
private fun ShortcutHint() {
    val outline =
        if (MaterialTheme.colors.isLight) Modifier else Modifier.border(Dimens.one, secondary, CornerShapes.small)
    Text(
        text = SEARCH_SHORTCUT_HINT,
        style = MaterialTheme.typography.caption,
        color = secondaryText,
        modifier =
            Modifier
                .padding(end = Dimens.compact)
                .then(outline)
                .padding(horizontal = Dimens.small, vertical = Dimens.micro)
                .clearAndSetSemantics {},
    )
}

/**
 * [DesktopHeader] for the current navigation state. Its field and Discover share [search]: it shows the search while
 * Discover's own screen, which lists the results, is open, and typing anywhere else opens that screen with the new
 * search. Search actions elsewhere in the app ([Navigator.openSearch]) and the "/" and Ctrl/⌘+K shortcuts focus it.
 */
@Composable
internal fun NavigationHeader(
    navigator: Navigator,
    search: SearchQueryState,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val discover = BottomBarRoute.DiscoverRoute
    val onDiscover =
        navigator.state.topLevelRoute == discover && navigator.state.backStacks[discover]?.singleOrNull() == discover
    val currentStack = navigator.state.backStacks[navigator.state.topLevelRoute]
    val isSearchFocusRequested by navigator.isSearchFocusRequested.collectAsStateWithLifecycle()
    LaunchedEffect(isSearchFocusRequested) {
        if (isSearchFocusRequested) {
            focusRequester.requestFocus()
            navigator.onSearchFocused()
        }
    }
    SearchShortcuts(onShortcut = { focusRequester.requestFocus() })
    DesktopHeader(
        searchText = if (onDiscover) search.text else "",
        onSearchTextChange = { text ->
            search.text = text
            // The field keeps the focus while Discover opens, so the request only confirms it.
            if (!onDiscover) navigator.openSearch()
        },
        onSearch = { if (onDiscover && search.text.isNotBlank()) search.submit() },
        onClearSearch = {
            search.text = ""
            focusRequester.requestFocus()
        },
        onLeaveSearch = {
            if (onDiscover) search.text = ""
            focusManager.clearFocus()
        },
        onProfileClick = { navigator.navigate(ProfileRoute) },
        isProfileSelected = currentStack?.lastOrNull() == ProfileRoute,
        modifier = modifier,
        searchFocusRequester = focusRequester,
    )
}

@Preview(widthDp = 1200)
@Composable
internal fun DesktopHeaderPreview() {
    BraindanceTheme {
        DesktopHeader(
            searchText = "",
            onSearchTextChange = {},
            onSearch = {},
            onClearSearch = {},
            onLeaveSearch = {},
            onProfileClick = {},
            isProfileSelected = false,
        )
    }
}
