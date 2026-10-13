// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_about
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_free
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_get_giveaway
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_instruction
import dev.vladleesi.braindanceapp.system.isLargeDevice
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PlatformLogoList
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.components.StaticText
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.icons.OpenInNew
import dev.vladleesi.braindanceapp.ui.style.icons.Users
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetailsViewModel
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import dev.vladleesi.braindanceapp.utils.giveawayPlatforms
import dev.vladleesi.braindanceapp.utils.orZero
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GiveawayDetailsScreen(
    id: Int,
    modifier: Modifier,
) {
    val viewModel: GiveawayDetailsViewModel =
        koinViewModel(
            key = "giveaway_details_$id",
            parameters = { parametersOf(id) },
        )

    val navigator = LocalNavigator.current

    val giveawayId by rememberSaveable {
        mutableStateOf(id)
    }
    val state by viewModel.giveawayDetailsState.collectAsState()

    LaunchedEffect(giveawayId) {
        viewModel.load()
    }

    when (val currentState = state) {
        is GiveawayDetailsState.Error ->
            ErrorScreen(errorMessage = currentState.message, modifier = modifier) {
                viewModel.refresh()
            }

        GiveawayDetailsState.Loading ->
            GlobalLoading(modifier = modifier)

        is GiveawayDetailsState.Success ->
            GiveawayDetailsScreen(
                state = currentState,
                modifier = modifier,
                onBackButtonPressed = { navigator.goBack() },
            )
    }
}

@Composable
fun GiveawayDetailsScreen(
    state: GiveawayDetailsState.Success,
    modifier: Modifier,
    onBackButtonPressed: (() -> Unit),
) {
    Box(
        modifier =
            modifier
                .fillMaxWidth()
                .background(background),
    ) {
        val lazyListState = rememberLazyListState()
        val (topBarColor, backButtonColor, topBarTitleColor) =
            lazyListState.calculateScrollTopBarColors()
        val isLargeDevice = isLargeDevice()

        // TODO: Rework (wip: large devices, timer, platforms, new ui model data class)
        LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = Dimens.medium)) {
            item { SpacerTopBarWithStatusBarInsets() }
            item {
                GiveawayDetailsImage(
                    state = state,
                    modifier = Modifier.padding(top = Dimens.medium),
                )
            }
            item { Spacer(Modifier.height(Dimens.small)) }
            item {
                Text(
                    text = state.giveawayDetails.title.orEmpty(),
                    style = MaterialTheme.typography.h2,
                )
            }
            item { Spacer(Modifier.height(Dimens.small)) }
            item {
                WorthInfo(state = state)
            }
            val claimed = state.giveawayDetails.users.orZero()
            if (claimed > 0) {
                item { Spacer(Modifier.height(Dimens.small)) }
                item {
                    UsersClaimed(claimed, Modifier.fillParentMaxWidth())
                }
            }
            val platforms =
                state.giveawayDetails.platforms
                    ?.giveawayPlatforms()
                    .orEmpty()
            if (platforms.isNotEmpty()) {
                item { Spacer(Modifier.height(Dimens.small)) }
                item {
                    PlatformLogoList(
                        platforms = platforms,
                        imageSize = 20.dp,
                        horizontalSpacing = Dimens.small,
                    )
                }
            }
            item { Spacer(Modifier.height(Dimens.large)) }
            item {
                SummaryText(
                    title = stringResource(Res.string.giveaway_details_screen_about),
                    description = state.giveawayDetails.description.orEmpty(),
                )
            }
            item { Spacer(Modifier.height(Dimens.small)) }
            item {
                SummaryText(
                    title = stringResource(Res.string.giveaway_details_screen_instruction),
                    description = state.giveawayDetails.instructions.orEmpty(),
                )
            }
            item { Spacer(Modifier.height(Dimens.medium)) }
            item { OpenGiveawayButton(url = state.giveawayDetails.openGiveawayUrl.orEmpty()) }
            item { Spacer(Modifier.height(Dimens.large)) }
        }
        Column {
            StatusBarOverlay(backgroundColor = topBarColor)
            TopAppBar(
                tabBarColor = topBarColor,
                titleColor = topBarTitleColor,
                showBackButton = true,
                backButtonBackgroundColor = backButtonColor,
                onBackButtonPressed = onBackButtonPressed,
            )
        }
    }
}

@Composable
private fun GiveawayDetailsImage(
    state: GiveawayDetailsState.Success,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        AsyncImage(
            modifier =
                Modifier
                    .height(Dimens.giveAwayCardDetailsHeight)
                    .width(Dimens.giveAwayCardDetailsWidth)
                    .clip(RoundedCornerShape(Dimens.small)),
            model = state.giveawayDetails.image,
            contentScale = ContentScale.Crop,
            contentDescription = state.giveawayDetails.title?.toContentDescription(),
        )
    }
}

@Composable
private fun WorthInfo(
    state: GiveawayDetailsState.Success,
    modifier: Modifier = Modifier,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = modifier) {
        Text(
            text = stringResource(Res.string.giveaway_details_screen_free),
            color = white,
            style = MaterialTheme.typography.body2,
        )
        val worth =
            state.giveawayDetails.worth
                .takeIf { it != "N/A" }
                .orEmpty()
        if (worth.isNotEmpty()) {
            Spacer(modifier = Modifier.width(Dimens.tiny))
            Text(
                text = worth,
                style = MaterialTheme.typography.body2,
                color = secondaryText,
                textDecoration = TextDecoration.LineThrough,
            )
        }
    }
}

@Composable
private fun UsersClaimed(
    claimed: Int,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier) {
        Icon(
            imageVector = Users,
            contentDescription = "$claimed+ users claimed this".toContentDescription(),
            modifier = Modifier.size(20.dp),
            tint = accent,
        )
        Spacer(Modifier.width(Dimens.tiny))
        Text(
            text = "$claimed+",
            style = MaterialTheme.typography.body2,
            color = accent,
        )
        Spacer(Modifier.width(Dimens.tiny))
        Text(
            text = "Claimed",
            style = MaterialTheme.typography.body2,
            color = white,
        )
    }
}

@Composable
private fun SummaryText(
    title: String,
    description: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        StaticText(
            text = title,
            style = MaterialTheme.typography.h3,
        )
        Spacer(Modifier.height(Dimens.micro))
        SelectionContainer {
            Text(
                text = description,
                style = MaterialTheme.typography.body2,
            )
        }
    }
}

@Composable
private fun OpenGiveawayButton(
    url: String,
    modifier: Modifier = Modifier,
    uriHandler: UriHandler = LocalUriHandler.current,
) {
    Button(
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = secondaryVariant),
        onClick = { uriHandler.openUri(url) },
    ) {
        Row {
            Text(
                text = stringResource(Res.string.giveaway_details_screen_get_giveaway),
                modifier = Modifier.align(Alignment.CenterVertically),
            )
            Spacer(Modifier.width(Dimens.tiny))
            Icon(
                imageVector = OpenInNew,
                contentDescription = url.toContentDescription(),
                tint = white,
                modifier = Modifier.align(Alignment.CenterVertically),
            )
        }
    }
}
