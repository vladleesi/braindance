// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Plus
import Search
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.library_create_list_coming_soon
import dev.vladleesi.braindanceapp.resources.library_custom_lists_description
import dev.vladleesi.braindanceapp.resources.library_custom_lists_title
import dev.vladleesi.braindanceapp.resources.library_search_coming_soon
import dev.vladleesi.braindanceapp.resources.library_status_backlog
import dev.vladleesi.braindanceapp.resources.library_status_completed
import dev.vladleesi.braindanceapp.resources.library_status_playing
import dev.vladleesi.braindanceapp.resources.library_status_wishlist
import dev.vladleesi.braindanceapp.resources.library_statuses_description
import dev.vladleesi.braindanceapp.resources.library_statuses_title
import dev.vladleesi.braindanceapp.resources.navigation_library
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.ComingSoonLabel
import dev.vladleesi.braindanceapp.ui.components.SpacerStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.TopLevelTopAppBar
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.white
import org.jetbrains.compose.resources.stringResource

private const val STATUSES_PER_ROW = 2

// Material's floating action button size.
private val fabSize = 56.dp

@Composable
fun LibraryScreen(modifier: Modifier) {
    val navigator = LocalNavigator.current
    // The scaffold's content area ends above the bottom bar and beside the rail, and already respects window insets.
    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
            SpacerStatusBarInsets()
            TopLevelTopAppBar(
                title = stringResource(Res.string.navigation_library),
                onProfileClick = { navigator.navigate(ProfileRoute) },
                actions = {
                    ComingSoonAction(
                        imageVector = Search,
                        label = stringResource(Res.string.library_search_coming_soon),
                    )
                },
            )
            Column(
                modifier = Modifier.padding(horizontal = Dimens.medium),
                verticalArrangement = Arrangement.spacedBy(Dimens.large),
            ) {
                StatusSection()
                ComingSoonCard(
                    title = stringResource(Res.string.library_custom_lists_title),
                    description = stringResource(Res.string.library_custom_lists_description),
                )
            }
            // Lets the last section scroll clear of the button.
            Spacer(Modifier.height(fabSize + Dimens.large))
        }
        CreateListButton(
            modifier =
                Modifier
                    .align(Alignment.BottomEnd)
                    .padding(Dimens.medium),
        )
    }
}

/** Game statuses, kept apart from custom lists: each game has one status but can be in any number of lists. */
@Composable
private fun StatusSection() {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        ComingSoonLabel()
        Text(
            text = stringResource(Res.string.library_statuses_title),
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.h3,
        )
        Text(
            text = stringResource(Res.string.library_statuses_description),
            style = MaterialTheme.typography.caption,
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            maxItemsInEachRow = STATUSES_PER_ROW,
            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
            verticalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            StatusCard(stringResource(Res.string.library_status_playing), Modifier.weight(1f))
            StatusCard(stringResource(Res.string.library_status_completed), Modifier.weight(1f))
            StatusCard(stringResource(Res.string.library_status_backlog), Modifier.weight(1f))
            StatusCard(stringResource(Res.string.library_status_wishlist), Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatusCard(
    status: String,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(Dimens.medium),
        color = secondary,
    ) {
        Text(
            text = status,
            modifier = Modifier.padding(Dimens.medium),
            style = MaterialTheme.typography.subtitle1,
        )
    }
}

/** Shown disabled until list creation exists; Material's FloatingActionButton cannot be disabled. */
@Composable
private fun CreateListButton(modifier: Modifier) {
    Surface(
        modifier = modifier,
        shape = CircleShape,
        color = secondary,
        elevation = Dimens.small,
    ) {
        IconButton(onClick = {}, modifier = Modifier.size(fabSize), enabled = false) {
            Icon(
                imageVector = Plus,
                contentDescription = stringResource(Res.string.library_create_list_coming_soon),
                tint = white.copy(alpha = ContentAlpha.disabled),
            )
        }
    }
}
