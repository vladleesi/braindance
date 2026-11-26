// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.ShareTarget
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.giveaway_details_not_found
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_about
import dev.vladleesi.braindanceapp.resources.giveaway_details_screen_instruction
import dev.vladleesi.braindanceapp.ui.components.GlobalLoading
import dev.vladleesi.braindanceapp.ui.components.PageScrollbar
import dev.vladleesi.braindanceapp.ui.components.ShareButton
import dev.vladleesi.braindanceapp.ui.components.StatusBarOverlay
import dev.vladleesi.braindanceapp.ui.components.TopAppBar
import dev.vladleesi.braindanceapp.ui.components.giveaways.ClaimSteps
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayFactsCard
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayHero
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawaySection
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawaySourceButton
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageContentWidth
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.components.showsShareActions
import dev.vladleesi.braindanceapp.ui.components.userMessage
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetails
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetailsState
import dev.vladleesi.braindanceapp.ui.viewmodels.GiveawayDetailsViewModel
import dev.vladleesi.braindanceapp.utils.calculateScrollTopBarColors
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun GiveawayDetailsScreen(
    id: Int,
    modifier: Modifier,
) {
    val viewModel: GiveawayDetailsViewModel =
        // Scoped to this navigation entry, which clears it when the screen is popped.
        koinViewModel(parameters = { parametersOf(id) })

    val navigator = LocalNavigator.current

    val state by viewModel.giveawayDetailsState.collectAsStateWithLifecycle()

    // The ViewModel loads once per entry; later calls do nothing until a refresh.
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    when (val currentState = state) {
        is GiveawayDetailsState.Error ->
            ErrorScreen(errorMessage = currentState.error.userMessage(), modifier = modifier) {
                viewModel.refresh()
            }

        GiveawayDetailsState.NotFound ->
            ErrorScreen(errorMessage = stringResource(Res.string.giveaway_details_not_found), modifier = modifier) {
                viewModel.refresh()
            }

        GiveawayDetailsState.Loading ->
            GlobalLoading(modifier = modifier)

        is GiveawayDetailsState.Success ->
            GiveawayDetailsScreen(
                id = id,
                state = currentState,
                modifier = modifier,
                onBackButtonPressed = { navigator.goBack() },
            )
    }
}

/**
 * The loaded giveaway [id]: a hero over its artwork with the action that opens it, then its facts, description, and
 * claim steps, and a link to its page on GamerPower. Wide windows put the facts in a panel beside the text. Its top
 * bar offers Share in layouts that [showsShareActions].
 */
@Composable
fun GiveawayDetailsScreen(
    id: Int,
    state: GiveawayDetailsState.Success,
    modifier: Modifier,
    onBackButtonPressed: (() -> Unit),
    uriHandler: UriHandler = LocalUriHandler.current,
) {
    val details = state.giveawayDetails
    val gutter = pageGutter()
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize(),
    ) {
        val lazyListState = rememberLazyListState()
        val (topBarColor, backButtonColor, topBarTitleColor) =
            lazyListState.calculateScrollTopBarColors()
        val contentWidth = pageContentWidth(maxWidth, gutter)

        LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize()) {
            item(key = HERO_KEY) {
                GiveawayHero(
                    id = id,
                    details = details,
                    expanded = contentWidth >= HeroBesideImageMinWidth,
                    onGetGiveaway = details.openUrl?.let { url -> { uriHandler.openExternalUrl(url) } },
                )
            }
            item(key = BODY_KEY) {
                GiveawayBody(
                    details = details,
                    withSidePanel = contentWidth >= SidePanelLayoutMinWidth,
                    onOpenSource = details.sourceUrl?.let { url -> { uriHandler.openExternalUrl(url) } },
                    modifier = Modifier.pageColumn(gutter).padding(bottom = Dimens.large),
                )
            }
        }
        PageScrollbar(lazyListState)
        GiveawayDetailsTopBar(
            title = details.title,
            shareTarget = ShareTarget.Giveaway(id = id, name = details.title),
            topBarColor = topBarColor,
            backButtonColor = backButtonColor,
            titleColor = topBarTitleColor,
            onBackButtonPressed = onBackButtonPressed,
        )
    }
}

private const val HERO_KEY = "hero"
private const val BODY_KEY = "body"

// The text beside the image needs room for a few words a line next to it.
private val HeroBesideImageMinWidth = 720.dp

// The facts panel beside the text, and the narrowest the text may get next to it.
private val SidePanelWidth = 320.dp
private val SidePanelLayoutMinWidth = 560.dp + Dimens.large + SidePanelWidth

/**
 * The facts, the description, the claim steps, and the source link. [withSidePanel] puts the facts and the source
 * beside the text at the column's end; otherwise the facts come first, since when a giveaway ends decides whether
 * the rest is worth reading.
 */
@Composable
private fun GiveawayBody(
    details: GiveawayDetails,
    withSidePanel: Boolean,
    onOpenSource: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val facts =
        @Composable {
            GiveawayFactsCard(
                ending = details.ending,
                claimedCount = details.claimedCount,
                addedOn = details.addedOn,
                platformNames = details.platformNames,
            )
        }
    val source = @Composable { onOpenSource?.let { onClick -> GiveawaySourceButton(onClick = onClick) } }
    if (withSidePanel) {
        Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            GiveawayText(
                details = details,
                modifier = Modifier.weight(1f, fill = false).widthIn(max = Dimens.readableTextWidth),
            )
            Column(
                modifier = Modifier.padding(start = Dimens.large).width(SidePanelWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.medium),
            ) {
                facts()
                source()
            }
        }
    } else {
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.large)) {
            facts()
            GiveawayText(details = details, modifier = Modifier.widthIn(max = Dimens.readableTextWidth))
            source()
        }
    }
}

/** The description and the claim steps, each left out when GamerPower sends none. */
@Composable
private fun GiveawayText(
    details: GiveawayDetails,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.large)) {
        if (details.description.isNotEmpty()) {
            GiveawaySection(title = stringResource(Res.string.giveaway_details_screen_about)) {
                SelectionContainer {
                    Text(text = details.description, style = MaterialTheme.typography.body2, color = white)
                }
            }
        }
        if (details.claimSteps.isNotEmpty()) {
            GiveawaySection(title = stringResource(Res.string.giveaway_details_screen_instruction)) {
                ClaimSteps(steps = details.claimSteps)
            }
        }
    }
}

/**
 * The bar over the scrolling content: back, the giveaway's title, which fades in as the bar fills, and, in layouts
 * that [showsShareActions], Share, which links to the giveaway's page. Back and Share are white icons on circular
 * backdrops that blend into the bar once it fills.
 */
@Suppress("LongParameterList")
@Composable
private fun GiveawayDetailsTopBar(
    title: String,
    shareTarget: ShareTarget,
    topBarColor: Color,
    backButtonColor: Color,
    titleColor: Color,
    onBackButtonPressed: () -> Unit,
) {
    Column {
        StatusBarOverlay(backgroundColor = topBarColor)
        TopAppBar(
            tabBarColor = topBarColor,
            title = title,
            titleColor = titleColor,
            showBackButton = true,
            backButtonBackgroundColor = backButtonColor,
            onBackButtonPressed = onBackButtonPressed,
            actions =
                if (showsShareActions()) {
                    { ShareButton(target = shareTarget, backgroundColor = backButtonColor, visible = true) }
                } else {
                    null
                },
        )
    }
}
