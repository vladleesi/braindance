// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import ChevronLeft
import Search
import SlidersHorizontal
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
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
import dev.vladleesi.braindanceapp.resources.navigation_discover
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.PlaceholderChips
import dev.vladleesi.braindanceapp.ui.components.SearchBar
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.clickable
import org.jetbrains.compose.resources.stringResource

@Composable
fun DiscoverScreen(modifier: Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val navigator = LocalNavigator.current

    val searchTabReselected by navigator.resultsFlow(BottomBarRoute.DiscoverRoute.RESULT_KEY).collectAsState(null)

    LaunchedEffect(searchTabReselected) {
        if (searchTabReselected == true) {
            searchFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    val onSearchIconClick: () -> Unit = {
        if (isFocused) {
            keyboardController?.hide()
            isFocused = false
            focusManager.clearFocus()
        }
    }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().background(background)) {
        SpacerStatusBarInsets()
        TopLevelTopAppBar(
            title = stringResource(Res.string.navigation_discover),
            onProfileClick = { navigator.navigate(ProfileRoute) },
        )
        Row(
            modifier = Modifier.padding(start = Dimens.medium, end = Dimens.tiny),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SearchBar(
                modifier =
                    Modifier
                        .weight(1f)
                        .focusRequester(searchFocusRequester)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            if (isFocused.not()) {
                                navigator.setResult(BottomBarRoute.DiscoverRoute.RESULT_KEY, false)
                            }
                        },
                leadingIcon = {
                    SearchIcon(visible = !isFocused, imageVector = Search, onClick = onSearchIconClick)
                    SearchIcon(visible = isFocused, imageVector = ChevronLeft, onClick = onSearchIconClick)
                },
                trailingIcon = {
                    SearchLoadingIcon(visible = isLoading)
                },
                onQueryChange = { query ->
                },
                onSearch = { query ->
                    searchFocusRequester.freeFocus()
                    keyboardController?.hide()
                    isLoading = true
                },
            )
            ComingSoonAction(
                imageVector = SlidersHorizontal,
                label = stringResource(Res.string.discover_advanced_filters_coming_soon),
            )
        }
        DiscoverPlaceholders(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SearchIcon(
    visible: Boolean,
    imageVector: ImageVector,
    onClick: () -> Unit,
) {
    AnimatedVisibility(visible = visible) {
        Icon(
            imageVector = imageVector,
            contentDescription = null,
            modifier =
                Modifier
                    .clickable(
                        rippleEnabled = false,
                        onClick = onClick,
                    ),
        )
    }
}

@Composable
private fun SearchLoadingIcon(visible: Boolean) {
    AnimatedVisibility(visible = visible) {
        CircularProgressIndicator(
            modifier = Modifier.padding(horizontal = Dimens.medium).size(24.dp),
            color = white,
            strokeWidth = Dimens.micro,
        )
    }
}

@Composable
private fun DiscoverPlaceholders(modifier: Modifier) {
    Column(
        modifier =
            modifier
                .verticalScroll(rememberScrollState())
                .padding(Dimens.medium),
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
