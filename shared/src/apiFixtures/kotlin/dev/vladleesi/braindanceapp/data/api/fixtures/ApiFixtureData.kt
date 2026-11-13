// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.api.fixtures

import dev.vladleesi.braindanceapp.data.api.backendJson
import dev.vladleesi.braindanceapp.data.models.games.Cover
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.Genre
import dev.vladleesi.braindanceapp.data.models.games.Platform
import dev.vladleesi.braindanceapp.data.models.games.ReleaseDate
import dev.vladleesi.braindanceapp.data.models.games.SimilarGame
import dev.vladleesi.braindanceapp.data.models.games.TrendingGameItem
import dev.vladleesi.braindanceapp.data.models.games.WebsiteType
import dev.vladleesi.braindanceapp.data.models.games.Websites
import dev.vladleesi.braindanceapp.data.models.giveaways.GiveawayResponse
import kotlin.random.Random

/**
 * Synthetic sample responses, encoded once. Every name and text is invented, images come from [FixtureImages], and
 * external links point to the reserved example.com domain, so fixtures hold no third-party data.
 */
internal object ApiFixtureData {
    private val games: List<GameItem> by lazy { gameNames.mapIndexed(::sampleGame) }
    private val gamesById: Map<Int, GameItem> by lazy { games.associateBy { game -> game.id ?: 0 } }
    private val giveaways: List<GiveawayResponse> by lazy { giveawayTitles.mapIndexed(::sampleGiveaway) }

    val trendingJson: String by lazy {
        backendJson.encodeToString(
            games.take(TRENDING_COUNT).mapIndexed { index, game ->
                val reason = trendingReasons[index % trendingReasons.size]
                TrendingGameItem(
                    id = game.id ?: 0,
                    name = game.name,
                    cover = game.cover,
                    reason = reason,
                    platform = if (reason == "new_platform") "Switch 2" else null,
                )
            },
        )
    }

    // The anticipated feed sends compact cards: ID, name, and cover only.
    private val anticipated: List<GameItem> by lazy {
        games.drop(ANTICIPATED_OFFSET).map { game -> compactGame(game.id, game.name, game.cover) }
    }

    val giveawaysJson: String by lazy { backendJson.encodeToString(giveaways) }

    fun anticipatedJson(pageSize: Int): String = backendJson.encodeToString(anticipated.take(pageSize))

    /** Like the API, an unknown ID is an empty list. */
    fun gameDetailsJson(id: Int): String = backendJson.encodeToString(listOfNotNull(gamesById[id]))

    fun recommendationsJson(id: Int): String {
        val similar =
            if (id in gamesById) {
                games
                    .filter { game -> game.id != id }
                    .shuffled(Random(id))
                    .take(RECOMMENDATION_COUNT)
                    .map { game -> SimilarGame(cover = game.cover, id = game.id, name = game.name) }
            } else {
                emptyList()
            }
        return backendJson.encodeToString(similar)
    }

    fun giveawayJsonOrNull(id: Int): String? =
        giveaways.firstOrNull { giveaway -> giveaway.id == id }?.let(backendJson::encodeToString)
}

private const val FIRST_GAME_ID = 101
private const val FIRST_GIVEAWAY_ID = 501
private const val TRENDING_COUNT = 8
private const val ANTICIPATED_OFFSET = 4
private const val RECOMMENDATION_COUNT = 6
private const val NO_COVER_INDEX = 9
private const val FIRST_RELEASE_SECONDS = 1_773_273_600L // Mar 12, 2026
private const val RELEASE_STEP_SECONDS = 41L * 24 * 60 * 60
private const val EXAMPLE_URL = "https://example.com"
private const val SUMMARY_FILLER = "Its text only fills the summary. "

private val gameNames =
    listOf(
        "Sample Game One",
        "Sample Game Two",
        "Sample Game Three",
        "Sample Game Four",
        "Sample Game Five",
        "Sample Game Six",
        "Sample Game Seven",
        "Sample Game Eight",
        "Sample Game Nine",
        "Sample Game Ten Without a Cover",
        "Sample Game Eleven",
        "Sample Game With a Deliberately Long Title to Check How Cards Wrap and Truncate Text",
    )

// Every reason the cards label, plus one unknown reason and none, which show no label.
private val trendingReasons = listOf("new_release", "full_release", "showcased", "new_platform", "anniversary", null)

private val genres =
    listOf(
        Genre(id = 31, name = "Adventure"),
        Genre(id = 12, name = "Role-playing (RPG)"),
        Genre(id = 5, name = "Shooter"),
        Genre(id = 15, name = "Strategy"),
        Genre(id = 32, name = "Indie"),
        Genre(id = 9, name = "Puzzle"),
    )

// Names chosen to reach every platform icon.
private val platforms =
    listOf(
        "PC (Microsoft Windows)",
        "PlayStation 5",
        "Xbox Series X|S",
        "Nintendo Switch",
        "Apple macOS",
        "iOS",
        "Android",
        "Linux",
    ).mapIndexed { index, name -> Platform(id = index + 1, name = name) }

private val storeTypes =
    listOf(
        WebsiteType.STEAM,
        WebsiteType.GOG,
        WebsiteType.EPIC_GAMES,
        WebsiteType.PLAYSTATION,
        WebsiteType.XBOX,
        WebsiteType.NINTENDO,
        WebsiteType.OFFICIAL,
    )

private val giveawayTitles =
    listOf(
        "Sample Giveaway: Game Key",
        "Sample Giveaway: Bonus Pack",
        "Sample Giveaway: Early Access",
        "Sample Giveaway: Mobile Bundle",
        "Sample Giveaway: Console Add-on",
        "Sample Giveaway With a Long Title to Check Wrapping",
    )

private fun sampleGame(
    index: Int,
    name: String,
): GameItem {
    val id = FIRST_GAME_ID + index
    val releaseSeconds = FIRST_RELEASE_SECONDS + index * RELEASE_STEP_SECONDS
    return GameItem(
        cover = if (index == NO_COVER_INDEX) null else sampleCover(id),
        firstReleaseDate = releaseSeconds,
        genres = listOf(genres[index % genres.size], genres[(index + 2) % genres.size]),
        id = id,
        name = name,
        platforms = List(index % platforms.size + 1) { offset -> platforms[(index + offset) % platforms.size] },
        // Some games have no human-readable date, so the app formats first_release_date instead.
        releaseDates = if (index % 3 == 0) null else listOf(ReleaseDate(id = id, human = "Q${index % 4 + 1} 2027")),
        screenshots = null,
        websites =
            List(index % storeTypes.size + 1) { offset ->
                val type = storeTypes[(index + offset) % storeTypes.size]
                Websites(id = id * 10 + offset, url = "$EXAMPLE_URL/stores/${type.name.lowercase()}/$id", type = type)
            },
        summary = "Sample data for developer builds; this game does not exist. " + SUMMARY_FILLER.repeat(index + 1),
        storyline = if (index % 2 == 0) "A sample storyline that exists only to fill this section." else null,
        videos = null,
    )
}

// The API sends protocol-relative IGDB URLs at t_thumb size; the app derives the size it shows.
private fun sampleCover(gameId: Int) =
    Cover(id = gameId, url = "//$API_FIXTURES_HOST/igdb/image/upload/t_thumb/cover-$gameId.jpg")

private fun compactGame(
    id: Int?,
    name: String?,
    cover: Cover?,
) = GameItem(
    cover = cover,
    firstReleaseDate = null,
    genres = null,
    id = id,
    name = name,
    platforms = null,
    releaseDates = null,
    screenshots = null,
    websites = null,
    summary = null,
    storyline = null,
    videos = null,
)

private val giveawayTypes = listOf("Game", "DLC", "Early Access")
private val giveawayPlatforms =
    listOf("PC, Steam", "PC, Epic Games Store", "PS5", "Android, iOS", "Xbox Series X|S", "PC, GOG")

private fun sampleGiveaway(
    index: Int,
    title: String,
): GiveawayResponse {
    val id = FIRST_GIVEAWAY_ID + index
    val image = "$API_FIXTURES_BASE_URL/images/giveaway-$id.png"
    val link = "$EXAMPLE_URL/giveaways/$id"
    return GiveawayResponse(
        id = id,
        title = title,
        // GamerPower sends "N/A" for an unknown worth and end date; the app hides both.
        worth = if (index % 3 == 2) "N/A" else "\$${index * 5 + 4}.99",
        thumbnail = image,
        image = image,
        description = "Sample giveaway for developer builds. Nothing here can be claimed.",
        instructions = "1. This is sample data.\n2. The claim link opens example.com.",
        openGiveawayUrl = link,
        publishedDate = "2026-11-01 10:00:00",
        type = giveawayTypes[index % giveawayTypes.size],
        platforms = giveawayPlatforms[index % giveawayPlatforms.size],
        endDate = if (index % 3 == 2) "N/A" else "2099-12-31 23:59:00",
        users = (index + 1) * 1_250,
        status = "Active",
        gamerpowerUrl = link,
        openGiveaway = link,
    )
}
