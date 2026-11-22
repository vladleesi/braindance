// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.event_dates_range
import dev.vladleesi.braindanceapp.resources.event_empty
import dev.vladleesi.braindanceapp.resources.event_games_title
import dev.vladleesi.braindanceapp.resources.event_unavailable
import dev.vladleesi.braindanceapp.resources.event_watch
import dev.vladleesi.braindanceapp.resources.home_events_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.SectionEmpty
import dev.vladleesi.braindanceapp.ui.components.SectionError
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TonalButton
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.home.EventCard
import dev.vladleesi.braindanceapp.ui.components.home.EventLogo
import dev.vladleesi.braindanceapp.ui.components.home.EventStatusTag
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.PreviewEvent
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.pageSidePadding
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.viewmodels.EventCardModel
import dev.vladleesi.braindanceapp.ui.viewmodels.EventDetail
import dev.vladleesi.braindanceapp.ui.viewmodels.EventStatus
import dev.vladleesi.braindanceapp.ui.viewmodels.EventsViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
internal fun EventsScreen(
    route: EventsRoute,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val viewModel: EventsViewModel = koinViewModel(parameters = { parametersOf(route) })
    LaunchedEffect(viewModel) { viewModel.load() }

    Column(modifier.fillMaxSize()) {
        SpacerStatusBarInsets()
        TopAppBar(
            title = stringResource(Res.string.home_events_title),
            titleStyle = MaterialTheme.typography.h2,
            showBackButton = true,
            onBackButtonPressed = { navigator.goBack() },
        )
        if (route.id == null) {
            val events by viewModel.events.collectAsStateWithLifecycle()
            EventsList(events, viewModel::refresh, onOpenEvent = { navigator.navigate(EventsRoute(it)) })
        } else {
            val detail by viewModel.detail.collectAsStateWithLifecycle()
            val uriHandler = LocalUriHandler.current
            EventDetails(
                detail,
                viewModel::refresh,
                onWatch = { uriHandler.openExternalUrl(it) },
                onOpenGame = { navigator.navigate(GameDetailsRoute(it)) },
            )
        }
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
private fun EventDetails(
    detail: EventDetail,
    onRetry: () -> Unit,
    onWatch: (String) -> Unit,
    onOpenGame: (Int) -> Unit,
) {
    val gutter = pageGutter()
    val event =
        when (detail) {
            is EventDetail.Ready -> detail.event
            is EventDetail.Error -> detail.event
            else -> null
        }
    val grid = rememberLazyGridState()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            GridCells.Adaptive(Dimens.gameCardWidth),
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
            event?.let {
                item(span = { GridItemSpan(maxLineSpan) }) { EventHeader(it, onWatch) }
            }
            when (detail) {
                EventDetail.Loading -> item { CircularProgressIndicator() }
                EventDetail.Unavailable ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(stringResource(Res.string.event_unavailable), color = secondaryText)
                    }
                is EventDetail.Error ->
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        SectionError(detail.error.userMessage(), onRetry)
                    }
                is EventDetail.Ready -> {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Text(
                            stringResource(Res.string.event_games_title),
                            style = MaterialTheme.typography.h3,
                            modifier = Modifier.semantics { heading() },
                        )
                    }
                    if (detail.games.isEmpty()) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
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
        PageScrollbar(grid)
    }
}

@Composable
private fun EventHeader(
    event: EventCardModel,
    onWatch: (String) -> Unit,
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
                EventLogo(event.logoUrl, Modifier.width(Dimens.giveAwayCardDetailsWidth))
                EventHeaderText(event, onWatch, Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.medium)) {
                EventLogo(event.logoUrl, Modifier.fillMaxWidth())
                EventHeaderText(event, onWatch, Modifier.fillMaxWidth(), fullWidthWatch = !desktop)
            }
        }
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
        val dates =
            event.endDate?.let { stringResource(Res.string.event_dates_range, event.startDate, it) }
                ?: event.startDate
        Text(dates, style = MaterialTheme.typography.body2, color = secondaryText)
        event.watchUrl?.let { url ->
            TonalButton(
                onClick = { onWatch(url) },
                modifier = if (fullWidthWatch) Modifier.fillMaxWidth() else Modifier,
            ) { Text(stringResource(Res.string.event_watch)) }
        }
    }
}

@Preview
@Composable
internal fun EventsListPreview() {
    BraindanceTheme { EventsList(SectionState.Success(listOf(PreviewEvent)), {}, {}) }
}

@Preview
@Composable
internal fun EventDetailsPreview() {
    BraindanceTheme {
        EventDetails(EventDetail.Ready(PreviewEvent, listOf(GameCardModel(1, "Sample Game", ""))), {}, {}, {})
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
