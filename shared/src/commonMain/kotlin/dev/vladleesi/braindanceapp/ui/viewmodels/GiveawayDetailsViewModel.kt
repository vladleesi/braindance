// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GiveawayDetailsViewModel(
    private val giveawayId: Int?,
    private val gamerPowerRepo: GamerPowerRepo,
) : ScreenViewModel() {
    private val _giveawayDetailsState = MutableStateFlow<GiveawayDetailsState>(GiveawayDetailsState.Loading)
    val giveawayDetailsState: StateFlow<GiveawayDetailsState> = _giveawayDetailsState.asStateFlow()

    private val handler =
        CoroutineExceptionHandler { _, exception ->
            BLogger.error(message = exception.message.orEmpty(), throwable = exception)
            _giveawayDetailsState.value = GiveawayDetailsState.Error(exception.message.orEmpty())
        }

    override fun onLoad() {
        viewModelScope.launch(handler) {
            runCatching {
                _giveawayDetailsState.emit(GiveawayDetailsState.Loading)
                if (giveawayId == null) {
                    _giveawayDetailsState.emit(GiveawayDetailsState.Error("Invalid giveaway ID: $giveawayId"))
                    return@launch
                }
                val result = gamerPowerRepo.giveaway(giveawayId)
                requireNotNull(result)
            }.onSuccess { result ->
                _giveawayDetailsState.emit(GiveawayDetailsState.Success(result))
            }.onFailure { throwable ->
                _giveawayDetailsState.emit(GiveawayDetailsState.Error(throwable.message.orEmpty()))
            }
        }
    }

    override fun onRefresh() {
        onLoad()
    }
}

sealed class GiveawayDetailsState {
    data object Loading : GiveawayDetailsState()

    // TODO: Replace GiveawayResponse to GiveawayDetailsModel
    data class Success(
        val giveawayDetails: GiveawayResponse,
    ) : GiveawayDetailsState()

    data class Error(
        val message: String,
    ) : GiveawayDetailsState()
}
