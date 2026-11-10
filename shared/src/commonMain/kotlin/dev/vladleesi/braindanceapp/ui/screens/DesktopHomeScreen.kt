// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import Bell
import CalendarDays
import CircleUserRound
import Star
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import dev.vladleesi.braindanceapp.navigation.LocalNavigator
import dev.vladleesi.braindanceapp.navigation.routes.BottomBarRoute
import dev.vladleesi.braindanceapp.navigation.routes.GameDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.GiveawayDetailsRoute
import dev.vladleesi.braindanceapp.navigation.routes.ProfileRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.desktop_development_status
import dev.vladleesi.braindanceapp.resources.desktop_hero_action
import dev.vladleesi.braindanceapp.resources.desktop_hero_description
import dev.vladleesi.braindanceapp.resources.desktop_hero_title
import dev.vladleesi.braindanceapp.resources.desktop_recommended_title
import dev.vladleesi.braindanceapp.resources.desktop_tagline
import dev.vladleesi.braindanceapp.resources.home_events_description
import dev.vladleesi.braindanceapp.resources.home_events_title
import dev.vladleesi.braindanceapp.resources.home_latest_news_description
import dev.vladleesi.braindanceapp.resources.home_latest_news_title
import dev.vladleesi.braindanceapp.resources.home_notifications_coming_soon
import dev.vladleesi.braindanceapp.resources.home_open_news
import dev.vladleesi.braindanceapp.resources.home_open_releases
import dev.vladleesi.braindanceapp.resources.home_recommendations_description
import dev.vladleesi.braindanceapp.resources.home_screen_popular_latest_giveaways
import dev.vladleesi.braindanceapp.resources.home_screen_popular_most_anticipated
import dev.vladleesi.braindanceapp.resources.home_screen_popular_right_now
import dev.vladleesi.braindanceapp.resources.home_upcoming_releases_description
import dev.vladleesi.braindanceapp.resources.profile_screen_title
import dev.vladleesi.braindanceapp.resources.releases_upcoming_title
import dev.vladleesi.braindanceapp.system.DesktopLayout
import dev.vladleesi.braindanceapp.ui.components.COVER_ASPECT_RATIO
import dev.vladleesi.braindanceapp.ui.components.ComingSoonAction
import dev.vladleesi.braindanceapp.ui.components.ComingSoonCard
import dev.vladleesi.braindanceapp.ui.components.ComingSoonLabel
import dev.vladleesi.braindanceapp.ui.components.desktop.BrandAccentStrip
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopCardRow
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopGridColumns
import dev.vladleesi.braindanceapp.ui.components.desktop.DesktopSection
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayCard
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayCardSkeleton
import dev.vladleesi.braindanceapp.ui.components.home.GameCard
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardSkeleton
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.ui.style.navBar
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.HomeViewModel
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.rememberCachedImageRequest
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

// Keeps rows of cards from stretching across very wide windows.
private val MainColumnMaxWidth = 1120.dp
private val ContextColumnWidth = 280.dp
private val HeroMinHeight = 220.dp
private val ContextIconSize = 20.dp
private const val HERO_TEXT_WIDTH_FRACTION = 0.6f
private const val HERO_COVER_COUNT = 3
private const val HERO_SCRIM_OPAQUE_END = 0.5f
private const val HERO_SCRIM_FADE_STOP = 0.75f
private const val HERO_SCRIM_FADE_ALPHA = 0.6f
private const val HERO_SCRIM_END_ALPHA = 0.2f

// Opaque behind the text, then fading so the covers on the right stay visible but subdued.
private val HeroScrim =
    Brush.horizontalGradient(
        0f to navBar,
        HERO_SCRIM_OPAQUE_END to navBar,
        HERO_SCRIM_FADE_STOP to navBar.copy(alpha = HERO_SCRIM_FADE_ALPHA),
        1f to navBar.copy(alpha = HERO_SCRIM_END_ALPHA),
    )

/**
 * The Home screen for wide Web windows: a header, a discovery hero, fixed card grids for each section, and previews
 * of planned features. It renders the same [HomeViewModel] state and navigation as the standard Home screen.
 */
@Composable
internal fun DesktopHomeScreen(
    layout: DesktopLayout,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current

    // Loads only on first navigation, like the standard Home screen; switching layouts does not reload.
    LaunchedEffect(Unit) {
        viewModel.load()
    }

    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxSize()
                .background(background),
    ) {
        val horizontalPadding = Dimens.large
        val contextWidth = if (layout.hasContextColumn) ContextColumnWidth + Dimens.large else 0.dp
        val mainWidth = (maxWidth - horizontalPadding * 2 - contextWidth).coerceIn(0.dp, MainColumnMaxWidth)
        val columns = remember(mainWidth) { DesktopGridColumns.forWidth(mainWidth) }

        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = horizontalPadding, vertical = Dimens.large),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier = Modifier.width(mainWidth + contextWidth),
                verticalArrangement = Arrangement.spacedBy(Dimens.large),
            ) {
                DesktopHomeHeader(onProfileClick = { navigator.navigate(ProfileRoute) })
                Row(horizontalArrangement = Arrangement.spacedBy(Dimens.large)) {
                    DesktopHomeMainColumn(
                        layout = layout,
                        viewModel = viewModel,
                        columns = columns,
                        modifier = Modifier.width(mainWidth),
                    )
                    if (layout.hasContextColumn) {
                        DesktopContextColumn(
                            onOpenReleases = { navigator.navigate(BottomBarRoute.ReleasesRoute) },
                            modifier = Modifier.width(ContextColumnWidth),
                        )
                    }
                }
            }
        }
    }
}

/** The real content: the hero, the three card sections, and previews of planned features. */
@Composable
private fun DesktopHomeMainColumn(
    layout: DesktopLayout,
    viewModel: HomeViewModel,
    columns: DesktopGridColumns,
    modifier: Modifier = Modifier,
) {
    val navigator = LocalNavigator.current
    val trendingNow by viewModel.trendingNow.collectAsStateWithLifecycle()
    val mostAnticipated by viewModel.mostAnticipated.collectAsStateWithLifecycle()
    val giveaways by viewModel.giveaways.collectAsStateWithLifecycle()
    val onGameClick = { id: Int -> navigator.navigate(GameDetailsRoute(id)) }
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(Dimens.large),
    ) {
        DesktopHero(
            trendingNow = trendingNow,
            visibleTrendingCount = columns.games,
            onExploreClick = { navigator.navigate(BottomBarRoute.DiscoverRoute) },
        )
        DesktopSection(title = stringResource(Res.string.home_screen_popular_right_now)) {
            DesktopCardRow(
                state = trendingNow,
                columns = columns.games,
                onRetry = viewModel::loadTrendingNow,
                skeleton = { itemModifier -> GameCardSkeleton(showTitle = true, modifier = itemModifier) },
            ) { card, itemModifier ->
                GameCard(card = card, onClick = onGameClick, showTitle = true, modifier = itemModifier)
            }
        }
        DesktopSection(title = stringResource(Res.string.home_screen_popular_most_anticipated)) {
            DesktopCardRow(
                state = mostAnticipated,
                columns = columns.anticipated,
                onRetry = { viewModel.loadMostAnticipated() },
                skeleton = { itemModifier -> GameCardSkeleton(showTitle = false, modifier = itemModifier) },
            ) { card, itemModifier ->
                GameCard(card = card, onClick = onGameClick, showTitle = false, modifier = itemModifier)
            }
        }
        DesktopSection(title = stringResource(Res.string.home_screen_popular_latest_giveaways)) {
            DesktopCardRow(
                state = giveaways,
                columns = columns.giveaways,
                onRetry = { viewModel.loadGiveaways() },
                skeleton = { itemModifier -> GiveawayCardSkeleton(modifier = itemModifier) },
            ) { item, itemModifier ->
                GiveawayCard(
                    item = item,
                    onClick = { id -> navigator.navigate(GiveawayDetailsRoute(id)) },
                    modifier = itemModifier,
                )
            }
        }
        // Without the context column, its modules join the other planned features here.
        DesktopPreviewGrid(
            includeContextModules = !layout.hasContextColumn,
            onOpenReleases = { navigator.navigate(BottomBarRoute.ReleasesRoute) },
            onOpenNews = { navigator.navigate(BottomBarRoute.NewsRoute) },
        )
    }
}

@Composable
private fun DesktopHomeHeader(onProfileClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = stringResource(Res.string.desktop_tagline),
            style = MaterialTheme.typography.h2,
            modifier =
                Modifier
                    .weight(1f)
                    .semantics { heading() },
        )
        Text(
            text = stringResource(Res.string.desktop_development_status),
            style = MaterialTheme.typography.caption,
            color = signalYellow,
            maxLines = 1,
            modifier =
                Modifier
                    .padding(end = Dimens.small)
                    .background(secondary, CircleShape)
                    .padding(horizontal = Dimens.small, vertical = Dimens.micro),
        )
        ComingSoonAction(imageVector = Bell, label = stringResource(Res.string.home_notifications_coming_soon))
        IconButton(onClick = onProfileClick, modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)) {
            Icon(
                imageVector = CircleUserRound,
                contentDescription = stringResource(Res.string.profile_screen_title),
                tint = white,
            )
        }
    }
}

/**
 * A wide banner introducing discovery. Its artwork is trending covers the Popular row does not already show, drawn
 * behind a dark scrim; it has no slideshow or motion.
 */
@Composable
private fun DesktopHero(
    trendingNow: SectionState<GameCardModel>,
    visibleTrendingCount: Int,
    onExploreClick: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .heightIn(min = HeroMinHeight)
                .clip(CornerShapes.medium)
                .background(navBar),
    ) {
        if (trendingNow is SectionState.Success && trendingNow.items.isNotEmpty()) {
            val items = trendingNow.items
            // Prefer covers below the fold of the Popular row; fall back to the last ones when the list is short.
            val firstIndex = visibleTrendingCount.coerceAtMost((items.size - HERO_COVER_COUNT).coerceAtLeast(0))
            Row(
                modifier = Modifier.matchParentSize(),
                horizontalArrangement = Arrangement.spacedBy(Dimens.small, Alignment.End),
            ) {
                for (index in firstIndex until (firstIndex + HERO_COVER_COUNT).coerceAtMost(items.size)) {
                    val card = items[index]
                    AsyncImage(
                        model =
                            rememberCachedImageRequest(
                                data = card.backgroundImageUrl,
                                imageCacheKey = "dhc-${card.id}",
                            ),
                        // Decorative: the same games are listed, with their titles, in the sections below.
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier =
                            Modifier
                                .fillMaxHeight()
                                .aspectRatio(COVER_ASPECT_RATIO, matchHeightConstraintsFirst = true),
                    )
                }
            }
        }
        Box(
            modifier =
                Modifier
                    .matchParentSize()
                    .background(HeroScrim),
        )
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(HERO_TEXT_WIDTH_FRACTION)
                    .padding(Dimens.large),
            verticalArrangement = Arrangement.spacedBy(Dimens.compact),
        ) {
            BrandAccentStrip()
            Text(
                text = stringResource(Res.string.desktop_hero_title),
                style = MaterialTheme.typography.h1,
                modifier = Modifier.semantics { heading() },
            )
            Text(
                text = stringResource(Res.string.desktop_hero_description),
                style = MaterialTheme.typography.body2,
                color = secondaryText,
            )
            Spacer(Modifier.height(Dimens.tiny))
            Button(
                onClick = onExploreClick,
                colors = ButtonDefaults.buttonColors(backgroundColor = white, contentColor = navBar),
                elevation = null,
                modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
            ) {
                // The theme's text styles carry their own color, which would override the button's content color.
                Text(
                    text = stringResource(Res.string.desktop_hero_action),
                    color = navBar,
                    modifier = Modifier.padding(horizontal = Dimens.small),
                )
            }
        }
    }
}

/** The contextual modules beside the main column in the widest layout. */
@Composable
private fun DesktopContextColumn(
    onOpenReleases: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.medium)) {
        ContextCard(
            imageVector = CalendarDays,
            title = stringResource(Res.string.releases_upcoming_title),
            description = stringResource(Res.string.home_upcoming_releases_description),
            link = Res.string.home_open_releases,
            onLinkClick = onOpenReleases,
        )
        ContextCard(
            imageVector = Star,
            title = stringResource(Res.string.desktop_recommended_title),
            description = stringResource(Res.string.home_recommendations_description),
        )
    }
}

/** A planned feature in the context column; [link], when given, opens the destination that will host it. */
@Composable
private fun ContextCard(
    imageVector: ImageVector,
    title: String,
    description: String,
    link: StringResource? = null,
    onLinkClick: (() -> Unit)? = null,
) {
    Surface(modifier = Modifier.fillMaxWidth(), shape = CornerShapes.medium, color = secondary) {
        Column(
            modifier = Modifier.padding(Dimens.medium),
            verticalArrangement = Arrangement.spacedBy(Dimens.small),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = imageVector,
                    contentDescription = null,
                    tint = secondaryText,
                    modifier = Modifier.size(ContextIconSize),
                )
                Spacer(Modifier.weight(1f))
                ComingSoonLabel()
            }
            Text(
                text = title,
                style = MaterialTheme.typography.subtitle1,
                modifier = Modifier.semantics { heading() },
            )
            Text(text = description, style = MaterialTheme.typography.caption, color = secondaryText)
            if (link != null && onLinkClick != null) {
                DesktopLinkButton(text = stringResource(link), onClick = onLinkClick)
            }
        }
    }
}

/** Planned Home sections as compact previews, two per row, below the real content. */
@Composable
private fun DesktopPreviewGrid(
    includeContextModules: Boolean,
    onOpenReleases: () -> Unit,
    onOpenNews: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(Dimens.medium)) {
        if (includeContextModules) {
            PreviewRow {
                PreviewCard(
                    title = stringResource(Res.string.releases_upcoming_title),
                    description = Res.string.home_upcoming_releases_description,
                    link = Res.string.home_open_releases,
                    onLinkClick = onOpenReleases,
                    modifier = previewCell(),
                )
                PreviewCard(
                    title = stringResource(Res.string.desktop_recommended_title),
                    description = Res.string.home_recommendations_description,
                    modifier = previewCell(),
                )
            }
        }
        PreviewRow {
            PreviewCard(
                title = stringResource(Res.string.home_latest_news_title),
                description = Res.string.home_latest_news_description,
                link = Res.string.home_open_news,
                onLinkClick = onOpenNews,
                modifier = previewCell(),
            )
            PreviewCard(
                title = stringResource(Res.string.home_events_title),
                description = Res.string.home_events_description,
                modifier = previewCell(),
            )
        }
    }
}

/** Cards in a row share the height of the tallest one. */
@Composable
private fun PreviewRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(Dimens.medium),
        content = content,
    )
}

private fun RowScope.previewCell(): Modifier = Modifier.weight(1f).fillMaxHeight()

@Composable
private fun PreviewCard(
    title: String,
    description: StringResource,
    modifier: Modifier = Modifier,
    link: StringResource? = null,
    onLinkClick: (() -> Unit)? = null,
) {
    ComingSoonCard(
        title = title,
        description = stringResource(description),
        modifier = modifier,
        action =
            if (link != null && onLinkClick != null) {
                {
                    DesktopLinkButton(text = stringResource(link), onClick = onLinkClick)
                }
            } else {
                null
            },
    )
}

/** A preview card's link to its destination, filled so it reads as a button before it is hovered. */
@Composable
private fun DesktopLinkButton(
    text: String,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        shape = CornerShapes.small,
        colors = ButtonDefaults.textButtonColors(backgroundColor = background, contentColor = white),
        contentPadding = PaddingValues(horizontal = Dimens.compact, vertical = Dimens.small),
        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
    ) {
        Text(text = text, color = white)
    }
}

@Preview
@Composable
internal fun DesktopLinkButtonPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(secondary).padding(Dimens.medium)) {
            DesktopLinkButton(text = "Open Releases", onClick = {})
        }
    }
}

@Preview
@Composable
internal fun DesktopHomeHeaderPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(Dimens.large)) {
            DesktopHomeHeader(onProfileClick = {})
        }
    }
}

@Preview
@Composable
internal fun DesktopHeroPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(Dimens.large)) {
            DesktopHero(trendingNow = SectionState.Loading, visibleTrendingCount = 0, onExploreClick = {})
        }
    }
}

@Preview
@Composable
internal fun DesktopContextColumnPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(Dimens.large)) {
            DesktopContextColumn(onOpenReleases = {}, modifier = Modifier.width(ContextColumnWidth))
        }
    }
}

@Preview
@Composable
internal fun DesktopPreviewGridPreview() {
    BraindanceTheme {
        Box(modifier = Modifier.background(background).padding(Dimens.large)) {
            DesktopPreviewGrid(includeContextModules = true, onOpenReleases = {}, onOpenNews = {})
        }
    }
}
