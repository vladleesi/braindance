// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import ChevronLeft
import SlidersHorizontal
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.PageTitle
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute.ReleasesRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.navigation_releases
import dev.vladleesi.braindanceapp.resources.releases_filter_all
import dev.vladleesi.braindanceapp.resources.releases_filter_coming_soon
import dev.vladleesi.braindanceapp.resources.releases_filter_events
import dev.vladleesi.braindanceapp.resources.releases_filter_wishlist
import dev.vladleesi.braindanceapp.resources.releases_next_month_coming_soon
import dev.vladleesi.braindanceapp.resources.releases_previous_month_coming_soon
import dev.vladleesi.braindanceapp.resources.releases_this_month
import dev.vladleesi.braindanceapp.resources.releases_upcoming_description
import dev.vladleesi.braindanceapp.resources.releases_upcoming_title
import dev.vladleesi.braindanceapp.system.desktopLayout
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCardMaxWidth
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.PlaceholderChips
import dev.vladleesi.braindanceapp.ui.components.ScrollAwayBarLayout
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.SurfaceTile
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.rememberContentScrollPosition
import dev.vladleesi.braindanceapp.ui.style.Dimens
import org.jetbrains.compose.resources.stringResource

private const val DAYS_IN_WEEK = 7
private const val NEXT_ICON_ROTATION_DEGREES = 180f

/** A UI-only draft of the release calendar: an agenda below a month and week header, with no data yet. */
@Composable
fun ReleasesScreen(modifier: Modifier) {
    val navigator = LocalNavigator.current
    val title = stringResource(Res.string.navigation_releases)
    PageTitle(ReleasesRoute, title)
    val scrollState = rememberScrollState()
    ScrollAwayBarLayout(
        position = rememberContentScrollPosition(scrollState),
        bar = {
            Column {
                SpacerStatusBarInsets()
                TopLevelTopAppBar(
                    title = title,
                    onProfileClick = { navigator.navigate(ProfileRoute) },
                    actions = {
                        ComingSoonAction(
                            imageVector = SlidersHorizontal,
                            label = stringResource(Res.string.releases_filter_coming_soon),
                        )
                    },
                )
            }
        },
        modifier = modifier,
    ) { topInset ->
        Box {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .pageColumn(pageGutter())
                        .padding(top = topInset, bottom = Dimens.medium),
                verticalArrangement = Arrangement.spacedBy(Dimens.medium),
            ) {
                // The desktop layout spans the week across the column, as a calendar's week view does. Elsewhere the
                // month's arrows and days stay together at a readable width.
                val desktop = desktopLayout() != null
                val calendarModifier = if (desktop) Modifier else Modifier.widthIn(max = Dimens.readableTextWidth)
                MonthHeader(modifier = calendarModifier)
                WeekStrip(modifier = calendarModifier)
                PlaceholderChips(
                    labels =
                        listOf(
                            stringResource(Res.string.releases_filter_all),
                            stringResource(Res.string.releases_filter_wishlist),
                            stringResource(Res.string.releases_filter_events),
                        ),
                )
                ComingSoonCard(
                    title = stringResource(Res.string.releases_upcoming_title),
                    description = stringResource(Res.string.releases_upcoming_description),
                    maxWidth = if (desktop) Dp.Unspecified else ComingSoonCardMaxWidth,
                )
            }
            PageScrollbar(scrollState)
        }
    }
}

@Composable
private fun MonthHeader(modifier: Modifier) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        ComingSoonAction(
            imageVector = ChevronLeft,
            label = stringResource(Res.string.releases_previous_month_coming_soon),
        )
        Text(
            text = stringResource(Res.string.releases_this_month),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.h3,
            textAlign = TextAlign.Center,
        )
        ComingSoonAction(
            imageVector = ChevronLeft,
            label = stringResource(Res.string.releases_next_month_coming_soon),
            modifier = Modifier.rotate(NEXT_ICON_ROTATION_DEGREES),
        )
    }
}

/** Empty day cells that outline the week view; dates arrive with the calendar itself. */
@Composable
private fun WeekStrip(modifier: Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.tiny),
    ) {
        repeat(DAYS_IN_WEEK) {
            SurfaceTile(modifier = Modifier.weight(1f).height(Dimens.extraLarge))
        }
    }
}
