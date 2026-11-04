// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.repository.GameDetailsRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardModel
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

    override fun onLoad() {
        detailsJob?.cancel()
        detailsJob =
            viewModelScope.launch {
                _gameDetailsState.value = GameDetailsState.Loading
                loadRecommendations(gameId)
                _gameDetailsState.value =
                    when (val outcome = gameDetailsRepo.gameDetails(gameId)) {
                        is Outcome.Success -> GameDetailsState.Success(outcome.value.toGameDetails())
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

    override fun onRefresh() {
        onLoad()
    }

    private fun loadRecommendations(gameId: Int) {
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

private fun GameItem.toGameDetails() =
    GameDetails(
        id = id.orZero(),
        name = name.orEmpty(),
        summary = summary.orEmpty(),
        storyline = storyline.orEmpty(),
        coverImageUrl =
            cover
                ?.url
                ?.toCoverUrl(CoverSize.ORIGINAL)
                .orEmpty(),
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
    )

// Cards without an ID cannot be opened, and lazy lists need unique keys.
private fun List<SimilarGame>.toCards() =
    mapNotNull { game ->
        val id = game.id ?: return@mapNotNull null
        MiniGameCardModel(
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
        val games: List<MiniGameCardModel>,
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
    ) : GameDetailsState()

    data class Error(
        val error: DataError,
    ) : GameDetailsState()
}

data class GameDetails(
    val id: Int,
    val name: String,
    val summary: String,
    val storyline: String,
    val coverImageUrl: String,
    val releaseDate: String?,
    val platforms: List<ParentPlatformType>,
    val stores: List<StoreTypeModel>,
    val genres: List<GenreTag>,
)

data class GenreTag(
    val id: Int,
    val name: String,
)
