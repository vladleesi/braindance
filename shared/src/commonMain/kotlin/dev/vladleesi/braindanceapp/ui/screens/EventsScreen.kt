// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.event_dates_range
import dev.vladleesi.braindanceapp.resources.event_empty
import dev.vladleesi.braindanceapp.resources.event_games_title
import dev.vladleesi.braindanceapp.resources.event_loading
import dev.vladleesi.braindanceapp.resources.event_unavailable
import dev.vladleesi.braindanceapp.resources.event_watch
import dev.vladleesi.braindanceapp.resources.home_events_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.ButtonMinHeight
import dev.vladleesi.braindanceapp.ui.components.CardGrid
import dev.vladleesi.braindanceapp.ui.components.LOADING_ROWS
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.PagedGridCardWidth
import dev.vladleesi.braindanceapp.ui.components.SectionEmpty
import dev.vladleesi.braindanceapp.ui.components.SectionError
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TextLineSkeleton
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.home.EventCard
import dev.vladleesi.braindanceapp.ui.components.home.EventLogo
import dev.vladleesi.braindanceapp.ui.components.home.EventLogoSkeleton
import dev.vladleesi.braindanceapp.ui.components.home.EventStatusTag
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.home.PreviewEvent
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageSidePadding
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.EventCardModel
import dev.vladleesi.braindanceapp.ui.viewmodels.EventDetail
import dev.vladleesi.braindanceapp.ui.viewmodels.EventStatus
import dev.vladleesi.braindanceapp.ui.viewmodels.EventsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import dev.vladleesi.braindanceapp.utils.formatEventDate
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.braindanceapp.utils.rememberCachedLazyGridState
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlin.time.Instant

private const val HEADER_KEY = "event-header"
private const val SECTION_TITLE_KEY = "event-games-title"
private const val MESSAGE_KEY = "event-games-message"
private const val SKELETON_STATUS_FRACTION = 0.23f
private const val SKELETON_NAME_FRACTION = 0.76f
private const val SKELETON_WATCH_FRACTION = 0.4f
private const val SKELETON_SECTION_TITLE_FRACTION = 0.25f
private val SkeletonSampleDate = Instant.parse("2026-11-21T20:20:00Z")

@Composable
internal fun EventsScreen(
    route: EventsRoute,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val viewModel: EventsViewModel = koinViewModel(parameters = { parametersOf(route) })
    LaunchedEffect(viewModel) { viewModel.load() }

    if (route.id == null) {
        val events by viewModel.events.collectAsStateWithLifecycle()
        Column(modifier.fillMaxSize()) {
            SpacerStatusBarInsets()
            TopAppBar(
                title = stringResource(Res.string.home_events_title),
                titleStyle = MaterialTheme.typography.h2,
                showBackButton = true,
                onBackButtonPressed = { navigator.goBack() },
            )
            EventsList(events, viewModel::refresh, onOpenEvent = { navigator.navigate(EventsRoute(it)) })
        }
    } else {
        val detail by viewModel.detail.collectAsStateWithLifecycle()
        val uriHandler = LocalUriHandler.current
        EventDetails(
            detail = detail,
            onRetry = viewModel::refresh,
            onWatch = { uriHandler.openExternalUrl(it) },
            onOpenGame = { navigator.navigate(GameDetailsRoute(it)) },
            onBack = { navigator.goBack() },
            modifier = modifier,
        )
    }
}

@Composable
private fun EventsList(
    state: SectionState<EventCardModel>,
    onRetry: () -> Unit,
    onOpenEvent: (Int) -> Unit,
) {
    val gutter = pageGutter()
    val grid = rememberLazyGridState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            GridCells.Adaptive(Dimens.giveAwayCardWidth),
            modifier = Modifier.fillMaxSize(),
            state = grid,
            contentPadding =
                PaddingValues(
                    horizontal = pageSidePadding(maxWidth, gutter),
                    vertical = Dimens.medium,
                ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.medium),
        ) {
            when (state) {
                SectionState.Loading -> item { CircularProgressIndicator() }
                is SectionState.Error ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionError(state.error.userMessage(), onRetry)
                    }
                is SectionState.Success -> {
                    if (state.items.isEmpty()) item { SectionEmpty() }
                    items(state.items, key = { it.id }, contentType = { "event" }) { event ->
                        EventCard(event, { onOpenEvent(event.id) })
                    }
                }
            }
        }
        PageScrollbar(grid)
    }
}

@Composable
@Suppress("LongParameterList")
private fun EventDetails(
    detail: EventDetail,
    onRetry: () -> Unit,
    onWatch: (String) -> Unit,
    onOpenGame: (Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val event =
        when (detail) {
            is EventDetail.Ready -> detail.event
            is EventDetail.Error -> detail.event
            else -> null
        }
    val grid = rememberCachedLazyGridState()
    Box(modifier.fillMaxSize()) {
        if (detail == EventDetail.Loading) {
            EventDetailsSkeleton()
        } else {
            val topInset = Dimens.topBarHeightWithInsets
            CardGrid(
                modifier = Modifier,
                cardWidth = PagedGridCardWidth.Games,
                state = grid,
                topPadding = topInset,
                scrollbarTopInset = topInset,
            ) {
                if (event != null) {
                    item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = HEADER_KEY) {
                        EventHeader(event, onWatch)
                    }
                }
                when (detail) {
                    EventDetail.Loading -> Unit
                    EventDetail.Unavailable ->
                        item(key = MESSAGE_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = "unavailable") {
                            Text(stringResource(Res.string.event_unavailable), color = secondaryText)
                        }
                    is EventDetail.Error ->
                        item(key = MESSAGE_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = "error") {
                            SectionError(detail.error.userMessage(), onRetry)
                        }
                    is EventDetail.Ready -> {
                        item(
                            key = SECTION_TITLE_KEY,
                            span = { GridItemSpan(maxLineSpan) },
                            contentType = SECTION_TITLE_KEY,
                        ) {
                            Text(
                                stringResource(Res.string.event_games_title),
                                style = MaterialTheme.typography.h3,
                                modifier = Modifier.semantics { heading() },
                            )
                        }
                        if (detail.games.isEmpty()) {
                            item(key = MESSAGE_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                                if (event?.status == EventStatus.UPCOMING) {
                                    Text(stringResource(Res.string.event_empty), color = secondaryText)
                                } else {
                                    SectionEmpty()
                                }
                            }
                        }
                        items(detail.games, key = { it.id }, contentType = { "game" }) {
                            GameCard(it, onOpenGame, showTitle = true, titleLines = 2)
                        }
                    }
                }
            }
        }
        EventDetailsTopBar(event, grid, loading = detail == EventDetail.Loading, onBack = onBack)
    }
}

@Composable
private fun EventDetailsTopBar(
    event: EventCardModel?,
    grid: LazyGridState,
    loading: Boolean,
    onBack: () -> Unit,
) {
    val (barColor, chipColor, titleColor) =
        when {
            loading -> Triple(Color.Transparent, navBar, Color.Transparent)
            event == null -> Triple(navBar, Color.Transparent, white)
            else -> grid.calculateScrollTopBarColors()
        }
    val title =
        when {
            titleColor.alpha == 0f -> ""
            event != null -> event.name
            else -> stringResource(Res.string.home_events_title)
        }
    Column {
        StatusBarOverlay(backgroundColor = barColor)
        TopAppBar(
            tabBarColor = barColor,
            title = title,
            subtitle = event?.let { eventDates(it.startDate, it.endDate) }.orEmpty(),
            titleColor = titleColor,
            titleStyle = MaterialTheme.typography.subtitle1.copy(fontWeight = FontWeight.SemiBold),
            showBackButton = true,
            backButtonBackgroundColor = chipColor,
            onBackButtonPressed = onBack,
            actions =
                if (event != null && showsShareActions()) {
                    { ShareButton(ShareTarget.Event(event.id, event.name), backgroundColor = chipColor) }
                } else {
                    null
                },
        )
    }
}

@Composable
private fun EventDetailsSkeleton() {
    val loadingLabel = stringResource(Res.string.event_loading)
    CardGrid(
        modifier =
            Modifier.semantics {
                contentDescription = loadingLabel
                liveRegion = LiveRegionMode.Polite
            },
        cardWidth = PagedGridCardWidth.Games,
        userScrollEnabled = false,
        topPadding = Dimens.topBarHeightWithInsets,
    ) { columns ->
        item(key = HEADER_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = "header-skeleton") {
            EventHeaderSkeleton()
        }
        item(key = SECTION_TITLE_KEY, span = { GridItemSpan(maxLineSpan) }, contentType = "title-skeleton") {
            TextLineSkeleton(MaterialTheme.typography.h3, widthFraction = SKELETON_SECTION_TITLE_FRACTION)
        }
        items(columns * LOADING_ROWS, key = { "game-skeleton-$it" }, contentType = { "game-skeleton" }) {
            GameCardSkeleton(showTitle = true, titleLines = 2)
        }
    }
}

@Composable
private fun EventHeader(
    event: EventCardModel,
    onWatch: (String) -> Unit,
) {
    EventHeaderLayout(logo = { EventLogo(event.logoUrl, it) }) { modifier, fullWidthWatch ->
        EventHeaderText(event, onWatch, modifier, fullWidthWatch)
    }
}

@Composable
private fun EventHeaderSkeleton() {
    EventHeaderLayout(logo = { EventLogoSkeleton(it) }) { modifier, fullWidthWatch ->
        EventHeaderTextSkeleton(modifier, fullWidthWatch)
    }
}

@Composable
private fun EventHeaderLayout(
    logo: @Composable (Modifier) -> Unit,
    text: @Composable (Modifier, fullWidthWatch: Boolean) -> Unit,
) {
    val desktop = desktopLayout() != null
    BoxWithConstraints(
        Modifier
            .widthIn(max = Dimens.readableTextWidth)
            .fillMaxWidth()
            .background(surface, CornerShapes.large)
            .padding(Dimens.medium),
    ) {
        if (desktop && maxWidth >= Dimens.giveAwayCardDetailsWidth + Dimens.giveAwayCardWidth + Dimens.medium) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.medium),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                logo(Modifier.width(Dimens.giveAwayCardDetailsWidth))
                text(Modifier.weight(1f), false)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.medium)) {
                logo(Modifier.fillMaxWidth())
                text(Modifier.fillMaxWidth(), !desktop)
            }
        }
    }
}

@Composable
private fun EventHeaderTextSkeleton(
    modifier: Modifier = Modifier,
    fullWidthWatch: Boolean = false,
) {
    val sampleDate = remember { formatEventDate(SkeletonSampleDate.epochSeconds, timeOnly = false) }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        TextLineSkeleton(
            MaterialTheme.typography.caption,
            widthFraction = SKELETON_STATUS_FRACTION,
            modifier = Modifier.clip(CornerShapes.pill).padding(vertical = Dimens.micro),
        )
        TextLineSkeleton(MaterialTheme.typography.h2, widthFraction = SKELETON_NAME_FRACTION)
        TextLineSkeleton(
            MaterialTheme.typography.body2,
            sampleText = stringResource(Res.string.event_dates_range, sampleDate, sampleDate),
        )
        TextLineSkeleton(
            MaterialTheme.typography.button,
            widthFraction = if (fullWidthWatch) 1f else SKELETON_WATCH_FRACTION,
            modifier = Modifier.heightIn(min = ButtonMinHeight).clip(CornerShapes.pill),
        )
    }
}

@Composable
private fun EventHeaderText(
    event: EventCardModel,
    onWatch: (String) -> Unit,
    modifier: Modifier = Modifier,
    fullWidthWatch: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        EventStatusTag(event.status)
        Text(event.name, style = MaterialTheme.typography.h2, modifier = Modifier.semantics { heading() })
        Text(eventDates(event.startDate, event.endDate), style = MaterialTheme.typography.body2, color = secondaryText)
        event.watchUrl?.let { url ->
            TonalButton(
                onClick = { onWatch(url) },
                modifier = if (fullWidthWatch) Modifier.fillMaxWidth() else Modifier,
            ) { Text(stringResource(Res.string.event_watch)) }
        }
    }
}

@Composable
private fun eventDates(
    start: String,
    end: String?,
): String = end?.let { stringResource(Res.string.event_dates_range, start, it) } ?: start

@Preview
@Composable
internal fun EventsListPreview() {
    BraindanceTheme { EventsList(SectionState.Success(listOf(PreviewEvent)), {}, {}) }
}

@Preview
@Composable
internal fun EventDetailsPreview() {
    BraindanceTheme {
        EventDetails(EventDetail.Ready(PreviewEvent, listOf(GameCardModel(1, "Sample Game", ""))), {}, {}, {}, {})
    }
}

@Preview(widthDp = 720)
@Composable
internal fun EventHeaderPreview() {
    BraindanceTheme { EventHeader(PreviewEvent, {}) }
}

@Preview
@Composable
internal fun EventHeaderTextPreview() {
    BraindanceTheme { EventHeaderText(PreviewEvent, {}) }
}

@Preview
@Composable
internal fun EventDetailsTopBarPreview() {
    BraindanceTheme {
        EventDetailsTopBar(
            PreviewEvent,
            rememberLazyGridState(initialFirstVisibleItemIndex = 1),
            loading = false,
            onBack = {},
        )
    }
}

@Preview
@Composable
internal fun EventDetailsSkeletonPreview() {
    BraindanceTheme { EventDetailsSkeleton() }
}

@Preview(widthDp = 720)
@Composable
internal fun EventHeaderSkeletonPreview() {
    BraindanceTheme { EventHeaderSkeleton() }
}

@Preview(widthDp = 720)
@Composable
internal fun EventHeaderLayoutPreview() {
    BraindanceTheme { EventHeaderSkeleton() }
}

@Preview
@Composable
internal fun EventHeaderTextSkeletonPreview() {
    BraindanceTheme { EventHeaderTextSkeleton() }
}
