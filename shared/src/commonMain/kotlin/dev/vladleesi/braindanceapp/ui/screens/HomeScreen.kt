// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Bell
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routeLink
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.Compilation
import dev.vladleesi.braindanceapp.navigation.routes.CompilationRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.home_latest_news_description
import dev.vladleesi.braindanceapp.resources.home_latest_news_title
import dev.vladleesi.braindanceapp.resources.home_notifications_coming_soon
import dev.vladleesi.braindanceapp.resources.home_open_news
import dev.vladleesi.braindanceapp.resources.home_open_releases
import dev.vladleesi.braindanceapp.resources.home_recommendations_description
import dev.vladleesi.braindanceapp.resources.home_recommendations_title
import dev.vladleesi.braindanceapp.resources.home_screen_popular_latest_giveaways
import dev.vladleesi.braindanceapp.resources.home_screen_popular_most_anticipated
import dev.vladleesi.braindanceapp.resources.home_screen_popular_right_now
import dev.vladleesi.braindanceapp.resources.home_upcoming_releases_description
import dev.vladleesi.braindanceapp.resources.navigation_home
import dev.vladleesi.braindanceapp.resources.releases_upcoming_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.SearchAction
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayCard
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.EventsCarousel
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith80Alpha
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.observePointerType
import dev.vladleesi.braindanceapp.utils.rememberCachedLazyListState
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    // Wide Web windows show the desktop layout of the same state; Android and iOS never enable it.
    val desktopLayout = desktopLayout()
    if (desktopLayout != null) {
        DesktopHomeScreen(layout = desktopLayout, viewModel = viewModel, modifier = modifier)
    } else {
        StandardHomeScreen(modifier = modifier, viewModel = viewModel)
    }
}

@Composable
@OptIn(ExperimentalMaterialApi::class)
private fun StandardHomeScreen(
    modifier: Modifier,
    viewModel: HomeViewModel,
) {
    val navigator = LocalNavigator.current

    val trendingNow by viewModel.trendingNow.collectAsStateWithLifecycle()
    val mostAnticipated by viewModel.mostAnticipated.collectAsStateWithLifecycle()
    val events by viewModel.events.collectAsStateWithLifecycle()
    val giveaways by viewModel.giveaways.collectAsStateWithLifecycle()

    // Load the screen only on first navigation
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    val pullState =
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = viewModel::refresh,
        )
    // A mouse wheel or trackpad overscroll has no release that ends the pull, so it would leave the indicator stuck
    // part-way down. Pull-to-refresh therefore follows the most recent pointer: it works for touch only.
    var isMousePointer by remember { mutableStateOf(false) }
    // Each Home item is a whole section, such as a carousel with its images, so composing one costs a frame.
    val listState = rememberCachedLazyListState()
    // Carousels scroll through the gutter, so they take the page column's width and inset their own cards by it.
    val gutter = pageGutter()
    val carouselModifier = Modifier.pageWidth(gutter)

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background),
    ) {
        LazyColumn(
            state = listState,
            modifier =
                modifier
                    .fillMaxSize()
                    .observePointerType { isMouse -> isMousePointer = isMouse }
                    .pullRefresh(pullState, enabled = !isMousePointer),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            // The sections with real content come first, then the previews of planned ones, so the games are within
            // a swipe or two rather than spread between placeholders. Desktop Home groups them the same way.
            item {
                HomeHeader(
                    trendingNow = trendingNow,
                    onProfileClick = { navigator.navigate(ProfileRoute) },
                    onSearchClick = navigator::openSearch,
                    onRetry = viewModel::loadTrendingNow,
                    seeAllRoute = CompilationRoute(Compilation.Popular),
                    onSeeAll = navigator::navigate,
                    onGameClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                    carouselModifier = carouselModifier,
                )
            }
            item {
                GamesCarousel(
                    title = stringResource(Res.string.home_screen_popular_most_anticipated),
                    state = mostAnticipated,
                    onRetry = { viewModel.loadMostAnticipated() },
                    seeAllRoute = CompilationRoute(Compilation.Anticipated),
                    onSeeAll = navigator::navigate,
                    showTitles = false,
                    onGameClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                    modifier = carouselModifier,
                )
            }
            item {
                GiveawaysCarousel(
                    title = stringResource(Res.string.home_screen_popular_latest_giveaways),
                    state = giveaways,
                    onRetry = { viewModel.loadGiveaways() },
                    seeAllRoute = CompilationRoute(Compilation.Giveaways),
                    onSeeAll = navigator::navigate,
                    onGiveawayClick = { id -> navigator.navigate(GiveawayDetailsRoute(id)) },
                    modifier = carouselModifier,
                )
            }
            item { EventsCarousel(events, viewModel::loadEvents, navigator::navigate, Modifier.fillMaxWidth()) }
            homePreviewCards(
                onOpenReleases = { navigator.navigate(BottomBarRoute.ReleasesRoute) },
                onOpenNews = { navigator.navigate(BottomBarRoute.NewsRoute) },
                cardModifier = Modifier.pageColumn(gutter),
            )
            item { Spacer(Modifier.height(Dimens.large)) }
        }
        PageScrollbar(listState)
        StatusBarOverlay(backgroundColor = overlayBlackWith80Alpha)
        PullRefreshIndicator(
            refreshing = isRefreshing,
            state = pullState,
            modifier =
                Modifier
                    .padding(top = Dimens.statusBarInsets)
                    .align(TopCenter),
        )
    }
}

private const val GAME_CARD_CONTENT_TYPE = "game_card"
private const val GIVEAWAY_CARD_CONTENT_TYPE = "giveaway_card"

/**
 * The top bar and the first section as one list item, so Popular Right Now starts right under the title, like the
 * content of every other screen, without the gap that separates sections.
 */
@Suppress("LongParameterList")
@Composable
private fun HomeHeader(
    trendingNow: SectionState<GameCardModel>,
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
    onRetry: () -> Unit,
    seeAllRoute: NavKey,
    onSeeAll: (NavKey) -> Unit,
    onGameClick: (id: Int) -> Unit,
    carouselModifier: Modifier,
) {
    Column {
        HomeTopBar(onProfileClick = onProfileClick, onSearchClick = onSearchClick)
        GamesCarousel(
            title = stringResource(Res.string.home_screen_popular_right_now),
            state = trendingNow,
            onRetry = onRetry,
            seeAllRoute = seeAllRoute,
            onSeeAll = onSeeAll,
            showTitles = true,
            onGameClick = onGameClick,
            modifier = carouselModifier,
        )
    }
}

/**
 * A carousel of game cards; [showTitles] adds each game's title and trending reason below its cover, and [onSeeAll]
 * opens the section's full list at [seeAllRoute].
 */
@Suppress("LongParameterList")
@Composable
private fun GamesCarousel(
    title: String,
    state: SectionState<GameCardModel>,
    onRetry: () -> Unit,
    seeAllRoute: NavKey,
    onSeeAll: (NavKey) -> Unit,
    showTitles: Boolean,
    onGameClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardModifier = Modifier.width(Dimens.gameCardWidth)
    CarouselSection(
        title = title,
        state = state,
        modifier = modifier,
        onRetry = onRetry,
        onSeeAll = { onSeeAll(seeAllRoute) },
        seeAllRoute = seeAllRoute,
        skeleton = { GameCardSkeleton(showTitle = showTitles, modifier = cardModifier) },
    ) { games ->
        items(games, key = { game -> game.id }, contentType = { GAME_CARD_CONTENT_TYPE }) { game ->
            GameCard(card = game, onClick = onGameClick, showTitle = showTitles, modifier = cardModifier)
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun GiveawaysCarousel(
    title: String,
    state: SectionState<GiveawayItemModel>,
    onRetry: () -> Unit,
    seeAllRoute: NavKey,
    onSeeAll: (NavKey) -> Unit,
    onGiveawayClick: (id: Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cardModifier = Modifier.width(Dimens.giveAwayCardWidth)
    CarouselSection(
        title = title,
        state = state,
        modifier = modifier,
        onRetry = onRetry,
        onSeeAll = { onSeeAll(seeAllRoute) },
        seeAllRoute = seeAllRoute,
        skeleton = { GiveawayCardSkeleton(modifier = cardModifier) },
    ) { giveaways ->
        items(giveaways, key = { giveaway -> giveaway.id }, contentType = { GIVEAWAY_CARD_CONTENT_TYPE }) { giveaway ->
            GiveawayCard(item = giveaway, onClick = onGiveawayClick, modifier = cardModifier)
        }
    }
}

/** The title, a search action that starts a search on Discover, notifications, and the profile button. */
@Composable
private fun HomeTopBar(
    onProfileClick: () -> Unit,
    onSearchClick: () -> Unit,
) {
    // One list item, so the list spacing starts below the bar rather than between it and the status bar.
    Column {
        SpacerStatusBarInsets()
        TopLevelTopAppBar(
            title = stringResource(Res.string.navigation_home),
            onProfileClick = onProfileClick,
            actions = {
                SearchAction(onClick = onSearchClick)
                ComingSoonAction(
                    imageVector = Bell,
                    label = stringResource(Res.string.home_notifications_coming_soon),
                )
            },
        )
    }
}

/** Previews of planned Home sections, after the sections with real content. */
private fun LazyListScope.homePreviewCards(
    onOpenReleases: () -> Unit,
    onOpenNews: () -> Unit,
    cardModifier: Modifier,
) {
    item {
        HomePreviewCard(
            title = Res.string.home_recommendations_title,
            description = Res.string.home_recommendations_description,
            modifier = cardModifier,
        )
    }
    item {
        HomePreviewCard(
            title = Res.string.releases_upcoming_title,
            description = Res.string.home_upcoming_releases_description,
            modifier = cardModifier,
            link = Res.string.home_open_releases,
            onLinkClick = onOpenReleases,
            linkRoute = BottomBarRoute.ReleasesRoute,
        )
    }
    item {
        HomePreviewCard(
            title = Res.string.home_latest_news_title,
            description = Res.string.home_latest_news_description,
            modifier = cardModifier,
            link = Res.string.home_open_news,
            onLinkClick = onOpenNews,
            linkRoute = BottomBarRoute.NewsRoute,
        )
    }
}

/**
 * A planned Home section; [link], when given, opens the destination that will host it through [onLinkClick], and
 * [linkRoute] is that destination's route, so the link also opens in a new browser tab.
 */
@Composable
private fun HomePreviewCard(
    title: StringResource,
    description: StringResource,
    modifier: Modifier = Modifier,
    link: StringResource? = null,
    onLinkClick: (() -> Unit)? = null,
    linkRoute: NavKey? = null,
) {
    ComingSoonCard(
        title = stringResource(title),
        description = stringResource(description),
        modifier = modifier,
        action =
            if (link != null && onLinkClick != null) {
                {
                    TextButton(
                        onClick = onLinkClick,
                        modifier = linkRoute?.let { Modifier.routeLink(it) } ?: Modifier,
                    ) {
                        Text(stringResource(link), color = white)
                    }
                }
            } else {
                null
            },
    )
}
