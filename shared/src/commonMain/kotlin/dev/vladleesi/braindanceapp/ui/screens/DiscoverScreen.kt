// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import ChevronLeft
import Search
import SlidersHorizontal
import X
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.discover_advanced_filters_coming_soon
import dev.vladleesi.braindanceapp.resources.discover_filter_genre
import dev.vladleesi.braindanceapp.resources.discover_filter_platform
import dev.vladleesi.braindanceapp.resources.discover_filter_release_date
import dev.vladleesi.braindanceapp.resources.discover_genres_description
import dev.vladleesi.braindanceapp.resources.discover_genres_title
import dev.vladleesi.braindanceapp.resources.discover_recommendations_description
import dev.vladleesi.braindanceapp.resources.discover_recommendations_title
import dev.vladleesi.braindanceapp.resources.discover_search_clear
import dev.vladleesi.braindanceapp.resources.discover_search_close
import dev.vladleesi.braindanceapp.resources.discover_search_placeholder
import dev.vladleesi.braindanceapp.resources.navigation_discover
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.PlaceholderChips
import dev.vladleesi.braindanceapp.ui.components.SearchBar
import dev.vladleesi.braindanceapp.ui.components.SearchFieldIcon
import dev.vladleesi.braindanceapp.ui.components.SearchFieldProgress
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondary
import org.jetbrains.compose.resources.stringResource

@Composable
fun DiscoverScreen(modifier: Modifier) {
    var query by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val navigator = LocalNavigator.current
    val scrollState = rememberScrollState()

    val searchTabReselected by navigator.resultsFlow(BottomBarRoute.DiscoverRoute.RESULT_KEY).collectAsState(null)

    LaunchedEffect(searchTabReselected) {
        if (searchTabReselected == true) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Search is in use while the field has focus or holds a query, including after a search was submitted.
    val isSearchActive = isFocused || query.isNotEmpty()
    val hideKeyboard: () -> Unit = {
        keyboardController?.hide()
        focusManager.clearFocus()
    }
    val leaveSearch: () -> Unit = {
        hideKeyboard()
        query = ""
        isLoading = false
    }

    // Back first closes the keyboard (the system does that), then leaves search, and only then leaves the screen.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isSearchActive,
        onBackCompleted = leaveSearch,
    )

    Column(modifier = modifier.fillMaxSize().background(background)) {
        SpacerStatusBarInsets()
        DiscoverTitle(
            // The sidebar layout has room to spare, and a hardware keyboard there takes none.
            visible = !isSearchActive || desktopLayout()?.hasSidebar == true,
            onProfileClick = { navigator.navigate(ProfileRoute) },
        )
        SearchHeader(scrollState = scrollState) {
            SearchBar(
                query = query,
                onQueryChange = { value -> query = value },
                onSearch = { submitted ->
                    if (submitted.isNotBlank()) {
                        hideKeyboard()
                        isLoading = true
                    }
                },
                placeholder = stringResource(Res.string.discover_search_placeholder),
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (isFocused.not()) {
                                navigator.setResult(BottomBarRoute.DiscoverRoute.RESULT_KEY, false)
                            }
                        }.onPreviewKeyEvent { event ->
                            // Escape leaves search on a hardware keyboard, as back and the chevron do.
                            val leaves = isFocused && event.key == Key.Escape && event.type == KeyEventType.KeyDown
                            if (leaves) leaveSearch()
                            leaves
                        },
                leadingIcon = { SearchLeadingIcon(isSearchActive = isSearchActive, onLeaveSearch = leaveSearch) },
                trailingIcon = {
                    when {
                        isLoading -> SearchFieldProgress()
                        query.isNotEmpty() ->
                            SearchFieldIcon(
                                imageVector = X,
                                label = stringResource(Res.string.discover_search_clear),
                                onClick = { query = "" },
                            )
                    }
                },
            )
            ComingSoonAction(
                imageVector = SlidersHorizontal,
                label = stringResource(Res.string.discover_advanced_filters_coming_soon),
            )
        }
        DiscoverPlaceholders(scrollState = scrollState, modifier = Modifier.weight(1f))
    }
}

/** The search icon at rest; while search is in use, a back chevron that leaves it. */
@Composable
private fun SearchLeadingIcon(
    isSearchActive: Boolean,
    onLeaveSearch: () -> Unit,
) {
    Crossfade(targetState = isSearchActive) { active ->
        if (active) {
            SearchFieldIcon(
                imageVector = ChevronLeft,
                label = stringResource(Res.string.discover_search_close),
                onClick = onLeaveSearch,
            )
        } else {
            SearchFieldIcon(imageVector = Search)
        }
    }
}

/**
 * The title and avatar. While search is in use they give their row to the field, which moves to the top
 * with more room for the content above the keyboard; a small gap then keeps the field clear of the status bar.
 */
@Composable
private fun DiscoverTitle(
    visible: Boolean,
    onProfileClick: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(expandFrom = Alignment.Top),
        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        TopLevelTopAppBar(
            title = stringResource(Res.string.navigation_discover),
            onProfileClick = onProfileClick,
        )
    }
    AnimatedVisibility(
        visible = !visible,
        enter = expandVertically(expandFrom = Alignment.Top),
        exit = shrinkVertically(shrinkTowards = Alignment.Top),
    ) {
        Spacer(Modifier.height(Dimens.small))
    }
}

/**
 * The row under the title that holds the search field and its actions. The space under it is where scrolled content
 * passes out of view, and a hairline marks that edge only while something is scrolled under it. The scroll position is
 * read while drawing, so scrolling does not recompose the header.
 */
@Composable
private fun SearchHeader(
    scrollState: ScrollState,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (scrollState.value > 0) {
                        val stroke = Dimens.one.toPx()
                        val y = size.height - stroke / 2
                        drawLine(secondary, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                }.padding(start = Dimens.medium, end = Dimens.tiny, bottom = Dimens.compact),
    ) {
        Row(
            modifier = Modifier.widthIn(max = Dimens.readableTextWidth),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

@Composable
private fun DiscoverPlaceholders(
    scrollState: ScrollState,
    modifier: Modifier,
) {
    Column(
        modifier =
            modifier
                .verticalScroll(scrollState)
                // While the keyboard is open, the content can still scroll clear of it.
                .imePadding()
                .padding(start = Dimens.medium, top = Dimens.tiny, end = Dimens.medium, bottom = Dimens.large),
        verticalArrangement = Arrangement.spacedBy(Dimens.medium),
    ) {
        PlaceholderChips(
            labels =
                listOf(
                    stringResource(Res.string.discover_filter_platform),
                    stringResource(Res.string.discover_filter_genre),
                    stringResource(Res.string.discover_filter_release_date),
                ),
        )
        ComingSoonCard(
            title = stringResource(Res.string.discover_recommendations_title),
            description = stringResource(Res.string.discover_recommendations_description),
        )
        ComingSoonCard(
            title = stringResource(Res.string.discover_genres_title),
            description = stringResource(Res.string.discover_genres_description),
        )
    }
}
