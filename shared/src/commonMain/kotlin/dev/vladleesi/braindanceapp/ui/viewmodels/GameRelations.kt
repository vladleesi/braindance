// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.viewmodels

import dev.vladleesi.braindanceapp.data.models.games.FamilyGame
import dev.vladleesi.braindanceapp.data.models.games.GameItem
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.utils.CoverSize
import dev.vladleesi.braindanceapp.utils.toCoverUrl
import dev.vladleesi.braindanceapp.utils.utcYearOrNull

/** The edition or update a game's page was opened for; the page shows its game with this version selected. */
data class SelectedVariant(
    val id: Int,
    /** The full name, as a shared link names it. */
    val name: String,
    val kind: GameKind,
)

/**
 * How a game's page places its entry among the game's releases, in three tiers: the game it belongs to ([origin]),
 * the versions of the same game to switch between ([versions]), and separate releases that relate to it ([related]).
 */
data class GameRelations(
    val origin: GameOrigin? = null,
    val versions: VersionSelector? = null,
    /** Related releases by category, in display order and without empty categories. */
    val related: List<RelatedGroup> = emptyList(),
)

/** How an entry relates to the game it belongs to, which its page links to: "Expansion for", "Remaster of". */
enum class OriginRelation {
    /** Content that needs the game, such as an expansion. */
    FOR,

    /** A separate release of the game, such as a remaster or a port. */
    OF,

    /** Any other link IGDB records. */
    PART_OF,
}

data class GameOrigin(
    val kind: GameKind,
    val relation: OriginRelation,
    val gameId: Int,
    val gameName: String,
)

/** One version of the game; the base game has no [label], and its chip is named as such. */
data class VersionOption(
    val id: Int,
    val label: String?,
    val kind: GameKind,
)

/** The game's versions, of which [selectedId] is the one the page shows. Only built for two versions or more. */
data class VersionSelector(
    val options: List<VersionOption>,
    val selectedId: Int,
) {
    val selected: VersionOption? get() = options.firstOrNull { it.id == selectedId }
}

enum class RelatedCategory {
    EXTRAS,
    OTHER_VERSIONS,
    INCLUDED_IN,
    CONTENTS,
}

data class RelatedGroup(
    val category: RelatedCategory,
    val games: List<RelatedGame>,
)

/** A related release as a list row; [coverUrl] is at the row's small thumbnail size. */
data class RelatedGame(
    val id: Int,
    val title: String,
    val kind: GameKind,
    val year: Int?,
    val coverUrl: String,
)

private val extraKinds = setOf(GameKind.DLC, GameKind.EXPANSION, GameKind.PACK, GameKind.EPISODE, GameKind.SEASON)
private val releaseKinds =
    setOf(
        GameKind.STANDALONE_EXPANSION,
        GameKind.REMAKE,
        GameKind.REMASTER,
        GameKind.EXPANDED_GAME,
        GameKind.PORT,
        GameKind.FORK,
    )

/**
 * The relations of [page], the game this details response shows, with [selected] its version on a variant's page.
 * Built only from the response's family, which the API derives from IGDB's typed links; names never decide a
 * relation. Without a family, as from an API that predates families, the page has no relations.
 */
internal fun GameItem.gameRelations(
    page: GameItem,
    selected: SelectedVariant?,
): GameRelations {
    val family = family ?: return GameRelations()
    val pageId = page.id ?: return GameRelations()
    val pageKind = page.kind ?: GameKind.OTHER
    val origin =
        family.parent?.let { parent ->
            val parentId = parent.id
            val parentName = parent.name?.takeIf(String::isNotBlank)
            if (parentId == null || parentName == null || parentId == pageId) return@let null
            val relation =
                when (pageKind) {
                    in extraKinds -> OriginRelation.FOR
                    in releaseKinds -> OriginRelation.OF
                    else -> OriginRelation.PART_OF
                }
            GameOrigin(kind = pageKind, relation = relation, gameId = parentId, gameName = parentName)
        }
    return GameRelations(
        origin = origin,
        versions = family.variants.orEmpty().versionSelector(pageId, selected),
        related =
            listOf(
                RelatedCategory.EXTRAS to family.extras,
                RelatedCategory.OTHER_VERSIONS to family.otherVersions,
                RelatedCategory.INCLUDED_IN to family.includedIn,
                RelatedCategory.CONTENTS to family.contents,
            ).mapNotNull { (category, games) ->
                val rows = games.orEmpty().mapNotNull { it.toRelatedGame(pageId) }.distinctBy { it.id }
                rows.takeIf { it.isNotEmpty() }?.let { RelatedGroup(category, it) }
            },
    )
}

/**
 * The base game first, then its editions and updates as the API orders them. A selected version the list lacks, such
 * as an edition of an edition, is added so the selection is always visible.
 */
private fun List<FamilyGame>.versionSelector(
    pageId: Int,
    selected: SelectedVariant?,
): VersionSelector? {
    val variants =
        mapNotNull { variant ->
            val id = variant.id?.takeIf { it != pageId } ?: return@mapNotNull null
            val name = variant.name?.takeIf(String::isNotBlank) ?: return@mapNotNull null
            VersionOption(id = id, label = variant.label?.takeIf(String::isNotBlank) ?: name, kind = variant.kind())
        }.distinctBy { it.id }
    val unlisted =
        selected
            ?.takeIf { selection -> variants.none { it.id == selection.id } }
            ?.let { VersionOption(id = it.id, label = it.name, kind = it.kind) }
    val options =
        listOf(VersionOption(id = pageId, label = null, kind = GameKind.GAME)) + variants + listOfNotNull(unlisted)
    if (options.size < 2) return null
    return VersionSelector(
        options = options,
        selectedId = selected?.id ?: pageId,
    )
}

private fun FamilyGame.toRelatedGame(pageId: Int): RelatedGame? {
    val id = id?.takeIf { it != pageId } ?: return null
    val name = name?.takeIf(String::isNotBlank) ?: return null
    return RelatedGame(
        id = id,
        title = label?.takeIf(String::isNotBlank) ?: name,
        kind = kind(),
        year = firstReleaseDate?.utcYearOrNull(),
        coverUrl = cover?.url?.toCoverUrl(CoverSize.COVER_SMALL).orEmpty(),
    )
}

private fun FamilyGame.kind() = kind ?: GameKind.OTHER
