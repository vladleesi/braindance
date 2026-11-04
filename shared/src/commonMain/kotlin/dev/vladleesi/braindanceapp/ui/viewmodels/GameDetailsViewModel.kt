// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
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
import dev.vladleesi.braindanceapp.utils.suspendRunCatching
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import dev.vladleesi.braindanceapp.utils.toStoreTypes
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameDetailsViewModel(
    private val gameId: Int?,
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
                if (gameId == null) {
                    _gameDetailsState.value = GameDetailsState.Error("Invalid game ID: $gameId")
                    return@launch
                }
                loadRecommendations(gameId)
                _gameDetailsState.value =
                    suspendRunCatching { gameDetailsRepo.gameDetails(gameId)?.firstOrNull().toGameDetails() }.fold(
                        onSuccess = { details -> GameDetailsState.Success(details) },
                        onFailure = { throwable ->
                            BLogger.error(message = throwable.message.orEmpty(), throwable = throwable)
                            GameDetailsState.Error(throwable.message.orEmpty())
                        },
                    )
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
                    suspendRunCatching { gameDetailsRepo.recommendations(gameId) }.fold(
                        onSuccess = { games -> RecommendationsState.Success(games.toCards()) },
                        onFailure = { throwable ->
                            // The section is optional: keep the details visible and leave it hidden until a refresh.
                            BLogger.error(
                                message = "Recommendations unavailable: ${throwable.message.orEmpty()}",
                                throwable = throwable,
                            )
                            RecommendationsState.Unavailable
                        },
                    )
            }
    }
}

private fun GameItem?.toGameDetails() =
    GameDetails(
        id = this?.id.orZero(),
        name = this?.name.orEmpty(),
        summary = this?.summary.orEmpty(),
        storyline = this?.storyline.orEmpty(),
        coverImageUrl =
            this
                ?.cover
                ?.url
                ?.toCoverUrl(CoverSize.ORIGINAL)
                .orEmpty(),
        releaseDate = this?.releaseDates?.firstOrNull()?.human ?: this?.firstReleaseDate?.formatDate(),
        platforms = this?.platforms.orEmpty().parentPlatformTypes(),
        stores = this?.websites.orEmpty().toStoreTypes(),
        genres =
            this?.genres.orEmpty().map { genre ->
                GenreTag(
                    id = genre.id.orZero(),
                    name = genre.name.orEmpty(),
                )
            },
    )

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
    }

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

    data class Success(
        val gameDetails: GameDetails,
    ) : GameDetailsState()

    data class Error(
        val message: String,
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
