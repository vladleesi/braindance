// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.PageTitle
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_complete
import dev.vladleesi.braindanceapp.resources.about_critic
import dev.vladleesi.braindanceapp.resources.about_developer
import dev.vladleesi.braindanceapp.resources.about_main
import dev.vladleesi.braindanceapp.resources.game_details_not_found
import dev.vladleesi.braindanceapp.resources.game_details_screen_similar_games
import dev.vladleesi.braindanceapp.resources.game_details_show_all
import dev.vladleesi.braindanceapp.resources.game_details_show_fewer
import dev.vladleesi.braindanceapp.system.LocalDesktopLayoutEnabled
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.BackOnlyFrame
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.DisclosureButton
import dev.vladleesi.braindanceapp.ui.components.FullWindowOverlay
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.SupportingPaneLayout
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopHeaderHeight
import dev.vladleesi.braindanceapp.ui.components.details.GameDetailsHero
import dev.vladleesi.braindanceapp.ui.components.details.GameFacts
import dev.vladleesi.braindanceapp.ui.components.details.GameOverview
import dev.vladleesi.braindanceapp.ui.components.details.GameStoryline
import dev.vladleesi.braindanceapp.ui.components.details.GameVersionSelector
import dev.vladleesi.braindanceapp.ui.components.details.LibraryActionsState
import dev.vladleesi.braindanceapp.ui.components.details.OriginLine
import dev.vladleesi.braindanceapp.ui.components.details.RelatedGameRow
import dev.vladleesi.braindanceapp.ui.components.details.RelatedHeader
import dev.vladleesi.braindanceapp.ui.components.details.StoresBlock
import dev.vladleesi.braindanceapp.ui.components.details.TimeToBeatCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.media.GameMedia
import dev.vladleesi.braindanceapp.ui.components.media.MediaGallery
import dev.vladleesi.braindanceapp.ui.components.media.MediaGalleryState
import dev.vladleesi.braindanceapp.ui.components.media.MediaViewer
import dev.vladleesi.braindanceapp.ui.components.media.rememberMediaGalleryState
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageContentWidth
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.viewmodels.GameBadge
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.GameFact
import dev.vladleesi.braindanceapp.ui.viewmodels.GameFactsModel
import dev.vladleesi.braindanceapp.ui.viewmodels.GamePlayTimes
import dev.vladleesi.braindanceapp.ui.viewmodels.GameRelations
import dev.vladleesi.braindanceapp.ui.viewmodels.GenreTag
import dev.vladleesi.braindanceapp.ui.viewmodels.PlayTime
import dev.vladleesi.braindanceapp.ui.viewmodels.RecommendationsState
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedCategory
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedGame
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedGroup
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.ui.viewmodels.VersionOption
import dev.vladleesi.braindanceapp.ui.viewmodels.VersionSelector
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Duration.Companion.milliseconds

private const val SIMILAR_GAME_CONTENT_TYPE = "similar_game"
private const val RELEASE_HEADER_RESIZE_MILLIS = 180

/** Related releases shown before "Show all". */
private const val RELATED_COLLAPSED = 3

/** Related rows side by side in a wide layout. */
private const val WIDE_RELATED_COLUMNS = 2

// Wider pages fit more related releases side by side, each at least this wide.
private val RelatedRowMinWidth = 360.dp
private const val RELATED_COLUMNS_MAX = 4

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

    GameDetailsContent(
        route = route,
        state = state,
        recommendations = recommendations,
        modifier = modifier,
        onBack = { navigator.goBack() },
        onRetry = { viewModel.refresh() },
        onOpenGame = { id -> navigator.open(GameDetailsRoute(id)) },
        // Versions share one page and change its address in place without adding a Back step.
        onSelectVersion = { id -> navigator.replace(route.withVersion(id)) },
    )
}

@Suppress("LongParameterList")
@Composable
private fun GameDetailsContent(
    route: GameDetailsRoute,
    state: GameDetailsState,
    recommendations: RecommendationsState,
    modifier: Modifier,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    onOpenGame: (Int) -> Unit,
    onSelectVersion: (Int) -> Unit,
) {
    when (state) {
        is GameDetailsState.Error ->
            BackOnlyFrame(onBack = onBack, modifier = modifier) { content ->
                ErrorScreen(errorMessage = state.error.userMessage(), modifier = content, onRetry = onRetry)
            }

        GameDetailsState.NotFound ->
            BackOnlyFrame(onBack = onBack, modifier = modifier) { content ->
                ErrorScreen(
                    errorMessage = stringResource(Res.string.game_details_not_found),
                    modifier = content,
                    onRetry = onRetry,
                )
            }

        GameDetailsState.Loading ->
            BackOnlyFrame(onBack = onBack, modifier = modifier) { content ->
                GlobalLoading(modifier = content)
            }

        is GameDetailsState.Success -> {
            PageTitle(route, state.gameDetails.name)
            key(state.gameDetails.id) {
                GameDetailsPage(
                    route = route,
                    state = state,
                    recommendations = recommendations,
                    modifier = modifier,
                    onBackButtonPressed = onBack,
                    onOpenGame = onOpenGame,
                    onSelectVersion = onSelectVersion,
                )
            }
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun GameDetailsPage(
    route: GameDetailsRoute,
    state: GameDetailsState.Success,
    recommendations: RecommendationsState,
    modifier: Modifier,
    onBackButtonPressed: () -> Unit,
    onOpenGame: (Int) -> Unit,
    onSelectVersion: (Int) -> Unit,
) {
    val uriHandler = LocalUriHandler.current

    val scrollState = rememberScrollState()
    val isLargeWindow = isLargeWindow()
    val desktop = desktopLayout() != null
    val gutter = pageGutter()
    // Placeholders stand in for the release's own details only when a version takes a moment to load, so a fast
    // switch does not flash them; the selected chip already responds at once.
    val loadingRelease = rememberDelayedLoading(state.isSwitchingVersion)
    // The chosen tab and whether it shows every release, kept per page across recreation.
    var relatedCategory by rememberSaveable(state.gameDetails.id) { mutableStateOf<String?>(null) }
    var relatedExpanded by rememberSaveable(state.gameDetails.id) { mutableStateOf(false) }
    // Shared by the gallery and its full-screen viewer, and kept at the screen so the viewer stays open however far
    // the list scrolls and across recreation.
    val media = rememberMediaGalleryState(state.gameDetails.media)
    val watchOnYouTube: (GameMedia.YouTubeVideo) -> Unit = { video -> uriHandler.openExternalUrl(video.watchUrl) }

    Box(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        // The page has a bounded body with lazy horizontal galleries. ScrollState uses its measured height,
        // including expanded text and related rows, for both scrolling and the Web scrollbar.
        Column(Modifier.fillMaxSize().verticalScroll(scrollState)) {
            // New edition titles and wrapped version chips can change the hero's height. Its width is fixed first, so
            // only the height eases: a page that widens, as when the sidebar collapses, takes the hero along at once.
            Column(Modifier.fillMaxWidth().animateContentSize(tween(RELEASE_HEADER_RESIZE_MILLIS))) {
                GameDetailsHero(
                    details = state.gameDetails,
                    expanded = isLargeWindow,
                    // The hero is the first thing in the page, so the page's scroll is how far it has moved.
                    scrolledPx = { scrollState.value },
                    // Accounts and collections do not exist yet, so the actions stay disabled and never call back.
                    // The library will provide this state and handle onLibraryStatusChange.
                    libraryActions = LibraryActionsState.Unavailable,
                    onLibraryStatusChange = {},
                    // Genres become chips once Discover can browse by genre; until then they are plain tags.
                    onGenreClick = null,
                    relation =
                        state.gameDetails.relations.origin?.let { origin ->
                            { OriginLine(origin = origin, onOpenGame = onOpenGame) }
                        },
                    loadingRelease = loadingRelease,
                )
                VersionSelectorBlock(
                    selector = state.gameDetails.relations.versions,
                    compact = !isLargeWindow,
                    gutter = gutter,
                    onSelect = onSelectVersion,
                    versionRoute = route::withVersion,
                )
            }
            key(state.gameDetails.releaseId) {
                DetailsBody(
                    state.gameDetails,
                    media,
                    gutter,
                    desktop,
                    loadingRelease,
                    watchOnYouTube,
                    scrollState = scrollState,
                )
            }
            RelatedReleases(
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
                onGameClick = onOpenGame,
            )
            SimilarGames(recommendations, gutter, onGameClick = onOpenGame)
            Spacer(modifier = Modifier.size(Dimens.large))
        }
        PageScrollbar(scrollState, topInset = Dimens.heroTopBarHeightWithInsets)
        GameDetailsTopBar(
            gameDetails = state.gameDetails,
            scrollState = scrollState,
            onBackButtonPressed = onBackButtonPressed,
        )
    }
    if (media.isViewerOpen) {
        FullWindowOverlay {
            MediaViewer(state = media, gameName = state.gameDetails.name)
        }
    }
}

/**
 * The bar over the scrolling content: back, the game's name, and, in layouts that [showsShareActions], Share, which
 * links to the game's page. The title fades in as the bar fills, but Share is always visible: like the back button it
 * is an icon on a circular backdrop that is a dark chip over the artwork and blends into the bar once it fills.
 */
@Composable
private fun GameDetailsTopBar(
    gameDetails: GameDetails,
    scrollState: ScrollState,
    onBackButtonPressed: () -> Unit,
) {
    val fadeDistance = with(LocalDensity.current) { Dimens.heroTopBarHeightWithInsets.toPx() }
    val fraction by remember(scrollState, fadeDistance) {
        derivedStateOf { (scrollState.value / fadeDistance).coerceIn(0f, 1f) }
    }
    val topBarColor = lerp(Color.Transparent, navBar, fraction)
    val backButtonColor = lerp(navBar, Color.Transparent, fraction)
    val titleColor = lerp(Color.Transparent, foreground, fraction)
    val shareTarget = ShareTarget.Game(id = gameDetails.releaseId, name = gameDetails.name)
    Column {
        StatusBarOverlay(backgroundColor = topBarColor)
        TopAppBar(
            height = Dimens.heroTopBarHeight,
            tabBarColor = topBarColor,
            titleColor = titleColor,
            showBackButton = true,
            backButtonBackgroundColor = backButtonColor,
            onBackButtonPressed = onBackButtonPressed,
            title = gameDetails.name,
            titleStyle = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
            subtitle = gameDetails.releaseDate.orEmpty(),
            actions =
                if (showsShareActions()) {
                    { ShareButton(target = shareTarget, backgroundColor = backButtonColor, visible = true) }
                } else {
                    null
                },
        )
    }
}

@Composable
private fun DetailsBody(
    details: GameDetails,
    media: MediaGalleryState,
    gutter: Dp,
    desktop: Boolean,
    placeholder: Boolean,
    onWatchOnYouTube: (GameMedia.YouTubeVideo) -> Unit = {},
    scrollState: ScrollState? = null,
) {
    val onCollapse = if (desktop && scrollState != null) anchorOnCollapse(scrollState) else null
    val overview = @Composable {
        GameOverview(
            details.summary,
            details.overview,
            placeholder = placeholder,
            onCollapse = onCollapse,
            adult = details.isAdult,
        )
    }
    val storyline = @Composable { GameStoryline(details.storyline, placeholder = placeholder, onCollapse = onCollapse) }
    val about = @Composable { GameFacts(details.facts, placeholder = placeholder) }
    val stores = @Composable { StoresBlock(stores = details.stores, placeholder = placeholder) }
    val hasOverview = details.summary.isNotBlank() || details.overview.isNotEmpty() || details.isAdult
    if (desktop) {
        SupportingPaneLayout(
            main = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.large)) {
                    // Text keeps its line length, while the media take the column's full width.
                    if (hasOverview) ReadableWidth(overview)
                    if (!media.gallery.isEmpty) {
                        DetailsMedia(
                            details.name,
                            media,
                            0.dp,
                            placeholder,
                            onWatchOnYouTube,
                            scrollState = scrollState,
                        )
                    }
                    if (details.storyline.isNotEmpty()) ReadableWidth(storyline)
                }
            },
            pane = {
                if (!details.facts.isEmpty || details.playTimes != null || details.stores.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.medium)) {
                        if (!details.facts.isEmpty) about()
                        details.playTimes?.let { TimeToBeatCard(it, placeholder = placeholder) }
                        if (details.stores.isNotEmpty()) stores()
                    }
                }
            },
            modifier = Modifier.pageColumn(gutter).fillMaxWidth(),
        )
    } else {
        val readingColumn = Modifier.pageColumn(gutter).widthIn(max = Dimens.readableTextWidth)
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.large)) {
            if (hasOverview) {
                Column(readingColumn) { overview() }
            }
            if (!media.gallery.isEmpty) {
                DetailsMedia(
                    details.name,
                    media,
                    gutter,
                    placeholder,
                    onWatchOnYouTube,
                    modifier = Modifier.pageWidth(gutter),
                    scrollState = scrollState,
                )
            }
            if (!details.facts.isEmpty || details.playTimes != null) {
                Column(readingColumn, verticalArrangement = Arrangement.spacedBy(Dimens.medium)) {
                    if (!details.facts.isEmpty) about()
                    details.playTimes?.let { TimeToBeatCard(it, placeholder = placeholder) }
                }
            }
            if (details.storyline.isNotEmpty()) {
                Column(readingColumn) { storyline() }
            }
            if (details.stores.isNotEmpty()) {
                Column(readingColumn) { stores() }
            }
        }
    }
}

@Composable
private fun DetailsMedia(
    gameName: String,
    media: MediaGalleryState,
    gutter: Dp,
    placeholder: Boolean,
    onWatchOnYouTube: (GameMedia.YouTubeVideo) -> Unit,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
) {
    MediaGallery(
        state = media,
        gameName = gameName,
        onWatchOnYouTube = onWatchOnYouTube,
        modifier = modifier,
        placeholder = placeholder,
        horizontalPadding = gutter,
        // Measured from the window's top: in the desktop layout the header is above the page's bar.
        coveredTop = Dimens.heroTopBarHeightWithInsets + if (desktopLayout() != null) DesktopHeaderHeight else 0.dp,
        scrollState = scrollState,
    )
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
@Composable
private fun VersionSelectorBlock(
    selector: VersionSelector?,
    compact: Boolean,
    gutter: Dp,
    onSelect: (Int) -> Unit,
    versionRoute: (Int) -> NavKey,
) {
    selector ?: return
    GameVersionSelector(
        selector = selector,
        compact = compact,
        onSelect = onSelect,
        versionRoute = versionRoute,
        modifier = Modifier.pageColumn(gutter).widthIn(max = Dimens.readableTextWidth),
    )
    Spacer(modifier = Modifier.size(Dimens.large))
}

/**
 * One section for every related release: the heading or tabs, then the selected kind as compact rows, one to a line
 * in a [compact] layout and as many as fit otherwise, from [WIDE_RELATED_COLUMNS] to [RELATED_COLUMNS_MAX].
 * The API caps each group. A missing [category] opens the first kind.
 */
@Suppress("LongParameterList")
@Composable
private fun RelatedReleases(
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
    Spacer(modifier = Modifier.size(Dimens.large))
    RelatedHeader(
        groups = groups,
        selected = group.category,
        compact = compact,
        onSelect = onCategoryChange,
        modifier = Modifier.pageColumn(gutter).padding(bottom = Dimens.small),
    )
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val columns = if (compact) 1 else relatedColumnsFor(pageContentWidth(maxWidth, gutter))
        RelatedGrid(
            games = group.games,
            columns = columns,
            expanded = expanded,
            gutter = gutter,
            onToggleExpanded = onToggleExpanded,
            onGameClick = onGameClick,
        )
    }
}

/**
 * Related releases in [columns], and "Show all" while some are hidden. Collapsed, the list shows [RELATED_COLLAPSED]
 * releases, or its whole first line when that is longer.
 */
@Suppress("LongParameterList")
@Composable
private fun RelatedGrid(
    games: List<RelatedGame>,
    columns: Int,
    expanded: Boolean,
    gutter: Dp,
    onToggleExpanded: () -> Unit,
    onGameClick: (Int) -> Unit,
) {
    val collapsed = maxOf(RELATED_COLLAPSED, columns)
    val shown = if (expanded) games else games.take(collapsed)
    Column {
        for (line in shown.chunked(columns)) {
            Row(
                modifier = Modifier.pageColumn(gutter),
                horizontalArrangement = Arrangement.spacedBy(Dimens.large),
            ) {
                line.forEach { game ->
                    key(game.id) { RelatedGameRow(game = game, onClick = onGameClick, modifier = Modifier.weight(1f)) }
                }
                repeat(columns - line.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
        if (games.size > collapsed) {
            DisclosureButton(
                label =
                    if (expanded) {
                        stringResource(Res.string.game_details_show_fewer)
                    } else {
                        stringResource(Res.string.game_details_show_all, games.size)
                    },
                expanded = expanded,
                onClick = onToggleExpanded,
                modifier = Modifier.pageColumn(gutter).padding(top = Dimens.small),
            )
        }
    }
}

/** How many related releases fit side by side in [width]: at least two, and at most [RELATED_COLUMNS_MAX]. */
private fun relatedColumnsFor(width: Dp): Int =
    ((width + Dimens.large) / (RelatedRowMinWidth + Dimens.large))
        .toInt()
        .coerceIn(WIDE_RELATED_COLUMNS, RELATED_COLUMNS_MAX)

/** Similar games while they load and once there are any; a failure hides the section instead. */
@Composable
private fun SimilarGames(
    recommendations: RecommendationsState,
    gutter: Dp,
    onGameClick: (Int) -> Unit,
) {
    val showRecommendations =
        recommendations is RecommendationsState.Loading ||
            (recommendations is RecommendationsState.Success && recommendations.games.isNotEmpty())
    if (!showRecommendations) return
    Spacer(modifier = Modifier.size(Dimens.large))
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

/**
 * Keeps a collapsing text's disclosure in view on desktop: it moves up by the removed height, so the viewport moves by
 * the same amount. Returns whether it could.
 */
private fun anchorOnCollapse(scrollState: ScrollState): suspend (Int) -> Boolean =
    { height ->
        var anchored = false
        scrollState.scroll(MutatePriority.UserInput) {
            if (scrollState.value >= height) {
                scrollBy(-height.toFloat())
                anchored = true
            }
        }
        anchored
    }

/** [content] at most the readable width, at the start of a wider column. */
@Composable
private fun ReadableWidth(content: @Composable () -> Unit) {
    Column(modifier = Modifier.widthIn(max = Dimens.readableTextWidth)) { content() }
}

private const val PREVIEW_CRITIC_SCORE = 86
private const val PREVIEW_EXPANSION_ID = 3
private const val PREVIEW_RELEASE_YEAR = 2026
private const val PREVIEW_NORTHBOUND_ID = 4
private const val PREVIEW_LAST_BEACON_ID = 5

private val PreviewDetails =
    GameDetails(
        id = 1,
        name = "Signal Valley",
        summary = "Explore a remote valley where ruins hold clues to the people who lived there.",
        storyline = "A radio signal leads a surveyor to a mountain town abandoned decades ago.",
        coverImageUrl = "",
        backdrop = null,
        releaseDate = "October 8, 2026",
        platforms = listOf(ParentPlatformType.PC, ParentPlatformType.PlayStation, ParentPlatformType.Xbox),
        stores = emptyList(),
        genres = listOf(GenreTag(1, "Adventure"), GenreTag(2, "Puzzle")),
        badges = listOf(GameBadge.Score(Res.string.about_critic, PREVIEW_CRITIC_SCORE)),
        facts = GameFactsModel(rows = listOf(GameFact(Res.string.about_developer, "Sample Studio"))),
        playTimes =
            GamePlayTimes(
                estimates =
                    listOf(
                        PlayTime(Res.string.about_main, hours = 12, relativeLength = 0.5f),
                        PlayTime(Res.string.about_complete, hours = 24, relativeLength = 1f),
                    ),
                sampleCount = 42,
            ),
        relations =
            GameRelations(
                versions =
                    VersionSelector(
                        options =
                            listOf(
                                VersionOption(id = 1, label = null, kind = GameKind.GAME),
                                VersionOption(id = 2, label = "Deluxe Edition", kind = GameKind.EDITION),
                            ),
                        selectedId = 1,
                    ),
                related =
                    listOf(
                        RelatedGroup(
                            category = RelatedCategory.EXTRAS,
                            games =
                                listOf(
                                    RelatedGame(
                                        id = PREVIEW_EXPANSION_ID,
                                        title = "Signal Valley: High Pass",
                                        kind = GameKind.EXPANSION,
                                        year = PREVIEW_RELEASE_YEAR,
                                        coverUrl = "",
                                    ),
                                ),
                        ),
                    ),
            ),
    )

private val PreviewRecommendations =
    RecommendationsState.Success(
        listOf(
            GameCardModel(PREVIEW_NORTHBOUND_ID, "Northbound", ""),
            GameCardModel(PREVIEW_LAST_BEACON_ID, "Last Beacon", ""),
        ),
    )

@Preview(name = "Phone", group = "Game details", widthDp = 390, heightDp = 844)
@Preview(name = "Tablet", group = "Game details", widthDp = 768, heightDp = 1024)
@Composable
internal fun GameDetailsScreenPreview() {
    PreviewGameDetailsContent(GameDetailsState.Success(PreviewDetails), PreviewRecommendations)
}

@Preview(name = "Desktop", group = "Game details", widthDp = 1280, heightDp = 960)
@Composable
internal fun GameDetailsDesktopPreview() {
    PreviewGameDetailsContent(GameDetailsState.Success(PreviewDetails), PreviewRecommendations, desktop = true)
}

@Preview(name = "Adult game", group = "Game details", widthDp = 390, heightDp = 844)
@Composable
internal fun GameDetailsAdultPreview() {
    val details =
        PreviewDetails.copy(
            name = "Sample adult game",
            summary = "",
            storyline = "",
            badges = listOf(GameBadge.Adult),
            relations = GameRelations(),
            isAdult = true,
        )
    PreviewGameDetailsContent(GameDetailsState.Success(details))
}

@Preview(name = "Loading", group = "Game details", widthDp = 390, heightDp = 844)
@Composable
internal fun GameDetailsLoadingPreview() {
    PreviewGameDetailsContent(GameDetailsState.Loading)
}

@Preview(name = "Not found", group = "Game details", widthDp = 390, heightDp = 844)
@Composable
internal fun GameDetailsNotFoundPreview() {
    PreviewGameDetailsContent(GameDetailsState.NotFound)
}

@Preview(name = "Network error", group = "Game details", widthDp = 390, heightDp = 844)
@Composable
internal fun GameDetailsErrorPreview() {
    PreviewGameDetailsContent(GameDetailsState.Error(DataError.Network))
}

@Composable
private fun PreviewGameDetailsContent(
    state: GameDetailsState,
    recommendations: RecommendationsState = RecommendationsState.Unavailable,
    desktop: Boolean = false,
) {
    BraindanceTheme {
        CompositionLocalProvider(LocalDesktopLayoutEnabled provides desktop) {
            GameDetailsContent(
                route = GameDetailsRoute(PreviewDetails.id),
                state = state,
                recommendations = recommendations,
                modifier = Modifier.background(background),
                onBack = {},
                onRetry = {},
                onOpenGame = {},
                onSelectVersion = {},
            )
        }
    }
}

@Preview(name = "Details body", group = "Game details", widthDp = 1000)
@Composable
internal fun DetailsBodyPreview() {
    BraindanceTheme {
        DetailsBody(
            details = PreviewDetails,
            media = rememberMediaGalleryState(PreviewDetails.media),
            gutter = Dimens.medium,
            desktop = true,
            placeholder = false,
        )
    }
}
