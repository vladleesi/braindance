// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.home_screen_popular_latest_giveaways
import dev.vladleesi.braindanceapp.resources.home_screen_popular_most_anticipated
import dev.vladleesi.braindanceapp.resources.home_screen_popular_popular_right_now
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawaysList
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardList
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.overlayBlackWith80Alpha
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import dev.vladleesi.braindanceapp.utils.DelayDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel

@Composable
@OptIn(ExperimentalMaterialApi::class)
fun HomeScreen(
    modifier: Modifier,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val navigator = LocalNavigator.current

    val mostAnticipated by viewModel.mostAnticipated.collectAsState()
    val giveaways by viewModel.giveaways.collectAsState()
    val popularRightNow by viewModel.popularRightNow.collectAsState()

    // Load the screen only on first navigation
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    val scope = rememberCoroutineScope()

    var isRefreshing by remember { mutableStateOf(false) }

    val pullState =
        rememberPullRefreshState(
            refreshing = isRefreshing,
            onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.refresh()
                    delay(DelayDuration.ExtraLarge.millis)
                    isRefreshing = false
                }
            },
        )

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(background),
    ) {
        LazyColumn(
            modifier =
                modifier
                    .fillMaxSize()
                    .pullRefresh(pullState, enabled = true),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            item { SpacerStatusBarInsets() }
            item {
                MiniGameCardList(
                    title = stringResource(Res.string.home_screen_popular_most_anticipated),
                    state = mostAnticipated,
                    onRefresh = { viewModel.loadMostAnticipated() },
                    onItemClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                )
            }
            item {
                GiveawaysList(
                    title = stringResource(Res.string.home_screen_popular_latest_giveaways),
                    state = giveaways,
                    onRefresh = { viewModel.loadGiveaways() },
                    onItemClick = { id -> navigator.navigate(GiveawayDetailsRoute(id)) },
                )
            }
            item {
                MiniGameCardList(
                    title = stringResource(Res.string.home_screen_popular_popular_right_now),
                    state = popularRightNow,
                    onRefresh = { viewModel.loadPopularRightNow() },
                    onItemClick = { id -> navigator.navigate(GameDetailsRoute(id)) },
                )
            }
            item { Spacer(Modifier.height(Dimens.large)) }
        }
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
