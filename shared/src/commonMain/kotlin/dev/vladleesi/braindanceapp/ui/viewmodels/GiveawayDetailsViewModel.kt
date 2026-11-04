// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.utils.suspendRunCatching
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GiveawayDetailsViewModel(
    private val giveawayId: Int,
    private val gamerPowerRepo: GamerPowerRepo,
) : ScreenViewModel() {
    private val _giveawayDetailsState = MutableStateFlow<GiveawayDetailsState>(GiveawayDetailsState.Loading)
    val giveawayDetailsState: StateFlow<GiveawayDetailsState> = _giveawayDetailsState.asStateFlow()

    // One running load: a refresh cancels the previous one, so an older response can never replace a newer one.
    private var loadJob: Job? = null

    override fun onLoad() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _giveawayDetailsState.value = GiveawayDetailsState.Loading
                _giveawayDetailsState.value =
                    suspendRunCatching { gamerPowerRepo.giveaway(giveawayId) }.fold(
                        onSuccess = { giveaway ->
                            giveaway?.let { GiveawayDetailsState.Success(it) } ?: GiveawayDetailsState.NotFound
                        },
                        onFailure = { throwable ->
                            BLogger.error(message = throwable.message.orEmpty(), throwable = throwable)
                            GiveawayDetailsState.Error(throwable.message.orEmpty())
                        },
                    )
            }
    }

    override fun onRefresh() {
        onLoad()
    }
}

sealed class GiveawayDetailsState {
    data object Loading : GiveawayDetailsState()

    /** The backend returned no giveaway for this ID. */
    data object NotFound : GiveawayDetailsState()

    // TODO: Replace GiveawayResponse to GiveawayDetailsModel
    data class Success(
        val giveawayDetails: GiveawayResponse,
    ) : GiveawayDetailsState()

    data class Error(
        val message: String,
    ) : GiveawayDetailsState()
}
