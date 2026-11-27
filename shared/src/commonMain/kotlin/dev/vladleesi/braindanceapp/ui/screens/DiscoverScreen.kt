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
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
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
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.PageTitle
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute.DiscoverRoute
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
import dev.vladleesi.braindanceapp.resources.discover_results_for
import dev.vladleesi.braindanceapp.resources.discover_search_clear
import dev.vladleesi.braindanceapp.resources.discover_search_close
import dev.vladleesi.braindanceapp.resources.discover_search_placeholder
import dev.vladleesi.braindanceapp.resources.navigation_discover
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCardMaxWidth
import dev.vladleesi.braindanceapp.ui.components.ContentScrollPosition
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.PlaceholderChips
import dev.vladleesi.braindanceapp.ui.components.ScrollAwayBarLayout
import dev.vladleesi.braindanceapp.ui.components.SearchBar
import dev.vladleesi.braindanceapp.ui.components.SearchFieldIcon
import dev.vladleesi.braindanceapp.ui.components.SectionColumns
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.components.rememberContentScrollPosition
import dev.vladleesi.braindanceapp.ui.components.search.SearchError
import dev.vladleesi.braindanceapp.ui.components.search.SearchNoResults
import dev.vladleesi.braindanceapp.ui.components.search.SearchQueryState
import dev.vladleesi.braindanceapp.ui.components.search.SearchResultsGrid
import dev.vladleesi.braindanceapp.ui.components.search.SearchResultsSkeleton
import dev.vladleesi.braindanceapp.ui.components.search.rememberSearchQuery
import dev.vladleesi.braindanceapp.ui.components.sectionColumnsFor
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.viewmodels.GameSearchViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SearchState
import dev.vladleesi.braindanceapp.utils.keyboardPadding
import dev.vladleesi.braindanceapp.utils.observePointerType
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

/**
 * Discover with game search. [search] holds the typed text, which survives recreation, and [viewModel] searches it and
 * holds the results. Without a query that can be searched, Discover shows its own content. In the desktop layout the
 * header's field, which shares [search], is the search field, so Discover shows only its title and results.
 */
@Composable
fun DiscoverScreen(
    modifier: Modifier,
    viewModel: GameSearchViewModel = koinViewModel(),
    search: SearchQueryState = rememberSearchQuery(),
) {
    PageTitle(DiscoverRoute, stringResource(Res.string.navigation_discover))
    var query by search::text
    val headerSearch = desktopLayout() != null
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
    DiscoverSearchRequests(
        search = search,
        onSubmit = viewModel::onSearch,
        focusRequester = searchFocusRequester,
        ownField = !headerSearch,
    )

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
    val clearSearch = rememberSearchRestart(searchFocusRequester) { query = "" }

    // Back first closes the keyboard (the system does that), then leaves search, and only then leaves the screen.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = isSearchActive,
        onBackCompleted = leaveSearch,
    )

    Column(modifier = modifier.fillMaxSize()) {
        SpacerStatusBarInsets()
        val content: @Composable (topInset: Dp, contentModifier: Modifier) -> Unit = { topInset, contentModifier ->
            DiscoverContent(
                searchState = searchState,
                scrollState = scrollState,
                resultsState = resultsState,
                onGameClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                onLoadMore = viewModel::loadMore,
                onRetry = viewModel::retry,
                // The keyboard covers half the results on a phone. Scrolling them puts it away, as searching does.
                onResultsTouchScroll = hideKeyboard,
                topInset = topInset,
                modifier = contentModifier,
            )
        }
        if (headerSearch) {
            DesktopDiscover(
                searchState = searchState,
                scrollState = scrollState,
                resultsState = resultsState,
                onProfileClick = { navigator.navigate(ProfileRoute) },
                content = content,
            )
        } else {
            PhoneDiscoverHeader(
                isSearchActive = isSearchActive,
                query = query,
                onQueryChange = { value -> query = value },
                onSubmit = { submitted ->
                    hideKeyboard()
                    viewModel.onSearch(submitted)
                },
                scrolledContent = if (searchState == SearchState.Idle) scrollState else resultsState,
                focusRequester = searchFocusRequester,
                onFocusChanged = { focused -> isFocused = focused },
                onLeaveSearch = leaveSearch,
                onClearSearch = clearSearch,
                onProfileClick = { navigator.navigate(ProfileRoute) },
            )
            content(0.dp, Modifier.weight(1f))
        }
    }
}

/**
 * Discover's own content while nothing is searched; otherwise the search's placeholders, results, or message. A touch
 * scroll through results calls [onResultsTouchScroll]; mouse and trackpad scrolling leave the search field as it is,
 * so typing continues. [topInset] leaves room at the top for a bar that scrolls away with the content.
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
    topInset: Dp,
    modifier: Modifier,
) {
    when (searchState) {
        SearchState.Idle -> DiscoverPlaceholders(scrollState = scrollState, topInset = topInset, modifier = modifier)
        is SearchState.Loading -> SearchResultsSkeleton(modifier = modifier, contentTopInset = topInset)
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
                contentTopInset = topInset,
            )
        }
        is SearchState.Empty -> SearchNoResults(modifier = modifier.padding(top = topInset))
        is SearchState.Error ->
            SearchError(error = searchState.error, onRetry = onRetry, modifier = modifier.padding(top = topInset))
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

/**
 * Discover's title and search field on phones and tablets. While search is in use, [isSearchActive], the title gives
 * its row to the field, which moves to the top with more room for the content above the keyboard. Escape, Back, and
 * the chevron call [onLeaveSearch]. The field submits only text that is not blank.
 */
@Suppress("LongParameterList")
@Composable
private fun PhoneDiscoverHeader(
    isSearchActive: Boolean,
    query: String,
    onQueryChange: (String) -> Unit,
    onSubmit: (String) -> Unit,
    scrolledContent: ScrollableState,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    onLeaveSearch: () -> Unit,
    onClearSearch: () -> Unit,
    onProfileClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    DiscoverTitle(visible = !isSearchActive, onProfileClick = onProfileClick)
    SearchHeader(scrolledContent = scrolledContent) {
        SearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearch = { submitted -> if (submitted.isNotBlank()) onSubmit(submitted) },
            placeholder = stringResource(Res.string.discover_search_placeholder),
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { state ->
                        focused = state.isFocused
                        onFocusChanged(state.isFocused)
                    }.onPreviewKeyEvent { event ->
                        // Escape leaves search on a hardware keyboard, as back and the chevron do.
                        val leaves = focused && event.key == Key.Escape && event.type == KeyEventType.KeyDown
                        if (leaves) onLeaveSearch()
                        leaves
                    },
            leadingIcon = { SearchLeadingIcon(isSearchActive = isSearchActive, onLeaveSearch = onLeaveSearch) },
            trailingIcon = { SearchClearIcon(visible = query.isNotEmpty(), onClear = onClearSearch) },
        )
        DiscoverFiltersAction()
    }
}

/** Advanced filters, announced but not available yet. */
@Composable
private fun DiscoverFiltersAction() {
    ComingSoonAction(
        imageVector = SlidersHorizontal,
        label = stringResource(Res.string.discover_advanced_filters_coming_soon),
    )
}

/**
 * Discover in the desktop layout, where the header holds the search field. The page's title is its only bar, and it
 * scrolls away with Discover's content or the results, so they never pass under a title that stays. While a search
 * shows, the title names it, so the results always say what they are for.
 */
@Suppress("LongParameterList")
@Composable
private fun DesktopDiscover(
    searchState: SearchState,
    scrollState: ScrollState,
    resultsState: LazyGridState,
    onProfileClick: () -> Unit,
    content: @Composable (topInset: Dp, modifier: Modifier) -> Unit,
) {
    val contentPosition = rememberContentScrollPosition(scrollState)
    val resultsPosition = rememberContentScrollPosition(resultsState)
    val query =
        when (searchState) {
            SearchState.Idle -> null
            is SearchState.Loading -> searchState.query
            is SearchState.Results -> searchState.query
            is SearchState.Empty -> searchState.query
            is SearchState.Error -> searchState.query
        }
    ScrollAwayBarLayout(
        position =
            when (searchState) {
                SearchState.Idle -> contentPosition
                is SearchState.Results -> resultsPosition
                else -> ContentScrollPosition.Unscrolled
            },
        bar = {
            TopLevelTopAppBar(
                title =
                    query?.let { stringResource(Res.string.discover_results_for, it) }
                        ?: stringResource(Res.string.navigation_discover),
                onProfileClick = onProfileClick,
                actions = { DiscoverFiltersAction() },
            )
        },
    ) { topInset ->
        content(topInset, Modifier.fillMaxSize())
    }
}

/**
 * The searches that do not come from typing. Enter in the desktop header's field searches [search] at once with
 * [onSubmit], as the keyboard's search key does in Discover's own field. A search action anywhere in the app
 * (Navigator.openSearch) focuses the [ownField] through [focusRequester], which opens the keyboard on touch devices.
 * Reaching Discover any other way leaves it closed. Asking again, such as after the keyboard was put away while the
 * field kept focus, opens it again. Without its own field, in the desktop layout, the header answers that request.
 */
@Composable
private fun DiscoverSearchRequests(
    search: SearchQueryState,
    onSubmit: (String) -> Unit,
    focusRequester: FocusRequester,
    ownField: Boolean,
) {
    val navigator = LocalNavigator.current
    val keyboardController = LocalSoftwareKeyboardController.current
    LaunchedEffect(search.submissions) {
        if (search.submissions > 0) onSubmit(search.text)
    }
    val isSearchFocusRequested by navigator.isSearchFocusRequested.collectAsStateWithLifecycle()
    LaunchedEffect(isSearchFocusRequested, ownField) {
        if (isSearchFocusRequested && ownField) {
            focusRequester.requestFocus()
            // The field starts its text input session in the frame after it gains focus. A keyboard request before
            // then is dropped on Android when Discover has just been opened.
            withFrameNanos {}
            keyboardController?.show()
            navigator.onSearchFocused()
        }
    }
}

/**
 * Clearing that starts the search over: [onClear] empties the query, and the field keeps or regains focus, so the back
 * chevron stays the only way out. The two buttons never do the same thing, even after a submitted search has put the
 * keyboard away.
 */
@Composable
private fun rememberSearchRestart(
    focusRequester: FocusRequester,
    onClear: () -> Unit,
): () -> Unit {
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val clear by rememberUpdatedState(onClear)
    return remember(focusRequester, keyboardController) {
        {
            clear()
            coroutineScope.launch {
                focusRequester.requestFocus()
                // As for a requested search: the keyboard opens only once the field has its input session.
                withFrameNanos {}
                keyboardController?.show()
            }
        }
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
 * passes out of view, and a hairline across the whole width marks that edge only while something is scrolled under
 * it. The scroll position is read while drawing, so scrolling does not recompose the header. The field starts at the
 * page column's edge and keeps a readable width.
 */
@Composable
private fun SearchHeader(
    scrolledContent: ScrollableState,
    content: @Composable RowScope.() -> Unit,
) {
    val gutter = pageGutter()
    val hairline = secondary
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    if (scrolledContent.canScrollBackward) {
                        val stroke = Dimens.one.toPx()
                        val y = size.height - stroke / 2
                        drawLine(hairline, Offset(0f, y), Offset(size.width, y), stroke)
                    }
                }.pageWidth(gutter)
                // The trailing icon button's own inset completes the gutter, as in the top bar.
                .padding(start = gutter, end = gutter - Dimens.compact, bottom = Dimens.compact),
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
    topInset: Dp,
    modifier: Modifier,
) {
    // Keep the viewport above the keyboard so the content can scroll clear of it.
    Box(modifier.keyboardPadding()) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .pageColumn(pageGutter())
                    .padding(top = topInset + Dimens.tiny, bottom = Dimens.large),
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
            val recommendations: @Composable (Dp) -> Unit = { width ->
                ComingSoonCard(
                    title = stringResource(Res.string.discover_recommendations_title),
                    description = stringResource(Res.string.discover_recommendations_description),
                    maxWidth = width,
                )
            }
            val genres: @Composable (Dp) -> Unit = { width ->
                ComingSoonCard(
                    title = stringResource(Res.string.discover_genres_title),
                    description = stringResource(Res.string.discover_genres_description),
                    maxWidth = width,
                )
            }
            if (desktopLayout() != null) {
                // Side by side across the column, as the sections they stand for will be.
                BoxWithConstraints {
                    SectionColumns(
                        columns = sectionColumnsFor(maxWidth, maxColumns = 2),
                        sections = listOf({ recommendations(Dp.Unspecified) }, { genres(Dp.Unspecified) }),
                    )
                }
            } else {
                recommendations(ComingSoonCardMaxWidth)
                genres(ComingSoonCardMaxWidth)
            }
        }
        PageScrollbar(scrollState)
    }
}
