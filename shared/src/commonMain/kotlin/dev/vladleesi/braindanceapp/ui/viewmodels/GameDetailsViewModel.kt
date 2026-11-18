// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.data.models.games.Screenshot
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
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

class GameDetailsViewModel(
    private val gameId: Int,
    private val gameDetailsRepo: GameDetailsRepo,
) : ScreenViewModel() {
    private val _gameDetailsState = MutableStateFlow<GameDetailsState>(GameDetailsState.Loading)
    val gameDetailsState: StateFlow<GameDetailsState> = _gameDetailsState.asStateFlow()

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
        val shown = (_gameDetailsState.value as? GameDetailsState.Success)?.gameDetails
        if (shown == null) {
            loadDetails(id, inPlace = false)
            return
        }
        _gameDetailsState.value = GameDetailsState.Success(shown.selecting(id), isSwitchingVersion = true)
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
                    _gameDetailsState.value = GameDetailsState.Loading
                    loadRecommendations(id)
                }
                _gameDetailsState.value =
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
 * its game: its own name, cover, release date, platforms, genres, stores, summary, and screenshot, with the game's
 * for anything IGDB lacks for it, and the game's versions and related releases with it selected. The page keeps the
 * game's ID, so relations and similar games follow the game. Without a canonical game, the entry is shown as a game
 * of its own.
 */
private fun GameItem.toGameDetails(): GameDetails {
    val game = canonical?.takeIf { canonicalId != null && canonicalId != id } ?: this
    val selected = if (game === this) null else toSelectedVariant()
    val shown = if (selected == null) game else withFallbackTo(game)
    return shown.toPageDetails(
        id = game.id.orZero(),
        selectedVariant = selected,
        relations = gameRelations(game, selected),
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
        screenshots = screenshots?.takeIf { it.toBackdropImage() != null } ?: game.screenshots,
        summary = summary?.takeIf(String::isNotBlank) ?: game.summary,
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
) = GameDetails(
    id = id,
    name = name.orEmpty(),
    summary = summary.orEmpty(),
    storyline = storyline.orEmpty(),
    // The same size and URL the cards request: the file is usually already in the disk cache (the browser's HTTP
    // cache on Web) from the card that opened the screen, and the original (about 9x the bytes) is never
    // downloaded or decoded.
    coverImageUrl =
        cover
            ?.url
            ?.toCoverUrl(CoverSize.P_720)
            .orEmpty(),
    backdrop = screenshots.orEmpty().toBackdropImage(),
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
)

/**
 * The first still screenshot at the two sizes the hero draws it. It sits dimmed behind the hero, where IGDB's small
 * sizes are enough: a larger file would only add bytes and decoding the scrim hides.
 */
private fun List<Screenshot>.toBackdropImage(): BackdropImage? {
    val url = firstOrNull { screenshot -> screenshot.animated != true && !screenshot.url.isNullOrBlank() }?.url
    return url?.let {
        BackdropImage(
            compactUrl = it.toCoverUrl(CoverSize.SCREENSHOT_MED),
            wideUrl = it.toCoverUrl(CoverSize.SCREENSHOT_BIG),
        )
    }
}

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

sealed class GameDetailsState {
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

data class GameDetails(
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
) {
    /** The shown release: the selected edition or update, otherwise the game. A share points to it. */
    val releaseId: Int get() = selectedVariant?.id ?: id
}

/** One screenshot at a smaller size for compact layouts and a larger one for wide layouts. */
data class BackdropImage(
    val compactUrl: String,
    val wideUrl: String,
)

data class GenreTag(
    val id: Int,
    val name: String,
)
