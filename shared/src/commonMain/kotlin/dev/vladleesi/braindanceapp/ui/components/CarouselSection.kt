// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.home_see_all
import dev.vladleesi.braindanceapp.resources.home_see_all_description
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import org.jetbrains.compose.resources.stringResource

// A lazy row composes only the skeletons that fit, so one count fills phones and wide windows alike.
private const val SKELETON_COUNT = 8

/**
 * One section as a titled horizontal carousel for narrow layouts. Loading shows [skeleton]s, an error offers
 * [onRetry], and success lays out the items through [content], which adds them with keys and content types. Pointer
 * devices on Web also get previous and next buttons. With [onSeeAll], the header links to the section's full list in
 * every state, so it never shifts the layout when the items arrive.
 */
@Suppress("LongParameterList")
@Composable
fun <T> CarouselSection(
    title: String,
    state: SectionState<T>,
    onRetry: () -> Unit,
    skeleton: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    titleStyle: TextStyle = MaterialTheme.typography.h2,
    onSeeAll: (() -> Unit)? = null,
    content: LazyListScope.(items: List<T>) -> Unit,
) {
    Crossfade(targetState = state, modifier = modifier) { target ->
        // Each state has its own list state, so the controls only scroll the row that is on screen; before success
        // it is attached to nothing, and the controls render disabled at their loaded size.
        val listState = rememberLazyListState()
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
            CarouselHeader(title = title, titleStyle = titleStyle, listState = listState, onSeeAll = onSeeAll)
            when (target) {
                SectionState.Loading ->
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
                        contentPadding = PaddingValues(horizontal = Dimens.medium),
                        userScrollEnabled = false,
                    ) {
                        items(SKELETON_COUNT) { skeleton() }
                    }

                is SectionState.Success ->
                    if (target.items.isEmpty()) {
                        SectionEmpty(modifier = Modifier.padding(horizontal = Dimens.medium))
                    } else {
                        LazyRow(
                            state = listState,
                            horizontalArrangement = Arrangement.spacedBy(Dimens.small),
                            contentPadding = PaddingValues(horizontal = Dimens.medium),
                        ) {
                            content(target.items)
                        }
                    }

                is SectionState.Error ->
                    SectionError(
                        message = target.error.userMessage(),
                        onRetry = onRetry,
                        modifier = Modifier.padding(horizontal = Dimens.medium),
                    )
            }
        }
    }
}

@Composable
private fun CarouselHeader(
    title: String,
    titleStyle: TextStyle,
    listState: LazyListState,
    onSeeAll: (() -> Unit)?,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(end = if (onSeeAll == null) 0.dp else Dimens.tiny),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StaticText(
            text = title,
            style = titleStyle,
            modifier =
                Modifier
                    .weight(1f)
                    .padding(horizontal = Dimens.medium)
                    .semantics { heading() },
        )
        HorizontalListControls(listState = listState)
        if (onSeeAll != null) {
            SeeAllButton(sectionTitle = title, onClick = onSeeAll)
        }
    }
}

/**
 * Opens the full list of the section titled [sectionTitle]. It is set in the secondary text color, so the white
 * section title stays the stronger element; screen readers name the section it opens.
 */
@Composable
fun SeeAllButton(
    sectionTitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val description = stringResource(Res.string.home_see_all_description, sectionTitle)
    TextButton(
        onClick = onClick,
        modifier =
            modifier
                .pointerHoverIcon(PointerIcon.Hand)
                .semantics { contentDescription = description },
    ) {
        Text(
            text = stringResource(Res.string.home_see_all),
            style = MaterialTheme.typography.button,
            color = secondaryText,
        )
    }
}

@Preview
@Composable
internal fun CarouselSectionPreview() {
    BraindanceTheme {
        Column(
            modifier = Modifier.background(background).padding(vertical = Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.large),
        ) {
            CarouselSection(
                title = "Popular Right Now",
                state =
                    SectionState.Success(
                        listOf(
                            GameCardModel(id = 1, title = "Sample game", backgroundImageUrl = ""),
                            GameCardModel(id = 2, title = "Another game", backgroundImageUrl = ""),
                        ),
                    ),
                onRetry = {},
                skeleton = { GameCardSkeleton(showTitle = true, modifier = Modifier.width(Dimens.gameCardWidth)) },
                onSeeAll = {},
            ) { games ->
                items(games, key = { game -> game.id }) { game ->
                    GameCard(
                        card = game,
                        onClick = {},
                        showTitle = true,
                        modifier = Modifier.width(Dimens.gameCardWidth),
                    )
                }
            }
            CarouselSection<GameCardModel>(
                title = "Most Anticipated",
                state = SectionState.Loading,
                onRetry = {},
                skeleton = { GameCardSkeleton(showTitle = false, modifier = Modifier.width(Dimens.gameCardWidth)) },
            ) {}
        }
    }
}
