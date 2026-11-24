// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavKey
import dev.vladleesi.braindanceapp.navigation.routes.EventsRoute
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.event_in_days
import dev.vladleesi.braindanceapp.resources.event_logo_missing
import dev.vladleesi.braindanceapp.resources.event_today
import dev.vladleesi.braindanceapp.resources.home_events_title
import dev.vladleesi.braindanceapp.ui.components.CardCover
import dev.vladleesi.braindanceapp.ui.components.CardCoverSkeleton
import dev.vladleesi.braindanceapp.ui.components.CarouselSection
import dev.vladleesi.braindanceapp.ui.components.Tag
import dev.vladleesi.braindanceapp.ui.components.TextLineSkeleton
import dev.vladleesi.braindanceapp.ui.components.cardClickable
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.accent
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.secondaryText
import dev.vladleesi.braindanceapp.ui.style.surface
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.ui.viewmodels.EventCardModel
import dev.vladleesi.braindanceapp.ui.viewmodels.EventDateLabel
import dev.vladleesi.braindanceapp.ui.viewmodels.EventStatus
import dev.vladleesi.braindanceapp.ui.viewmodels.SectionState
import dev.vladleesi.braindanceapp.utils.focusRing
import dev.vladleesi.braindanceapp.utils.isFocusVisible
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

private const val NAME_SKELETON_FRACTION = 0.75f
private const val STATUS_SKELETON_FRACTION = 0.5f
private const val LIVE_TAG_FILL_ALPHA = 0.12f
internal const val EVENT_LOGO_RATIO = 16f / 9f

@Composable
internal fun EventsCarousel(
    events: SectionState<EventCardModel>,
    onRetry: () -> Unit,
    navigate: (NavKey) -> Unit,
    modifier: Modifier = Modifier,
) {
    val width = Modifier.width(Dimens.giveAwayCardWidth)
    CarouselSection(
        title = stringResource(Res.string.home_events_title),
        state = events,
        onRetry = onRetry,
        onSeeAll = { navigate(EventsRoute()) },
        seeAllRoute = EventsRoute(),
        skeleton = { EventCardSkeleton(width) },
        modifier = modifier,
    ) { events ->
        items(events.size, key = { events[it].id }, contentType = { "event" }) { index ->
            val event = events[index]
            EventCard(event, { navigate(EventsRoute(event.id)) }, width)
        }
    }
}

@Composable
internal fun EventCard(
    event: EventCardModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val date =
        when (val label = event.cardDate) {
            is EventDateLabel.Today -> stringResource(Res.string.event_today, label.time)
            is EventDateLabel.InDays -> pluralStringResource(Res.plurals.event_in_days, label.days, label.days)
            is EventDateLabel.Absolute -> label.date
        }
    val status = stringResource(event.status.label)
    Column(
        modifier =
            modifier
                .clip(CornerShapes.large)
                .background(surface)
                .focusRing(interaction.isFocusVisible(), CornerShapes.large)
                .semantics { contentDescription = "${event.name}, $date, $status" }
                .cardClickable(interaction, EventsRoute(event.id), onClick)
                .padding(Dimens.compact),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        EventLogo(event.logoUrl, interactionSource = interaction)
        Text(
            event.name,
            Modifier.clearAndSetSemantics {},
            style = MaterialTheme.typography.body2,
            color = white,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Column(
            modifier = Modifier.clearAndSetSemantics {},
            verticalArrangement = Arrangement.spacedBy(Dimens.tiny),
        ) {
            Text(
                date,
                style = MaterialTheme.typography.caption,
                color = secondaryText,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            EventStatusTag(event.status)
        }
    }
}

@Composable
internal fun EventLogo(
    url: String,
    modifier: Modifier = Modifier,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
) {
    CardCover(
        imageUrl = url,
        memoryCacheKey = "event-$url",
        contentDescription = stringResource(Res.string.event_logo_missing),
        aspectRatio = EVENT_LOGO_RATIO,
        interactionSource = interactionSource,
        modifier = modifier.clearAndSetSemantics {},
        contentScale = ContentScale.Fit,
    )
}

@Composable
internal fun EventStatusTag(status: EventStatus) {
    val live = status == EventStatus.LIVE
    Tag(
        stringResource(status.label),
        modifier =
            Modifier.background(
                if (live) accent.copy(alpha = LIVE_TAG_FILL_ALPHA) else secondary,
                CornerShapes.pill,
            ),
        color = if (live) accent else white,
    )
}

@Composable
internal fun EventCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier.background(surface, CornerShapes.large).padding(Dimens.compact),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        CardCoverSkeleton(aspectRatio = EVENT_LOGO_RATIO)
        TextLineSkeleton(MaterialTheme.typography.body2, widthFraction = NAME_SKELETON_FRACTION)
        TextLineSkeleton(MaterialTheme.typography.body2, widthFraction = NAME_SKELETON_FRACTION)
        TextLineSkeleton(MaterialTheme.typography.caption, widthFraction = NAME_SKELETON_FRACTION)
        TextLineSkeleton(MaterialTheme.typography.caption, widthFraction = STATUS_SKELETON_FRACTION)
    }
}

internal val PreviewEvent =
    EventCardModel(
        id = 1,
        name = "Indie Showcase",
        logoUrl = "",
        status = EventStatus.LIVE,
        cardDate = EventDateLabel.Today("19:00"),
        startDate = "Nov 21, 2026, 19:00",
        endDate = "Nov 21, 2026, 20:00",
        watchUrl = "https://example.com",
    )

@Preview
@Composable
internal fun EventsCarouselPreview() {
    BraindanceTheme { EventsCarousel(SectionState.Success(listOf(PreviewEvent)), {}, {}) }
}

@Preview
@Composable
internal fun EventCardPreview() {
    BraindanceTheme { EventCard(PreviewEvent, {}, Modifier.width(Dimens.giveAwayCardWidth)) }
}

@Preview
@Composable
internal fun EventLogoPreview() {
    BraindanceTheme { EventLogo("", Modifier.width(Dimens.giveAwayCardWidth)) }
}

@Preview
@Composable
internal fun EventStatusTagPreview() {
    BraindanceTheme { EventStatusTag(EventStatus.LIVE) }
}

@Preview
@Composable
internal fun EventCardSkeletonPreview() {
    BraindanceTheme { EventCardSkeleton(Modifier.width(Dimens.giveAwayCardWidth)) }
}
