// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.games.GamingEvent
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.event_ended
import dev.vladleesi.braindanceapp.resources.event_future
import dev.vladleesi.braindanceapp.resources.event_live
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.formatEventDate
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import dev.vladleesi.braindanceapp.utils.toSafeExternalUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.StringResource
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

private val DefaultEventDuration = 3.hours
private const val RELATIVE_DATE_WINDOW = 6

internal class EventsViewModel(
    private val id: Int?,
    private val homeRepo: HomeRepo,
    clock: Clock = Clock.System,
) : ScreenViewModel() {
    private val feed = MutableStateFlow<SectionState<GamingEvent>>(SectionState.Loading)
    private val games = MutableStateFlow<SectionState<GameCardModel>>(SectionState.Loading)
    private var loadJob: Job? = null

    val events = eventCards(feed, clock).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), SectionState.Loading)

    val detail =
        combine(events, games, ::eventDetail)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EventDetail.Loading)

    override fun onLoad() = onRefresh()

    override fun onRefresh() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                feed.value = SectionState.Loading
                games.value = SectionState.Loading
                launch {
                    feed.value =
                        when (val result = homeRepo.events()) {
                            is Outcome.Success -> SectionState.Success(result.value.distinctBy { it.id })
                            is Outcome.Failure -> SectionState.Error(result.error)
                        }
                }
                id?.let { eventId ->
                    launch {
                        games.value =
                            when (val result = homeRepo.eventGames(eventId)) {
                                is Outcome.Success ->
                                    SectionState.Success(
                                        result.value.mapNotNull { it.toGameCardModel() }.distinctBy { it.id },
                                    )
                                is Outcome.Failure -> SectionState.Error(result.error)
                            }
                    }
                }
            }
    }

    private fun eventDetail(
        feed: SectionState<EventCardModel>,
        games: SectionState<GameCardModel>,
    ): EventDetail {
        val event = (feed as? SectionState.Success)?.items?.firstOrNull { it.id == id }
        return when (games) {
            is SectionState.Error -> EventDetail.Error(games.error, event)
            SectionState.Loading -> EventDetail.Loading
            is SectionState.Success ->
                when {
                    feed is SectionState.Loading -> EventDetail.Loading
                    event != null || games.items.isNotEmpty() -> EventDetail.Ready(event, games.items)
                    feed is SectionState.Error -> EventDetail.Error(feed.error)
                    else -> EventDetail.Unavailable
                }
        }
    }
}

internal sealed interface EventDetail {
    data object Loading : EventDetail

    data object Unavailable : EventDetail

    data class Error(
        val error: DataError,
        val event: EventCardModel? = null,
    ) : EventDetail

    data class Ready(
        val event: EventCardModel?,
        val games: List<GameCardModel>,
    ) : EventDetail
}

internal enum class EventStatus(
    val label: StringResource,
) {
    LIVE(Res.string.event_live),
    UPCOMING(Res.string.event_future),
    ENDED(Res.string.event_ended),
}

internal sealed interface EventDateLabel {
    data class Today(
        val time: String,
    ) : EventDateLabel

    data class InDays(
        val days: Int,
    ) : EventDateLabel

    data class Absolute(
        val date: String,
    ) : EventDateLabel
}

internal data class EventCardModel(
    val id: Int,
    val name: String,
    val logoUrl: String,
    val status: EventStatus,
    val cardDate: EventDateLabel,
    val startDate: String,
    val endDate: String? = null,
    val watchUrl: String? = null,
)

internal fun eventStatus(
    startTime: Long,
    endTime: Long?,
    now: Long,
): EventStatus =
    when {
        now < startTime -> EventStatus.UPCOMING
        now >= (endTime?.takeIf { it > startTime } ?: (startTime + DefaultEventDuration.inWholeSeconds)) ->
            EventStatus.ENDED
        else -> EventStatus.LIVE
    }

private fun eventTicks(clock: Clock): Flow<Instant> =
    flow {
        while (currentCoroutineContext().isActive) {
            val now = clock.now()
            emit(now)
            val minute = 1.minutes.inWholeMilliseconds
            delay((minute - now.toEpochMilliseconds().mod(minute)).milliseconds)
        }
    }

internal fun eventCards(
    feed: Flow<SectionState<GamingEvent>>,
    clock: Clock,
): Flow<SectionState<EventCardModel>> =
    combine(feed, eventTicks(clock)) { state, now ->
        when (state) {
            is SectionState.Success -> {
                val zone = TimeZone.currentSystemDefault()
                val today = now.toLocalDateTime(zone).date
                SectionState.Success(state.items.map { it.toCard(now.epochSeconds, zone, today) })
            }
            is SectionState.Error -> state
            SectionState.Loading -> SectionState.Loading
        }
    }

private fun GamingEvent.toCard(
    now: Long,
    zone: TimeZone,
    today: LocalDate,
): EventCardModel {
    val status = eventStatus(startTime, endTime, now)
    val start = Instant.fromEpochSeconds(startTime)
    val date = formatEventDate(startTime, timeOnly = false)
    val daysAway = today.daysUntil(start.toLocalDateTime(zone).date)
    val cardDate =
        when {
            daysAway == 0 -> EventDateLabel.Today(formatEventDate(startTime, timeOnly = true))
            status == EventStatus.UPCOMING && daysAway in 1..RELATIVE_DATE_WINDOW -> EventDateLabel.InDays(daysAway)
            else -> EventDateLabel.Absolute(date)
        }
    return EventCardModel(
        id = id,
        name = name,
        logoUrl =
            logo
                ?.url
                ?.takeIf(String::isNotBlank)
                ?.toCoverUrl(CoverSize.LOGO_MED)
                .orEmpty(),
        status = status,
        cardDate = cardDate,
        startDate = date,
        endDate = endTime?.takeIf { it > startTime }?.let { formatEventDate(it, timeOnly = false) },
        watchUrl = liveStreamUrl.toSafeExternalUrl(),
    )
}
