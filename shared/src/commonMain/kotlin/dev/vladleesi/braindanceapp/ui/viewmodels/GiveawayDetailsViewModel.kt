// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import androidx.lifecycle.viewModelScope
import dev.vladleesi.braindanceapp.data.DataError
import dev.vladleesi.braindanceapp.data.Outcome
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import dev.vladleesi.braindanceapp.data.repository.GamerPowerRepo
import dev.vladleesi.braindanceapp.logger.BLogger
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.currentLocalDate
import dev.vladleesi.braindanceapp.utils.gamerPowerDateOrNull
import dev.vladleesi.braindanceapp.utils.giveawayPlatforms
import dev.vladleesi.braindanceapp.utils.orZero
import dev.vladleesi.braindanceapp.utils.toSafeExternalUrl
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

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

/**
 * What the giveaway screen shows. [openUrl] and [sourceUrl] are null when the giveaway has no safe link to open; an
 * empty [worth] or [type] is unknown.
 */
data class GiveawayDetails(
    val title: String,
    val imageUrl: String,
    /** The original price, such as "$4.99". */
    val worth: String,
    /** What is given away, such as "Game", "DLC", or "Early Access". */
    val type: String,
    val claimedCount: Int,
    /** The platforms that have a logo. */
    val platforms: List<ParentPlatformType>,
    /** Every platform and store as GamerPower names them, such as "PC" and "Epic Games Store". */
    val platformNames: List<String>,
    val description: String,
    /** The claim instructions, one entry per step and without their numbers; a single entry is plain text. */
    val claimSteps: List<String>,
    val ending: GiveawayEnding,
    /** The day GamerPower listed the giveaway. */
    val addedOn: LocalDate?,
    val openUrl: String?,
    /** The giveaway's page on GamerPower, the source of the data. */
    val sourceUrl: String?,
)

/** When a giveaway ends, decided on the day it was loaded. */
sealed interface GiveawayEnding {
    /** GamerPower gives no end date. */
    data object OpenEnded : GiveawayEnding

    /** It ends on [date], [daysLeft] days after today: 0 is today. */
    data class EndsOn(
        val date: LocalDate,
        val daysLeft: Int,
    ) : GiveawayEnding

    /** It is over: GamerPower marks it expired, or its end [date] has passed. */
    data class Ended(
        val date: LocalDate?,
    ) : GiveawayEnding
}

// GamerPower writes "N/A" for an unknown worth; it is shown as no worth.
private const val UNKNOWN_WORTH = "N/A"

// GamerPower's status of a giveaway that can no longer be claimed; active ones are "Active".
private const val EXPIRED_STATUS = "Expired"

// GamerPower separates instruction steps with line breaks, sometimes as HTML, and numbers them "1." or "1)".
private val InstructionLineBreak = Regex("""\r\n|\r|\n|<br\s*/?>""", RegexOption.IGNORE_CASE)
private val StepNumber = Regex("""^\d+\s*[.)]\s*""")

/** Maps the response for the screen; [today] decides how long it still runs. */
internal fun GiveawayResponse.toGiveawayDetails(today: LocalDate = currentLocalDate()) =
    GiveawayDetails(
        title = title.orEmpty(),
        imageUrl = image.orEmpty(),
        worth = worth.takeIf { it != UNKNOWN_WORTH }.orEmpty(),
        type = type?.trim().orEmpty(),
        claimedCount = users.orZero(),
        platforms = platforms?.giveawayPlatforms().orEmpty(),
        platformNames =
            platforms
                ?.split(",")
                ?.map(String::trim)
                ?.filter(String::isNotEmpty)
                ?.distinct()
                .orEmpty(),
        description = description?.trim().orEmpty(),
        claimSteps = instructions.orEmpty().claimSteps(),
        ending = ending(today),
        addedOn = publishedDate?.gamerPowerDateOrNull(),
        openUrl = openGiveawayUrl.toSafeExternalUrl(),
        sourceUrl = gamerpowerUrl.toSafeExternalUrl(),
    )

private fun GiveawayResponse.ending(today: LocalDate): GiveawayEnding {
    val endDate = endDate?.gamerPowerDateOrNull()
    val daysLeft = endDate?.let(today::daysUntil)
    return when {
        status.equals(EXPIRED_STATUS, ignoreCase = true) || (daysLeft != null && daysLeft < 0) ->
            GiveawayEnding.Ended(endDate)
        endDate != null && daysLeft != null -> GiveawayEnding.EndsOn(endDate, daysLeft)
        else -> GiveawayEnding.OpenEnded
    }
}

/** The steps of these instructions without their numbers, skipping blank lines. */
internal fun String.claimSteps(): List<String> =
    split(InstructionLineBreak)
        .map { line -> line.trim().replaceFirst(StepNumber, "").trim() }
        .filter(String::isNotEmpty)
