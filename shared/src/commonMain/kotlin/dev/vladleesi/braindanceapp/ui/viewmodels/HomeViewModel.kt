// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardModel
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.formatEndDate
import dev.vladleesi.braindanceapp.utils.giveawayPlatforms
import dev.vladleesi.braindanceapp.utils.orZero
import dev.vladleesi.braindanceapp.utils.parentPlatformTypes
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepo: HomeRepo,
    private val gamerPowerRepo: GamerPowerRepo,
) : ScreenViewModel() {
    private val _mostAnticipated = MutableStateFlow<HomeState>(HomeState.Loading)
    val mostAnticipated: StateFlow<HomeState> = _mostAnticipated.asStateFlow()

    private val _giveaways = MutableStateFlow<HomeState>(HomeState.Loading)
    val giveaways: StateFlow<HomeState> = _giveaways.asStateFlow()

    private val _popularRightNow = MutableStateFlow<HomeState>(HomeState.Loading)
    val popularRightNow: StateFlow<HomeState> = _popularRightNow.asStateFlow()

    private val handler =
        CoroutineExceptionHandler { _, exception ->
            BLogger.error(message = exception.message.orEmpty(), throwable = exception)
        }

    override fun onLoad() {
        loadMostAnticipated()
        loadGiveaways()
        loadPopularRightNow()
    }

    override fun onRefresh() {
        onLoad()
    }

    fun loadMostAnticipated(pageSize: Int = PAGE_SIZE) {
        viewModelScope.launch(handler) {
            runCatching {
                _mostAnticipated.emit(HomeState.Loading)
                val gameItems = homeRepo.mostAnticipated(pageSize = pageSize)
                gameItems.orEmpty().mapperMiniGameCardModel()
            }.onSuccess { result ->
                _mostAnticipated.emit(HomeState.Success(result))
            }.onFailure { throwable ->
                _mostAnticipated.emit(HomeState.Error(throwable.message.orEmpty()))
            }
        }
    }

    fun loadGiveaways(pageSize: Int = PAGE_SIZE) {
        viewModelScope.launch(handler) {
            runCatching {
                _giveaways.emit(HomeState.Loading)
                val giveaways = gamerPowerRepo.giveaways(pageSize = pageSize)
                giveaways.orEmpty().mapperGiveawayModel()
            }.onSuccess { result ->
                _giveaways.emit(HomeState.Success(result))
            }.onFailure { throwable ->
                _giveaways.emit(HomeState.Error(throwable.message.orEmpty()))
            }
        }
    }

    fun loadPopularRightNow(pageSize: Int = PAGE_SIZE) {
        viewModelScope.launch(handler) {
            runCatching {
                _popularRightNow.emit(HomeState.Loading)
                val gameItems = homeRepo.popularRightNow(pageSize = pageSize)
                gameItems.orEmpty().mapperMiniGameCardModel()
            }.onSuccess { result ->
                _popularRightNow.emit(HomeState.Success(result))
            }.onFailure { throwable ->
                _popularRightNow.emit(HomeState.Error(throwable.message.orEmpty()))
            }
        }
    }

    private fun List<GameItem>.mapperMiniGameCardModel() =
        map { item ->
            MiniGameCardModel(
                id = item.id.orZero(),
                title = item.name.orEmpty(),
                backgroundImageUrl =
                    item.cover
                        ?.url
                        ?.toCoverUrl(CoverSize.P_720)
                        .orEmpty(),
                platforms = item.platforms.orEmpty().parentPlatformTypes(),
            )
        }

    private fun List<GiveawayResponse>.mapperGiveawayModel() =
        map { item ->
            GiveawayItemModel(
                id = item.id.orZero(),
                title = item.title.orEmpty(),
                image = item.image.orEmpty(),
                worth = item.worth.takeIf { it != "N/A" }.orEmpty(),
                endDate = item.endDate?.formatEndDate().orEmpty(),
                platforms = item.platforms?.giveawayPlatforms().orEmpty(),
                type = item.type.orEmpty(),
            )
        }

    private companion object {
        private const val PAGE_SIZE = 20
    }
}

sealed interface HomeState {
    data object Loading : HomeState

    data class Success<T : HomeStateEntity>(
        val entities: List<T>,
    ) : HomeState

    data class Error(
        val message: String,
    ) : HomeState
}

interface HomeStateEntity
