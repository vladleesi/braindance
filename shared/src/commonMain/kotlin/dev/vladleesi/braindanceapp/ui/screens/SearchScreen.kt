// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import ChevronLeft
import Search
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.vladleesi.braindanceapp.ui.components.SearchBar
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.clickable

@Composable
fun SearchScreen(modifier: Modifier) {
    var isFocused by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val navigator = LocalNavigator.current

    val searchTabReselected by navigator.resultsFlow(BottomBarRoute.SearchRoute.RESULT_KEY).collectAsState(null)

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
        SearchBar(
            modifier =
                Modifier
                    .padding(horizontal = Dimens.medium)
                    .padding(top = Dimens.medium)
                    .focusRequester(searchFocusRequester)
                    .onFocusChanged { focusState ->
                        isFocused = focusState.isFocused
                        if (isFocused.not()) {
                            navigator.setResult(BottomBarRoute.SearchRoute.RESULT_KEY, false)
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
