// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.MiniGameCardModel
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.formatEndDate
import dev.vladleesi.braindanceapp.utils.giveawayPlatforms
import dev.vladleesi.braindanceapp.utils.orZero
import dev.vladleesi.braindanceapp.utils.suspendRunCatching
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class HomeViewModel(
    private val homeRepo: HomeRepo,
    private val gamerPowerRepo: GamerPowerRepo,
) : ScreenViewModel() {
    private val _mostAnticipated = MutableStateFlow<HomeState>(HomeState.Loading)
    val mostAnticipated: StateFlow<HomeState> = _mostAnticipated.asStateFlow()

    private val _giveaways = MutableStateFlow<HomeState>(HomeState.Loading)
    val giveaways: StateFlow<HomeState> = _giveaways.asStateFlow()

    private val _trendingNow = MutableStateFlow<HomeState>(HomeState.Loading)
    val trendingNow: StateFlow<HomeState> = _trendingNow.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)

    /** True from a pull-to-refresh until all three sections have finished reloading. */
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    // One running load per section: starting a section again cancels its previous load, so an older response can never
    // replace a newer one.
    private var trendingJob: Job? = null
    private var mostAnticipatedJob: Job? = null
    private var giveawaysJob: Job? = null
    private var refreshJob: Job? = null

    override fun onLoad() {
        loadTrendingNow()
        loadMostAnticipated()
        loadGiveaways()
    }

    override fun onRefresh() {
        refreshJob?.cancel()
        _isRefreshing.value = true
        onLoad()
        val sections = listOfNotNull(trendingJob, mostAnticipatedJob, giveawaysJob)
        refreshJob =
            viewModelScope.launch {
                sections.joinAll()
                _isRefreshing.value = false
            }
    }

    fun loadMostAnticipated(pageSize: Int = PAGE_SIZE) {
        mostAnticipatedJob =
            loadSection(mostAnticipatedJob, _mostAnticipated) {
                homeRepo.mostAnticipated(pageSize = pageSize).orEmpty().mapperMiniGameCardModel()
            }
    }

    fun loadGiveaways(pageSize: Int = PAGE_SIZE) {
        giveawaysJob =
            loadSection(giveawaysJob, _giveaways) {
                gamerPowerRepo.giveaways(pageSize = pageSize).orEmpty().mapperGiveawayModel()
            }
    }

    fun loadTrendingNow() {
        trendingJob =
            loadSection(trendingJob, _trendingNow) {
                homeRepo.trendingNow().map { it.toMiniGameCardModel() }
            }
    }

    private fun loadSection(
        previous: Job?,
        state: MutableStateFlow<HomeState>,
        load: suspend () -> List<HomeStateEntity>,
    ): Job {
        previous?.cancel()
        return viewModelScope.launch {
            state.value = HomeState.Loading
            state.value =
                suspendRunCatching { load() }.fold(
                    onSuccess = { entities -> HomeState.Success(entities) },
                    onFailure = { throwable -> HomeState.Error(throwable.message.orEmpty()) },
                )
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

/** Maps a trending card; a reason this client does not know is omitted rather than shown generically. */
internal fun TrendingGameItem.toMiniGameCardModel() =
    MiniGameCardModel(
        id = id,
        title = name.orEmpty(),
        backgroundImageUrl = cover?.url?.toCoverUrl(CoverSize.P_720).orEmpty(),
        label =
            when (reason) {
                "new_release" -> MiniGameCardLabel.NewRelease
                "full_release" -> MiniGameCardLabel.FullRelease
                "showcased" -> MiniGameCardLabel.Showcased
                "new_platform" -> MiniGameCardLabel.NewPlatform(platform?.takeIf(String::isNotBlank))
                else -> null
            },
    )

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
