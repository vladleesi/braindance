// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
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
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.system.isLargeWindow
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.DisclosureButton
import dev.vladleesi.braindanceapp.ui.components.FullWindowOverlay
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
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
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.media.GameMedia
import dev.vladleesi.braindanceapp.ui.components.media.MediaGallery
import dev.vladleesi.braindanceapp.ui.components.media.MediaGalleryState
import dev.vladleesi.braindanceapp.ui.components.media.MediaViewer
import dev.vladleesi.braindanceapp.ui.components.media.rememberMediaGalleryState
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageWidth
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GameDetailsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.RecommendationsState
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedCategory
import dev.vladleesi.braindanceapp.ui.viewmodels.RelatedGroup
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.ui.viewmodels.VersionSelector
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
    val uriHandler = LocalUriHandler.current

    val scrollState = rememberScrollState()
    val isLargeWindow = isLargeWindow()
    val desktop = desktopLayout() != null
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
            // New edition titles and wrapped version chips can change the hero's height.
            Column(Modifier.animateContentSize(tween(RELEASE_HEADER_RESIZE_MILLIS))) {
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
                VersionSelectorBlock(
                    selector = state.gameDetails.relations.versions,
                    compact = !isLargeWindow,
                    gutter = gutter,
                    onSelect = selectVersion,
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
                onGameClick = openGame,
            )
            SimilarGames(recommendations, gutter, onGameClick = openGame)
            Spacer(modifier = Modifier.size(Dimens.large))
        }
        PageScrollbar(scrollState, topInset = Dimens.topBarHeightWithInsets)
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
 * is a white icon on a circular backdrop that is a dark chip over the artwork and blends into the bar once it fills.
 */
@Composable
private fun GameDetailsTopBar(
    gameDetails: GameDetails,
    scrollState: ScrollState,
    onBackButtonPressed: () -> Unit,
) {
    val fadeDistance = with(LocalDensity.current) { Dimens.topBarHeightWithInsets.toPx() }
    val fraction by remember(scrollState, fadeDistance) {
        derivedStateOf { (scrollState.value / fadeDistance).coerceIn(0f, 1f) }
    }
    val topBarColor = lerp(Color.Transparent, navBar, fraction)
    val backButtonColor = lerp(navBar, Color.Transparent, fraction)
    val titleColor = lerp(Color.Transparent, white, fraction)
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
    val onCollapse: (suspend (Int) -> Boolean)? =
        if (desktop && scrollState != null) {
            { height ->
                // The disclosure moves up by the removed height. Move the viewport by the same amount.
                var anchored = false
                scrollState.scroll(MutatePriority.UserInput) {
                    if (scrollState.value >= height) {
                        scrollBy(-height.toFloat())
                        anchored = true
                    }
                }
                anchored
            }
        } else {
            null
        }
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
    if (desktop) {
        DetailsColumns(
            primary = {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.large)) {
                    if (details.summary.isNotBlank() || details.overview.isNotEmpty() || details.isAdult) overview()
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
                    if (details.storyline.isNotEmpty()) storyline()
                }
            },
            sidebar = {
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
            if (details.summary.isNotBlank() || details.overview.isNotEmpty() || details.isAdult) {
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
        coveredTop = Dimens.topBarHeightWithInsets,
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
 * in a [compact] layout and [WIDE_RELATED_COLUMNS] otherwise, and "Show all" past [RELATED_COLLAPSED].
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
    val columns = if (compact) 1 else WIDE_RELATED_COLUMNS
    val shown = if (expanded) group.games else group.games.take(RELATED_COLLAPSED)
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
    if (group.games.size > RELATED_COLLAPSED) {
        DisclosureButton(
            label =
                if (expanded) {
                    stringResource(Res.string.game_details_show_fewer)
                } else {
                    stringResource(Res.string.game_details_show_all, group.games.size)
                },
            expanded = expanded,
            onClick = onToggleExpanded,
            modifier = Modifier.pageColumn(gutter).padding(top = Dimens.small),
        )
    }
}

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

// Keep the main content readable when the sidebar fits beside it.
private val MinDetailsColumnWidth = 360.dp
private val DetailsSidebarWidth = 300.dp

/**
 * The main content uses a reading column. About, Time to beat, and stores share a sidebar when both columns fit.
 */
@Composable
private fun DetailsColumns(
    primary: @Composable () -> Unit,
    sidebar: @Composable () -> Unit,
    modifier: Modifier,
) {
    Layout(
        contents = listOf(primary, sidebar),
        modifier = modifier,
    ) { (primaryMeasurables, sidebarMeasurables), constraints ->
        val width = constraints.maxWidth
        val gap = Dimens.large.roundToPx()
        val readable = Dimens.readableTextWidth.roundToPx()
        val sidebarWidth = DetailsSidebarWidth.roundToPx()
        val hasSidebar = sidebarMeasurables.isNotEmpty()
        val beside = hasSidebar && width >= MinDetailsColumnWidth.roundToPx() + gap + sidebarWidth
        val primaryWidth = minOf(readable, if (beside) width - gap - sidebarWidth else width)
        val primaryPlaceable = primaryMeasurables.firstOrNull()?.measure(Constraints(maxWidth = primaryWidth))
        val sidebarMax = if (beside) sidebarWidth else primaryWidth
        val sidebarPlaceable = sidebarMeasurables.firstOrNull()?.measure(Constraints(maxWidth = sidebarMax))
        val primaryHeight = primaryPlaceable?.height ?: 0
        val sidebarHeight = sidebarPlaceable?.height ?: 0
        val height =
            when {
                sidebarPlaceable == null -> primaryHeight
                beside -> maxOf(primaryHeight, sidebarHeight)
                else -> primaryHeight + gap + sidebarHeight
            }
        layout(width, height) {
            primaryPlaceable?.placeRelative(0, 0)
            if (beside) {
                sidebarPlaceable?.placeRelative((primaryPlaceable?.width ?: 0) + gap, 0)
            } else {
                sidebarPlaceable?.placeRelative(0, primaryHeight + gap)
            }
        }
    }
}

private val PreviewBlockHeight = 180.dp

@Preview(widthDp = 1100)
@Composable
internal fun DetailsColumnsPreview() {
    BraindanceTheme {
        DetailsColumns(
            primary = { Box(modifier = Modifier.fillMaxWidth().height(PreviewBlockHeight).background(white)) },
            sidebar = { Box(modifier = Modifier.fillMaxWidth().height(PreviewBlockHeight / 2).background(accent)) },
            modifier = Modifier.background(background).fillMaxWidth(),
        )
    }
}

@Preview(widthDp = 1000)
@Composable
internal fun DetailsBodyPreview() {
    BraindanceTheme {
        val details =
            GameDetails(
                id = 1,
                name = "Sample edition",
                summary = "Explore a remote valley where ruins hold clues to the people who lived there.",
                storyline = "",
                coverImageUrl = "",
                backdrop = null,
                releaseDate = null,
                platforms = emptyList(),
                stores = emptyList(),
                genres = emptyList(),
            )
        DetailsBody(
            details,
            rememberMediaGalleryState(details.media),
            Dimens.medium,
            desktop = true,
            placeholder = false,
        )
    }
}
