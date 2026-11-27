// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.data.models.games.GameRating
import dev.vladleesi.braindanceapp.data.models.games.NamedItem
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.models.games.TimeToBeat
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.domain.games.OverviewHighlight
import dev.vladleesi.braindanceapp.domain.games.overviewHighlights
import dev.vladleesi.braindanceapp.domain.games.roundedPlayHours
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.about_complete
import dev.vladleesi.braindanceapp.resources.about_critic
import dev.vladleesi.braindanceapp.resources.about_developer
import dev.vladleesi.braindanceapp.resources.about_developer_publisher
import dev.vladleesi.braindanceapp.resources.about_extra
import dev.vladleesi.braindanceapp.resources.about_main
import dev.vladleesi.braindanceapp.resources.about_publisher
import dev.vladleesi.braindanceapp.resources.about_series
import dev.vladleesi.braindanceapp.resources.about_user
import dev.vladleesi.braindanceapp.resources.status_alpha
import dev.vladleesi.braindanceapp.resources.status_beta
import dev.vladleesi.braindanceapp.resources.status_cancelled
import dev.vladleesi.braindanceapp.resources.status_delisted
import dev.vladleesi.braindanceapp.resources.status_early_access
import dev.vladleesi.braindanceapp.resources.status_offline
import dev.vladleesi.braindanceapp.resources.status_rumored
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.ui.components.media.MediaGallery
import dev.vladleesi.braindanceapp.ui.components.media.mediaGallery
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.StoreTypeModel
import dev.vladleesi.braindanceapp.utils.formatDate
import dev.vladleesi.braindanceapp.utils.orZero
import dev.vladleesi.braindanceapp.utils.parentPlatformTypes
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import dev.vladleesi.braindanceapp.utils.toStoreTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.StringResource

class GameDetailsViewModel(
    private val gameId: Int,
    private val gameDetailsRepo: GameDetailsRepo,
) : ScreenViewModel() {
    private val detailsState = MutableStateFlow<GameDetailsState>(GameDetailsState.Loading)
    internal val gameDetailsState: StateFlow<GameDetailsState> = detailsState.asStateFlow()

    private val _recommendations = MutableStateFlow<RecommendationsState>(RecommendationsState.Loading)

    /** Loaded independently so a slow or failed recommendation request never delays or replaces the details. */
    val recommendations: StateFlow<RecommendationsState> = _recommendations.asStateFlow()

    // One running load each: a refresh cancels the previous loads, so an older response can never replace a newer one.
    private var detailsJob: Job? = null
    private var recommendationsJob: Job? = null

    // The entry or version the page shows: the screen's game at first, then each version switched to in place.
    private var shownId = gameId

    // The game similar games were last requested for, so switching versions of one game requests them once.
    private var recommendationsFor: Int? = null

    override fun onLoad() {
        loadDetails(shownId, inPlace = false)
    }

    override fun onRefresh() {
        loadDetails(shownId, inPlace = false)
    }

    /**
     * Shows [id], the page's game or one of its versions, in place, as the version selector switches them: the page
     * stays on screen, with [id] selected at once, until its details replace it. Before the page has loaded, or after
     * it failed, [id] loads as a page of its own.
     */
    fun showVersion(id: Int) {
        if (id == shownId) return
        shownId = id
        val shown = (detailsState.value as? GameDetailsState.Success)?.gameDetails
        if (shown == null) {
            loadDetails(id, inPlace = false)
            return
        }
        detailsState.value = GameDetailsState.Success(shown.selecting(id), isSwitchingVersion = true)
        loadDetails(id, inPlace = true)
    }

    private fun loadDetails(
        id: Int,
        inPlace: Boolean,
    ) {
        detailsJob?.cancel()
        detailsJob =
            viewModelScope.launch {
                if (!inPlace) {
                    detailsState.value = GameDetailsState.Loading
                    loadRecommendations(id)
                }
                detailsState.value =
                    when (val outcome = gameDetailsRepo.gameDetails(id)) {
                        is Outcome.Success -> {
                            val details = outcome.value.toGameDetails()
                            // An edition or update shows its game's relations, so similar games follow the game.
                            if (details.id != recommendationsFor) loadRecommendations(details.id)
                            GameDetailsState.Success(details)
                        }

                        is Outcome.Failure ->
                            if (outcome.error == DataError.NotFound) {
                                GameDetailsState.NotFound
                            } else {
                                BLogger.error(message = "Game details unavailable", throwable = outcome.cause)
                                GameDetailsState.Error(outcome.error)
                            }
                    }
            }
    }

    private fun loadRecommendations(gameId: Int) {
        recommendationsFor = gameId
        recommendationsJob?.cancel()
        _recommendations.value = RecommendationsState.Loading
        recommendationsJob =
            viewModelScope.launch {
                _recommendations.value =
                    when (val outcome = gameDetailsRepo.recommendations(gameId)) {
                        is Outcome.Success -> RecommendationsState.Success(outcome.value.toCards())
                        is Outcome.Failure -> {
                            // The section is optional: keep the details visible and leave it hidden until a refresh.
                            BLogger.error(message = "Recommendations unavailable", throwable = outcome.cause)
                            RecommendationsState.Unavailable
                        }
                    }
            }
    }
}

/**
 * The page for this details response. An edition or update with a known canonical game is shown as that release of
 * its game: its own name, cover, release date, platforms, genres, stores, summary, screenshot, and media, with the
 * game's for anything IGDB lacks for it, and the game's versions and related releases with it selected. The page
 * keeps the game's ID, so relations and similar games follow the game. Without a canonical game, the entry is shown
 * as a game of its own.
 */
private fun GameItem.toGameDetails(): GameDetails {
    val game = canonical?.takeIf { canonicalId != null && canonicalId != id } ?: this
    val selected = if (game === this) null else toSelectedVariant()
    val shown = if (selected == null) game else withFallbackTo(game)
    // Like store links, media falls back as a whole: a release without any shows its game's videos and screenshots.
    val ownMedia = mediaGallery(videos, screenshots)
    return shown.toPageDetails(
        id = game.id.orZero(),
        selectedVariant = selected,
        relations = gameRelations(game, selected),
        media = if (ownMedia.isEmpty && game !== this) mediaGallery(game.videos, game.screenshots) else ownMedia,
        // The API sends no images or text for an adult game, so the page only tags it and explains why.
        adult = adult || game.adult,
    )
}

/**
 * This release's own data, field by field, with [game]'s where this release has none. Store links fall back as a
 * whole: a release no store sells, such as a boxed collector's edition or a free update, is obtained through its game.
 */
private fun GameItem.withFallbackTo(game: GameItem): GameItem {
    val hasDate = !releaseDates.isNullOrEmpty() || firstReleaseDate != null
    return copy(
        name = name?.takeIf(String::isNotBlank) ?: game.name,
        cover = cover?.takeIf { !it.url.isNullOrBlank() } ?: game.cover,
        firstReleaseDate = if (hasDate) firstReleaseDate else game.firstReleaseDate,
        releaseDates = if (hasDate) releaseDates else game.releaseDates,
        genres = genres?.takeIf { it.isNotEmpty() } ?: game.genres,
        platforms = platforms?.takeIf { it.isNotEmpty() } ?: game.platforms,
        websites = websites?.takeIf { it.toStoreTypes().isNotEmpty() } ?: game.websites,
        summary = summary?.takeIf(String::isNotBlank) ?: game.summary,
        developers = developers?.takeIf { it.isNotEmpty() } ?: game.developers,
        publishers = publishers?.takeIf { it.isNotEmpty() } ?: game.publishers,
        gameModes = gameModes?.takeIf { it.isNotEmpty() } ?: game.gameModes,
        playerPerspectives = playerPerspectives?.takeIf { it.isNotEmpty() } ?: game.playerPerspectives,
        themes = themes?.takeIf { it.isNotEmpty() } ?: game.themes,
        series = series?.takeIf { it.isNotEmpty() } ?: game.series,
        criticRating = criticRating ?: game.criticRating,
        userRating = userRating ?: game.userRating,
        timeToBeat = timeToBeat ?: game.timeToBeat,
        status = status ?: game.status,
        storyline = storyline?.takeIf(String::isNotBlank) ?: game.storyline,
    )
}

/** This page with [id] selected among its versions, while that version's details load. */
private fun GameDetails.selecting(id: Int): GameDetails =
    copy(relations = relations.copy(versions = relations.versions?.copy(selectedId = id)))

private fun GameItem.toSelectedVariant(): SelectedVariant? {
    val id = id ?: return null
    return SelectedVariant(
        id = id,
        name = name.orEmpty(),
        kind = kind ?: GameKind.OTHER,
    )
}

private fun GameItem.toPageDetails(
    id: Int,
    selectedVariant: SelectedVariant?,
    relations: GameRelations,
    media: MediaGallery,
    adult: Boolean,
) = GameDetails(
    id = id,
    isAdult = adult,
    name = name.orEmpty(),
    summary = summary.orEmpty(),
    storyline =
        storyline
            .orEmpty()
            .trim()
            .takeUnless { it == summary.orEmpty().trim() }
            .orEmpty(),
    facts = toFacts(),
    playTimes = timeToBeat?.toPlayTimes(),
    badges = listOfNotNull(GameBadge.Adult.takeIf { adult }) + toBadges(),
    overview =
        overviewHighlights(
            modes = gameModes.names(),
            themes = themes.names(),
            perspectives = playerPerspectives.names(),
            genres = genres.orEmpty().mapNotNull { it.name },
            mainStorySeconds = timeToBeat?.hastily,
        ),
    // The same size and URL the cards request: the file is usually already in the disk cache (the browser's HTTP
    // cache on Web) from the card that opened the screen, and the original (about 9x the bytes) is never
    // downloaded or decoded.
    coverImageUrl =
        cover
            ?.url
            ?.toCoverUrl(CoverSize.P_720)
            .orEmpty(),
    backdrop = backdrop?.url?.takeIf(String::isNotBlank)?.toBackdropImage(),
    releaseDate = releaseDates?.firstOrNull()?.human ?: firstReleaseDate?.formatDate(),
    platforms = platforms.orEmpty().parentPlatformTypes(),
    stores = websites.orEmpty().toStoreTypes(),
    genres =
        genres.orEmpty().map { genre ->
            GenreTag(
                id = genre.id.orZero(),
                name = genre.name.orEmpty(),
            )
        },
    selectedVariant = selectedVariant,
    relations = relations,
    media = media,
)

private fun GameItem.toFacts(): GameFactsModel {
    val developer = developers.fact(Res.string.about_developer)
    val publisher = publishers.fact(Res.string.about_publisher)
    val credits =
        if (developer != null && publisher != null && developers.companyIds() == publishers.companyIds()) {
            listOf(developer.copy(label = Res.string.about_developer_publisher))
        } else {
            listOfNotNull(developer, publisher)
        }
    return GameFactsModel(
        rows = credits + listOfNotNull(series.fact(Res.string.about_series)),
        tags = (gameModes.orEmpty() + playerPerspectives.orEmpty() + themes.orEmpty()).names(),
    )
}

private fun List<NamedItem>?.companyIds() =
    orEmpty()
        .asSequence()
        .filter { it.name.isNotBlank() }
        .map(NamedItem::id)
        .toSet()

private fun List<NamedItem>?.names() = orEmpty().map { it.name.trim() }.filter(String::isNotEmpty).distinct()

private fun List<NamedItem>?.fact(label: StringResource) =
    names().takeIf { it.isNotEmpty() }?.let { GameFact(label, it.joinToString(", ")) }

private fun TimeToBeat.toPlayTimes(): GamePlayTimes? {
    val longest = maxOf(hastily ?: 0, normally ?: 0, completely ?: 0)
    if (longest <= 0) return null

    fun estimate(
        label: StringResource,
        seconds: Long?,
    ): PlayTime? {
        if (seconds == null) return null
        val hours = roundedPlayHours(seconds) ?: return null
        return PlayTime(label, hours, (seconds.toDouble() / longest).toFloat())
    }

    return GamePlayTimes(
        estimates =
            listOfNotNull(
                estimate(Res.string.about_main, hastily),
                estimate(Res.string.about_extra, normally),
                estimate(Res.string.about_complete, completely),
            ),
        sampleCount = count.takeIf { it > 0 },
    )
}

private fun GameItem.toBadges() =
    listOfNotNull(
        criticRating.badge(Res.string.about_critic),
        userRating.badge(Res.string.about_user),
        GameBadge.Status.entries.firstOrNull { it.apiName == status },
    )

private fun GameRating?.badge(label: StringResource): GameBadge? =
    this?.takeIf { it.score in 0..MAX_RATING }?.let { GameBadge.Score(label, it.score) }

private const val MAX_RATING = 100

internal data class GameFactsModel(
    val rows: List<GameFact> = emptyList(),
    val tags: List<String> = emptyList(),
) {
    val isEmpty: Boolean get() = rows.isEmpty() && tags.isEmpty()
}

internal data class GameFact(
    val label: StringResource,
    val value: String,
)

internal data class GamePlayTimes(
    val estimates: List<PlayTime>,
    val sampleCount: Int? = null,
)

internal data class PlayTime(
    val label: StringResource,
    val hours: Long,
    /** The share of the longest available estimate, calculated before rounding the displayed hours. */
    val relativeLength: Float,
)

internal sealed interface GameBadge {
    /** An adult game: the API sends no images or description for it. */
    data object Adult : GameBadge

    data class Score(
        val label: StringResource,
        val score: Int,
    ) : GameBadge

    enum class Status(
        val apiName: String,
        val label: StringResource,
    ) : GameBadge {
        Alpha("alpha", Res.string.status_alpha),
        Beta("beta", Res.string.status_beta),
        EarlyAccess("early_access", Res.string.status_early_access),
        Offline("offline", Res.string.status_offline),
        Cancelled("cancelled", Res.string.status_cancelled),
        Rumored("rumored", Res.string.status_rumored),
        Delisted("delisted", Res.string.status_delisted),
    }
}

/**
 * The API's backdrop at the sizes the hero draws it: IGDB's small size on phones, where it sits dimmed and the data
 * matters more, and a ladder up to 1080p for wide layouts, which pick by the window's width.
 */
private fun String.toBackdropImage() =
    BackdropImage(
        compactUrl = toCoverUrl(CoverSize.SCREENSHOT_MED),
        wideUrl = toCoverUrl(CoverSize.SCREENSHOT_BIG),
        hdUrl = toCoverUrl(CoverSize.P_720),
        fullHdUrl = toCoverUrl(CoverSize.P_1080),
    )

// Cards without an ID cannot be opened, and lazy lists need unique keys.
private fun List<SimilarGame>.toCards() =
    mapNotNull { game ->
        val id = game.id ?: return@mapNotNull null
        GameCardModel(
            id = id,
            title = game.name.orEmpty(),
            backgroundImageUrl =
                game.cover
                    ?.url
                    ?.toCoverUrl(CoverSize.P_720)
                    .orEmpty(),
        )
    }.distinctBy { it.id }

sealed interface RecommendationsState {
    data object Loading : RecommendationsState

    data class Success(
        val games: List<GameCardModel>,
    ) : RecommendationsState

    /** The request failed; the optional section stays hidden until a refresh. */
    data object Unavailable : RecommendationsState
}

internal sealed class GameDetailsState {
    data object Loading : GameDetailsState()

    /** The backend has no game with this ID. */
    data object NotFound : GameDetailsState()

    data class Success(
        val gameDetails: GameDetails,
        /** Another version is loading in place; the release's own details are about to change. */
        val isSwitchingVersion: Boolean = false,
    ) : GameDetailsState()

    data class Error(
        val error: DataError,
    ) : GameDetailsState()
}

internal data class GameDetails(
    /** The game's ID; on an edition's or update's page, the ID of its game, whose relations the page shows. */
    val id: Int,
    /** The shown release's name: the selected edition's or update's on its page. */
    val name: String,
    val summary: String,
    val storyline: String,
    val coverImageUrl: String,
    /** The hero's background, or null when the game has no still screenshot. */
    val backdrop: BackdropImage?,
    val releaseDate: String?,
    val platforms: List<ParentPlatformType>,
    val stores: List<StoreTypeModel>,
    val genres: List<GenreTag>,
    /** The edition or update the page was opened for, or null on the game's own page. */
    val selectedVariant: SelectedVariant? = null,
    /** The game it belongs to, its versions, and its related releases. */
    val relations: GameRelations = GameRelations(),
    /** The shown release's videos and screenshots; empty when it has none, which hides the section. */
    val media: MediaGallery = MediaGallery(),
    val facts: GameFactsModel = GameFactsModel(),
    val playTimes: GamePlayTimes? = null,
    val badges: List<GameBadge> = emptyList(),
    val overview: List<OverviewHighlight> = emptyList(),
    /** An adult game, whose images and description the API withholds. */
    val isAdult: Boolean = false,
) {
    /** The shown release: the selected edition or update, otherwise the game. A share points to it. */
    val releaseId: Int get() = selectedVariant?.id ?: id
}

/**
 * The backdrop at a small size for compact layouts, and at IGDB's larger sizes for wide ones: 889 px wide
 * ([wideUrl]), 720p ([hdUrl]), and 1080p ([fullHdUrl]).
 */
data class BackdropImage(
    val compactUrl: String,
    val wideUrl: String,
    val hdUrl: String,
    val fullHdUrl: String,
) {
    /**
     * The wide layout's image for a backdrop drawn up to [widthPx] pixels wide: the smallest size that fills it, so a
     * large or high-density screen gets the sharpest file IGDB resizes to, and a small window no more than it shows.
     */
    fun wideUrlFor(widthPx: Int): String =
        when {
            widthPx <= SCREENSHOT_BIG_WIDTH_PX -> wideUrl
            widthPx <= P_720_WIDTH_PX -> hdUrl
            else -> fullHdUrl
        }
}

// The widths of IGDB's screenshot_big and 720p sizes.
private const val SCREENSHOT_BIG_WIDTH_PX = 889
private const val P_720_WIDTH_PX = 1280

data class GenreTag(
    val id: Int,
    val name: String,
)
