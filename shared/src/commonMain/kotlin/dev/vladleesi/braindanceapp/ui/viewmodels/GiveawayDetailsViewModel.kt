// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.giveawayPlatforms
import dev.vladleesi.braindanceapp.utils.orZero
import dev.vladleesi.braindanceapp.utils.toSafeExternalUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GiveawayDetailsViewModel(
    private val giveawayId: Int,
    private val gamerPowerRepo: GamerPowerRepo,
) : ScreenViewModel() {
    // A giveaway opened from the list starts as listed, so the screen's first frame shows it without a request or a
    // loading frame. Direct links and giveaways outside the list start loading and ask the backend.
    private val _giveawayDetailsState =
        MutableStateFlow(
            gamerPowerRepo.listedGiveaway(giveawayId)?.let { GiveawayDetailsState.Success(it.toGiveawayDetails()) }
                ?: GiveawayDetailsState.Loading,
        )
    val giveawayDetailsState: StateFlow<GiveawayDetailsState> = _giveawayDetailsState.asStateFlow()

    // One running load: a refresh cancels the previous one, so an older response can never replace a newer one.
    private var loadJob: Job? = null

    override fun onLoad() {
        if (_giveawayDetailsState.value == GiveawayDetailsState.Loading) fetch()
    }

    override fun onRefresh() {
        fetch()
    }

    private fun fetch() {
        loadJob?.cancel()
        loadJob =
            viewModelScope.launch {
                _giveawayDetailsState.value = GiveawayDetailsState.Loading
                _giveawayDetailsState.value =
                    when (val outcome = gamerPowerRepo.giveaway(giveawayId)) {
                        is Outcome.Success -> GiveawayDetailsState.Success(outcome.value.toGiveawayDetails())
                        is Outcome.Failure ->
                            if (outcome.error == DataError.NotFound) {
                                GiveawayDetailsState.NotFound
                            } else {
                                BLogger.error(message = "Giveaway unavailable", throwable = outcome.cause)
                                GiveawayDetailsState.Error(outcome.error)
                            }
                    }
            }
    }
}

sealed class GiveawayDetailsState {
    data object Loading : GiveawayDetailsState()

    /** The backend has no giveaway with this ID. */
    data object NotFound : GiveawayDetailsState()

    data class Success(
        val giveawayDetails: GiveawayDetails,
    ) : GiveawayDetailsState()

    data class Error(
        val error: DataError,
    ) : GiveawayDetailsState()
}

/** What the giveaway screen shows; [openUrl] is null when the giveaway has no safe link to open. */
data class GiveawayDetails(
    val title: String,
    val imageUrl: String,
    val worth: String,
    val claimedCount: Int,
    val platforms: List<ParentPlatformType>,
    val description: String,
    val instructions: String,
    val openUrl: String?,
)

// GamerPower writes "N/A" for an unknown worth; it is shown as no worth.
private const val UNKNOWN_WORTH = "N/A"

internal fun GiveawayResponse.toGiveawayDetails() =
    GiveawayDetails(
        title = title.orEmpty(),
        imageUrl = image.orEmpty(),
        worth = worth.takeIf { it != UNKNOWN_WORTH }.orEmpty(),
        claimedCount = users.orZero(),
        platforms = platforms?.giveawayPlatforms().orEmpty(),
        description = description.orEmpty(),
        instructions = instructions.orEmpty(),
        openUrl = openGiveawayUrl.toSafeExternalUrl(),
    )
