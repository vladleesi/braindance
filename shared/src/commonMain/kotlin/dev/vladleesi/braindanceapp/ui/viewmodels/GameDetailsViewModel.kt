// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
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

    private val _recommendations = MutableStateFlow<List<MiniGameCardModel>>(emptyList())

    /** Loaded independently so a slow or failed recommendation request never delays or replaces the details. */
    val recommendations: StateFlow<List<MiniGameCardModel>> = _recommendations.asStateFlow()

    private val handler =
        CoroutineExceptionHandler { _, exception ->
            BLogger.error(message = exception.message.orEmpty(), throwable = exception)
            _gameDetailsState.value = GameDetailsState.Error(exception.message.orEmpty())
        }

    override fun onLoad() {
        viewModelScope.launch(handler) {
            _gameDetailsState.emit(GameDetailsState.Loading)
            if (gameId == null) {
                _gameDetailsState.emit(GameDetailsState.Error("Invalid game ID: $gameId"))
                return@launch
            }
            loadRecommendations(gameId)
            val game = gameDetailsRepo.gameDetails(gameId.orZero())?.firstOrNull()
            val gameDetails =
                GameDetails(
                    id = game?.id.orZero(),
                    name = game?.name.orEmpty(),
                    summary = game?.summary.orEmpty(),
                    storyline = game?.storyline.orEmpty(),
                    coverImageUrl =
                        game
                            ?.cover
                            ?.url
                            ?.toCoverUrl(CoverSize.ORIGINAL)
                            .orEmpty(),
                    releaseDate = game?.releaseDates?.firstOrNull()?.human ?: game?.firstReleaseDate?.formatDate(),
                    platforms = game?.platforms.orEmpty().parentPlatformTypes(),
                    stores = game?.websites.orEmpty().toStoreTypes(),
                    genres =
                        game?.genres.orEmpty().map { genre ->
                            GenreTag(
                                id = genre.id.orZero(),
                                name = genre.name.orEmpty(),
                            )
                        },
                )
            _gameDetailsState.emit(
                GameDetailsState.Success(gameDetails),
            )
        }
    }

    override fun onRefresh() {
        onLoad()
    }

    private fun loadRecommendations(gameId: Int) {
        viewModelScope.launch {
            val games =
                try {
                    gameDetailsRepo.recommendations(gameId)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    // The section is optional: keep the details visible and leave it hidden until a refresh.
                    BLogger.error(message = "Recommendations unavailable: ${e.message.orEmpty()}", throwable = e)
                    return@launch
                }
            _recommendations.value =
                games.mapNotNull { game ->
                    val id = game.id ?: return@mapNotNull null
                    MiniGameCardModel(
                        id = id,
                        title = game.name.orEmpty(),
                        backgroundImageUrl =
                            game.cover
                                ?.url
                                ?.toCoverUrl(CoverSize.P_720)
                                .orEmpty(),
                        platforms = emptyList(),
                    )
                }
        }
    }
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
