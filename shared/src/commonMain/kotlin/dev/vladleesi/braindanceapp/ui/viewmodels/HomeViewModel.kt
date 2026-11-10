// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.map
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.data.repository.HomeRepo
import dev.vladleesi.braindanceapp.ui.components.giveaways.GiveawayItemModel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardLabel
import dev.vladleesi.braindanceapp.ui.components.home.GameCardModel
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.formatEndDate
import dev.vladleesi.braindanceapp.utils.giveawayPlatforms
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
    private val _mostAnticipated = MutableStateFlow<SectionState<GameCardModel>>(SectionState.Loading)
    val mostAnticipated: StateFlow<SectionState<GameCardModel>> = _mostAnticipated.asStateFlow()

    private val _giveaways = MutableStateFlow<SectionState<GiveawayItemModel>>(SectionState.Loading)
    val giveaways: StateFlow<SectionState<GiveawayItemModel>> = _giveaways.asStateFlow()

    private val _trendingNow = MutableStateFlow<SectionState<GameCardModel>>(SectionState.Loading)
    val trendingNow: StateFlow<SectionState<GameCardModel>> = _trendingNow.asStateFlow()

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
                homeRepo.mostAnticipated(pageSize = pageSize).map { it.toGameCardModels() }
            }
    }

    fun loadGiveaways(pageSize: Int = PAGE_SIZE) {
        giveawaysJob =
            loadSection(giveawaysJob, _giveaways) {
                gamerPowerRepo.giveaways(pageSize = pageSize).map { it.mapperGiveawayModel() }
            }
    }

    fun loadTrendingNow() {
        trendingJob =
            loadSection(trendingJob, _trendingNow) {
                homeRepo.trendingNow().map { items -> items.map { it.toGameCardModel() }.distinctBy { it.id } }
            }
    }

    private fun <T> loadSection(
        previous: Job?,
        state: MutableStateFlow<SectionState<T>>,
        load: suspend () -> Outcome<List<T>>,
    ): Job {
        previous?.cancel()
        return viewModelScope.launch {
            state.value = SectionState.Loading
            state.value =
                when (val outcome = load()) {
                    is Outcome.Success -> SectionState.Success(outcome.value)
                    is Outcome.Failure -> SectionState.Error(outcome.error)
                }
        }
    }

    // Cards without an ID cannot be opened, and lazy lists need unique keys.
    private fun List<GameItem>.toGameCardModels() =
        mapNotNull { item ->
            val id = item.id ?: return@mapNotNull null
            GameCardModel(
                id = id,
                title = item.name.orEmpty(),
                backgroundImageUrl =
                    item.cover
                        ?.url
                        ?.toCoverUrl(CoverSize.P_720)
                        .orEmpty(),
            )
        }.distinctBy { it.id }

    private fun List<GiveawayResponse>.mapperGiveawayModel() =
        mapNotNull { item ->
            val id = item.id ?: return@mapNotNull null
            GiveawayItemModel(
                id = id,
                title = item.title.orEmpty(),
                image = item.image.orEmpty(),
                worth = item.worth.takeIf { it != "N/A" }.orEmpty(),
                endDate = item.endDate?.formatEndDate().orEmpty(),
                platforms = item.platforms?.giveawayPlatforms().orEmpty(),
                type = item.type.orEmpty(),
            )
        }.distinctBy { it.id }

    private companion object {
        private const val PAGE_SIZE = 20
    }
}

/** Maps a trending card; a reason this client does not know is omitted rather than shown generically. */
internal fun TrendingGameItem.toGameCardModel() =
    GameCardModel(
        id = id,
        title = name.orEmpty(),
        backgroundImageUrl = cover?.url?.toCoverUrl(CoverSize.P_720).orEmpty(),
        label =
            when (reason) {
                "new_release" -> GameCardLabel.NewRelease
                "full_release" -> GameCardLabel.FullRelease
                "showcased" -> GameCardLabel.Showcased
                "new_platform" -> GameCardLabel.NewPlatform(platform?.takeIf(String::isNotBlank))
                else -> null
            },
    )
