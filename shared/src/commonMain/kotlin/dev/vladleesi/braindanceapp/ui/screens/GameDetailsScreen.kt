// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_not_found
import dev.vladleesi.braindanceapp.resources.game_details_screen_similar_games
import dev.vladleesi.braindanceapp.resources.game_details_show_all
import dev.vladleesi.braindanceapp.resources.game_details_show_fewer
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.ExpandableText
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.details.GameDetailsHero
import dev.vladleesi.braindanceapp.ui.components.details.GameVersionSelector
import dev.vladleesi.braindanceapp.ui.components.details.LibraryActionsState
import dev.vladleesi.braindanceapp.ui.components.details.OriginLine
import dev.vladleesi.braindanceapp.ui.components.details.RelatedGameRow
import dev.vladleesi.braindanceapp.ui.components.details.RelatedHeader
import dev.vladleesi.braindanceapp.ui.components.details.storesBlockItem
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.RecommendationsState
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedCategory
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedGroup
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.ui.viewmodels.VersionSelector
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds

private const val HERO_KEY = "hero"
private const val VERSIONS_KEY = "versions"
private const val RELATED_HEADER_KEY = "related_header"
private const val RELATED_MORE_KEY = "related_more"
private const val SIMILAR_GAMES_KEY = "similar_games"
private const val SIMILAR_GAME_CONTENT_TYPE = "similar_game"
private const val RELATED_ROW_CONTENT_TYPE = "related_row"

/** Related releases shown before "Show all". */
private const val RELATED_COLLAPSED = 3

/** Related rows side by side in a wide layout. */
private const val WIDE_RELATED_COLUMNS = 2

@Composable
fun GameDetailsScreen(
    route: GameDetailsRoute,
    modifier: Modifier,
) {
    val viewModel: GameDetailsViewModel =
        // Scoped to this navigation entry, which clears it when the screen is popped. A version switch keeps the entry
        // (see GameDetailsRoute.page), so the ViewModel stays and switches the version in place.
        koinViewModel(parameters = { parametersOf(route.id) })

    val navigator = LocalNavigator.current

    val state by viewModel.gameDetailsState.collectAsStateWithLifecycle()
    val recommendations by viewModel.recommendations.collectAsStateWithLifecycle()

    // The ViewModel loads once per entry; later calls do nothing until a refresh. A new ID is a version switch, also
    // when Back or Forward in a browser restores another version of this page.
    LaunchedEffect(route.id) {
        viewModel.load()
        viewModel.showVersion(route.id)
    }

    when (val currentState = state) {
        is GameDetailsState.Error ->
            ErrorScreen(errorMessage = currentState.error.userMessage(), modifier = modifier) {
                viewModel.refresh()
            }

        GameDetailsState.NotFound ->
            ErrorScreen(errorMessage = stringResource(Res.string.game_details_not_found), modifier = modifier) {
                viewModel.refresh()
            }

        GameDetailsState.Loading ->
            GlobalLoading(modifier = modifier)

        is GameDetailsState.Success ->
            key(currentState.gameDetails.id) {
                GameDetailsScreen(
                    route = route,
                    state = currentState,
                    recommendations = recommendations,
                    modifier = modifier,
                    onBackButtonPressed = { navigator.goBack() },
                )
            }
    }
}

@Composable
private fun GameDetailsScreen(
    route: GameDetailsRoute,
    state: GameDetailsState.Success,
    recommendations: RecommendationsState,
    modifier: Modifier,
    onBackButtonPressed: (() -> Unit),
) {
    val navigator = LocalNavigator.current

    val lazyListState = rememberLazyListState()
    val (topBarColor, backButtonColor, topBarTitleColor) =
        lazyListState.calculateScrollTopBarColors()
    val isLargeWindow = isLargeWindow()
    val gutter = pageGutter()
    // Another game opens on top; returning to the game under this one goes back to it rather than stacking a copy.
    val openGame: (Int) -> Unit = { id -> navigator.open(GameDetailsRoute(id)) }
    // Versions of one game are views of one page: switching updates this screen in place, with no transition and no
    // Back step, while the address follows the version.
    val selectVersion: (Int) -> Unit = { id -> navigator.replace(route.withVersion(id)) }
    // Placeholders stand in for the release's own details only when a version takes a moment to load, so a fast
    // switch does not flash them; the selected chip already responds at once.
    val loadingRelease = rememberDelayedLoading(state.isSwitchingVersion)
    // The chosen tab and whether it shows every release, kept per page across recreation.
    var relatedCategory by rememberSaveable(state.gameDetails.id) { mutableStateOf<String?>(null) }
    var relatedExpanded by rememberSaveable(state.gameDetails.id) { mutableStateOf(false) }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
        ) {
            item(key = HERO_KEY) {
                GameDetailsHero(
                    details = state.gameDetails,
                    expanded = isLargeWindow,
                    // Accounts and collections do not exist yet, so the actions stay disabled and never call back.
                    // The library will provide this state and handle onLibraryStatusChange.
                    libraryActions = LibraryActionsState.Unavailable,
                    onLibraryStatusChange = {},
                    // Genres become chips once Discover can browse by genre; until then they are plain tags.
                    onGenreClick = null,
                    relation =
                        state.gameDetails.relations.origin?.let { origin ->
                            { OriginLine(origin = origin, onOpenGame = openGame) }
                        },
                    loadingRelease = loadingRelease,
                )
            }
            versionSelectorItem(
                selector = state.gameDetails.relations.versions,
                compact = !isLargeWindow,
                gutter = gutter,
                onSelect = selectVersion,
                versionRoute = route::withVersion,
            )
            releaseItems(state.gameDetails, gutter, placeholder = loadingRelease)
            relatedItems(
                groups = state.gameDetails.relations.related,
                category = relatedCategory,
                expanded = relatedExpanded,
                compact = !isLargeWindow,
                gutter = gutter,
                onCategoryChange = { category ->
                    relatedCategory = category.name
                    relatedExpanded = false
                },
                onToggleExpanded = { relatedExpanded = !relatedExpanded },
                onGameClick = openGame,
            )
            similarGamesItems(recommendations, gutter, onGameClick = openGame)
            item { Spacer(modifier = Modifier.size(Dimens.large)) }
        }
        PageScrollbar(lazyListState)
        GameDetailsTopBar(
            gameDetails = state.gameDetails,
            topBarColor = topBarColor,
            backButtonColor = backButtonColor,
            titleColor = topBarTitleColor,
            onBackButtonPressed = onBackButtonPressed,
        )
    }
}

/**
 * The bar over the scrolling content: back, the game's name, and, in layouts that [showsShareActions], Share, which
 * links to the game's page. The title fades in as the bar fills, but Share is always visible: like the back button it
 * is a white icon on a circular backdrop that is a dark chip over the artwork and blends into the bar once it fills.
 */
@Composable
private fun GameDetailsTopBar(
    gameDetails: GameDetails,
    topBarColor: Color,
    backButtonColor: Color,
    titleColor: Color,
    onBackButtonPressed: () -> Unit,
) {
    val shareTarget = ShareTarget.Game(id = gameDetails.releaseId, name = gameDetails.name)
    Column {
        StatusBarOverlay(backgroundColor = topBarColor)
        TopAppBar(
            tabBarColor = topBarColor,
            titleColor = titleColor,
            showBackButton = true,
            backButtonBackgroundColor = backButtonColor,
            onBackButtonPressed = onBackButtonPressed,
            title = gameDetails.name,
            actions =
                if (showsShareActions()) {
                    { ShareButton(target = shareTarget, backgroundColor = backButtonColor, visible = true) }
                } else {
                    null
                },
        )
    }
}

/**
 * The shown release's summary and stores. While [placeholder], another version's are loading and these keep their
 * place as shimmering placeholders of their exact size.
 */
private fun LazyListScope.releaseItems(
    details: GameDetails,
    gutter: Dp,
    placeholder: Boolean,
) {
    if (details.summary.isNotEmpty()) {
        item {
            ExpandableText(
                text = details.summary,
                modifier =
                    Modifier
                        .pageColumn(gutter)
                        .widthIn(max = Dimens.readableTextWidth)
                        .fillMaxWidth(),
                placeholder = placeholder,
            )
        }
        item { Spacer(modifier = Modifier.size(Dimens.large)) }
    }
    if (details.stores.isNotEmpty()) {
        storesBlockItem(details.stores, placeholder)
    }
}

// How long a version switch may take before placeholders replace the shown release: below it, the new details
// simply appear; above it, the wait is visible.
private const val RELEASE_SKELETON_DELAY_MILLIS = 150L

/** True once [loading] has lasted [RELEASE_SKELETON_DELAY_MILLIS], and false as soon as it ends. */
@Composable
private fun rememberDelayedLoading(loading: Boolean): Boolean {
    var shown by remember { mutableStateOf(false) }
    LaunchedEffect(loading) {
        if (loading) {
            delay(RELEASE_SKELETON_DELAY_MILLIS.milliseconds)
            shown = true
        } else {
            shown = false
        }
    }
    return loading && shown
}

/** The selector of the game's versions, when it has more than one. */
private fun LazyListScope.versionSelectorItem(
    selector: VersionSelector?,
    compact: Boolean,
    gutter: Dp,
    onSelect: (Int) -> Unit,
    versionRoute: (Int) -> NavKey,
) {
    selector ?: return
    item(key = VERSIONS_KEY) {
        GameVersionSelector(
            selector = selector,
            compact = compact,
            onSelect = onSelect,
            versionRoute = versionRoute,
            modifier = Modifier.pageColumn(gutter).widthIn(max = Dimens.readableTextWidth),
        )
    }
    item { Spacer(modifier = Modifier.size(Dimens.large)) }
}

/**
 * One section for every related release: the heading or tabs, then the selected kind as compact rows, one to a line
 * in a [compact] layout and [WIDE_RELATED_COLUMNS] otherwise, and "Show all" past [RELATED_COLLAPSED]. Each row is
 * its own lazy item, so a long list composes only what is on screen. [category] is the saved tab's name; an unknown or
 * missing one shows the first kind.
 */
@Suppress("LongParameterList")
private fun LazyListScope.relatedItems(
    groups: List<RelatedGroup>,
    category: String?,
    expanded: Boolean,
    compact: Boolean,
    gutter: Dp,
    onCategoryChange: (RelatedCategory) -> Unit,
    onToggleExpanded: () -> Unit,
    onGameClick: (Int) -> Unit,
) {
    val group = groups.firstOrNull { it.category.name == category } ?: groups.firstOrNull() ?: return
    item { Spacer(modifier = Modifier.size(Dimens.large)) }
    item(key = RELATED_HEADER_KEY) {
        RelatedHeader(
            groups = groups,
            selected = group.category,
            compact = compact,
            onSelect = onCategoryChange,
            modifier = Modifier.pageColumn(gutter).padding(bottom = Dimens.small),
        )
    }
    val columns = if (compact) 1 else WIDE_RELATED_COLUMNS
    val shown = if (expanded) group.games else group.games.take(RELATED_COLLAPSED)
    for (line in shown.chunked(columns)) {
        item(key = "related:${group.category}:${line.first().id}", contentType = RELATED_ROW_CONTENT_TYPE) {
            Row(
                modifier = Modifier.pageColumn(gutter),
                horizontalArrangement = Arrangement.spacedBy(Dimens.large),
            ) {
                line.forEach { game ->
                    RelatedGameRow(game = game, onClick = onGameClick, modifier = Modifier.weight(1f))
                }
                // A last line with fewer rows keeps the column width of the lines above.
                repeat(columns - line.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
    if (group.games.size > RELATED_COLLAPSED) {
        item(key = RELATED_MORE_KEY) {
            TonalButton(
                onClick = onToggleExpanded,
                modifier = Modifier.pageColumn(gutter).padding(top = Dimens.small),
            ) {
                Text(
                    text =
                        if (expanded) {
                            stringResource(Res.string.game_details_show_fewer)
                        } else {
                            stringResource(Res.string.game_details_show_all, group.games.size)
                        },
                )
            }
        }
    }
}

/** Similar games while they load and once there are any; a failure hides the section instead. */
private fun LazyListScope.similarGamesItems(
    recommendations: RecommendationsState,
    gutter: Dp,
    onGameClick: (Int) -> Unit,
) {
    val showRecommendations =
        recommendations is RecommendationsState.Loading ||
            (recommendations is RecommendationsState.Success && recommendations.games.isNotEmpty())
    if (!showRecommendations) return
    item { Spacer(modifier = Modifier.size(Dimens.large)) }
    item(key = SIMILAR_GAMES_KEY) {
        val title = stringResource(Res.string.game_details_screen_similar_games)
        val sectionState =
            remember(recommendations) {
                when (recommendations) {
                    is RecommendationsState.Success -> SectionState.Success(recommendations.games)
                    else -> SectionState.Loading
                }
            }
        val cardModifier = Modifier.width(Dimens.gameCardWidth)
        CarouselSection(
            title = title,
            state = sectionState,
            // Never shown: the section has no error state.
            onRetry = {},
            skeleton = { GameCardSkeleton(showTitle = true, modifier = cardModifier) },
            modifier = Modifier.pageWidth(gutter),
            titleStyle = MaterialTheme.typography.h3,
            horizontalPadding = gutter,
        ) { games ->
            items(games, key = { game -> game.id }, contentType = { SIMILAR_GAME_CONTENT_TYPE }) { game ->
                GameCard(
                    card = game,
                    onClick = onGameClick,
                    showTitle = true,
                    modifier = cardModifier,
                )
            }
        }
    }
}
