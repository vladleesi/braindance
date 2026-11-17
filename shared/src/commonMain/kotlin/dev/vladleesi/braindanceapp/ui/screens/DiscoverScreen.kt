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
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
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
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.components.search.SearchError
import dev.vladleesi.braindanceapp.ui.components.search.SearchNoResults
import dev.vladleesi.braindanceapp.ui.components.search.SearchResultsGrid
import dev.vladleesi.braindanceapp.ui.components.search.SearchResultsSkeleton
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.viewmodels.GameSearchViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SearchState
import dev.vladleesi.braindanceapp.utils.keyboardPadding
import dev.vladleesi.braindanceapp.utils.observePointerType
import kotlinx.coroutines.flow.filter
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Discover with game search. The field owns the typed text, which survives recreation; [viewModel] searches it and
 * holds the results. Without a query that can be searched, Discover shows its own content.
 */
@Composable
fun DiscoverScreen(
    modifier: Modifier,
    viewModel: GameSearchViewModel = koinViewModel(),
) {
    var query by rememberSaveable { mutableStateOf("") }
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val navigator = LocalNavigator.current
    val scrollState = rememberScrollState()
    val searchState by viewModel.state.collectAsStateWithLifecycle()
    // Each search starts at the top; its own position survives leaving the screen, as for a game opened from it.
    val resultsQuery = (searchState as? SearchState.Results)?.query
    val resultsState = rememberSaveable(resultsQuery, saver = LazyGridState.Saver) { LazyGridState() }

    // Covers typing, clearing, leaving search, and a query restored with the screen.
    LaunchedEffect(query) {
        viewModel.onQueryChange(query)
    }

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
        SearchHeader(scrolledContent = if (searchState == SearchState.Idle) scrollState else resultsState) {
            SearchBar(
                query = query,
                onQueryChange = { value -> query = value },
                onSearch = { submitted ->
                    if (submitted.isNotBlank()) {
                        hideKeyboard()
                        viewModel.onSearch(submitted)
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
                trailingIcon = { SearchClearIcon(visible = query.isNotEmpty(), onClear = { query = "" }) },
            )
            ComingSoonAction(
                imageVector = SlidersHorizontal,
                label = stringResource(Res.string.discover_advanced_filters_coming_soon),
            )
        }
        DiscoverContent(
            searchState = searchState,
            scrollState = scrollState,
            resultsState = resultsState,
            onGameClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
            onLoadMore = viewModel::loadMore,
            onRetry = viewModel::retry,
            // The keyboard covers half the results on a phone; scrolling them puts it away, as searching does.
            onResultsTouchScroll = hideKeyboard,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Discover's own content while nothing is searched; otherwise the search's placeholders, results, or message. A touch
 * scroll through results calls [onResultsTouchScroll]; mouse and trackpad scrolling leave the search field as it is,
 * so typing continues.
 */
@Suppress("LongParameterList")
@Composable
private fun DiscoverContent(
    searchState: SearchState,
    scrollState: ScrollState,
    resultsState: LazyGridState,
    onGameClick: (id: Int) -> Unit,
    onLoadMore: () -> Unit,
    onRetry: () -> Unit,
    onResultsTouchScroll: () -> Unit,
    modifier: Modifier,
) {
    when (searchState) {
        SearchState.Idle -> DiscoverPlaceholders(scrollState = scrollState, modifier = modifier)
        is SearchState.Loading -> SearchResultsSkeleton(modifier = modifier)
        is SearchState.Results -> {
            var isTouch by remember { mutableStateOf(false) }
            val onTouchScroll by rememberUpdatedState(onResultsTouchScroll)
            LaunchedEffect(resultsState) {
                snapshotFlow { resultsState.isScrollInProgress }
                    .filter { scrolling -> scrolling && isTouch }
                    .collect { onTouchScroll() }
            }
            SearchResultsGrid(
                games = searchState.games,
                more = searchState.more,
                onGameClick = onGameClick,
                onLoadMore = onLoadMore,
                onRetry = onRetry,
                modifier = modifier.observePointerType { isMouse -> isTouch = !isMouse },
                state = resultsState,
            )
        }
        is SearchState.Empty -> SearchNoResults(modifier = modifier)
        is SearchState.Error -> SearchError(error = searchState.error, onRetry = onRetry, modifier = modifier)
    }
}

/** Clears the field while it holds text; otherwise the slot stays free for the text. */
@Composable
private fun SearchClearIcon(
    visible: Boolean,
    onClear: () -> Unit,
) {
    if (visible) {
        SearchFieldIcon(imageVector = X, label = stringResource(Res.string.discover_search_clear), onClick = onClear)
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
 * The row under the title that holds the search field and its actions. The space under it is where [scrolledContent]
 * passes out of view, and a hairline marks that edge only while something is scrolled under it. The scroll position is
 * read while drawing, so scrolling does not recompose the header.
 */
@Composable
private fun SearchHeader(
    scrolledContent: ScrollableState,
    content: @Composable RowScope.() -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (scrolledContent.canScrollBackward) {
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
                // While the keyboard is open, the content ends at its top edge and can still scroll clear of it.
                .keyboardPadding()
                .verticalScroll(scrollState)
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
