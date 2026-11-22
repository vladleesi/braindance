// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.domain.games

import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.hours

internal sealed interface OverviewHighlight {
    enum class PlayStyle : OverviewHighlight {
        SOLO,
        MULTIPLAYER,
        SOLO_AND_MULTIPLAYER,
        MMO,
        CO_OP,
        SPLIT_SCREEN,
        CO_OP_AND_SPLIT_SCREEN,
    }

    data class MainStory(
        val hours: Long,
    ) : OverviewHighlight

    data class Theme(
        val name: String,
    ) : OverviewHighlight

    data class Perspective(
        val name: String,
    ) : OverviewHighlight
}

private const val HIGHLIGHT_LIMIT = 3

// Prefer specific ways a game plays before broad setting or mood.
private val distinctiveThemes =
    listOf(
        "survival",
        "stealth",
        "sandbox",
        "horror",
        "mystery",
        "historical",
        "science fiction",
        "fantasy",
        "open world",
    )

internal fun overviewHighlights(
    modes: List<String>,
    themes: List<String>,
    perspectives: List<String>,
    genres: List<String>,
    mainStorySeconds: Long?,
): List<OverviewHighlight> {
    val knownModes = modes.map(String::metadataKey).toSet()
    val genreKeys = genres.map(String::metadataKey).toSet()
    val themeKeys = themes.map(String::metadataKey).toSet()
    val perspectiveNames =
        perspectives
            .map(String::trim)
            .filter(String::isNotEmpty)
            .sorted()
            .distinctBy(String::metadataKey)
    return listOfNotNull(
        playerMode(knownModes),
        multiplayerCapability(knownModes),
        roundedPlayHours(mainStorySeconds)?.let { OverviewHighlight.MainStory(it) },
        distinctiveThemes.firstOrNull { it in themeKeys && it !in genreKeys }?.let { selected ->
            themes
                .asSequence()
                .map(String::trim)
                .filter { it.metadataKey() == selected }
                .minOrNull()
                ?.let { OverviewHighlight.Theme(it) }
        },
        perspectiveNames
            .singleOrNull()
            ?.takeUnless { it.metadataKey() in genreKeys }
            ?.let { OverviewHighlight.Perspective(it) },
    ).take(HIGHLIGHT_LIMIT)
}

private fun playerMode(modes: Set<String>): OverviewHighlight.PlayStyle? =
    when {
        "single player" in modes && "multiplayer" in modes -> OverviewHighlight.PlayStyle.SOLO_AND_MULTIPLAYER
        "single player" in modes -> OverviewHighlight.PlayStyle.SOLO
        "multiplayer" in modes -> OverviewHighlight.PlayStyle.MULTIPLAYER
        "massively multiplayer online (mmo)" in modes || "mmo" in modes -> OverviewHighlight.PlayStyle.MMO
        else -> null
    }

private fun multiplayerCapability(modes: Set<String>): OverviewHighlight.PlayStyle? {
    val coOp = "co operative" in modes || "co op" in modes || "cooperative" in modes
    val splitScreen = "split screen" in modes || "splitscreen" in modes
    return when {
        coOp && splitScreen -> OverviewHighlight.PlayStyle.CO_OP_AND_SPLIT_SCREEN
        coOp -> OverviewHighlight.PlayStyle.CO_OP
        splitScreen -> OverviewHighlight.PlayStyle.SPLIT_SCREEN
        else -> null
    }
}

internal fun roundedPlayHours(seconds: Long?): Long? =
    seconds?.takeIf { it > 0 }?.let { (it.toDouble() / 1.hours.inWholeSeconds).roundToLong().coerceAtLeast(1) }

private val metadataSeparators = Regex("[\\s_-]+")

private fun String.metadataKey(): String = trim().lowercase().replace(metadataSeparators, " ")
