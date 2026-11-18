// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.runtime.Composable
import dev.vladleesi.braindanceapp.data.models.games.GameKind
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_kind_bundle
import dev.vladleesi.braindanceapp.resources.game_kind_dlc
import dev.vladleesi.braindanceapp.resources.game_kind_edition
import dev.vladleesi.braindanceapp.resources.game_kind_episode
import dev.vladleesi.braindanceapp.resources.game_kind_expanded_game
import dev.vladleesi.braindanceapp.resources.game_kind_expansion
import dev.vladleesi.braindanceapp.resources.game_kind_fork
import dev.vladleesi.braindanceapp.resources.game_kind_game
import dev.vladleesi.braindanceapp.resources.game_kind_mod
import dev.vladleesi.braindanceapp.resources.game_kind_other
import dev.vladleesi.braindanceapp.resources.game_kind_pack
import dev.vladleesi.braindanceapp.resources.game_kind_port
import dev.vladleesi.braindanceapp.resources.game_kind_remake
import dev.vladleesi.braindanceapp.resources.game_kind_remaster
import dev.vladleesi.braindanceapp.resources.game_kind_season
import dev.vladleesi.braindanceapp.resources.game_kind_standalone_expansion
import dev.vladleesi.braindanceapp.resources.game_kind_update
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

/** What the entry is, such as "Expansion" or "Update", in words. */
@Composable
internal fun GameKind.text(): String = stringResource(resource)

private val GameKind.resource: StringResource
    get() =
        when (this) {
            GameKind.GAME -> Res.string.game_kind_game
            GameKind.EDITION -> Res.string.game_kind_edition
            GameKind.UPDATE -> Res.string.game_kind_update
            GameKind.DLC -> Res.string.game_kind_dlc
            GameKind.EXPANSION -> Res.string.game_kind_expansion
            GameKind.PACK -> Res.string.game_kind_pack
            GameKind.EPISODE -> Res.string.game_kind_episode
            GameKind.SEASON -> Res.string.game_kind_season
            GameKind.BUNDLE -> Res.string.game_kind_bundle
            GameKind.STANDALONE_EXPANSION -> Res.string.game_kind_standalone_expansion
            GameKind.REMAKE -> Res.string.game_kind_remake
            GameKind.REMASTER -> Res.string.game_kind_remaster
            GameKind.EXPANDED_GAME -> Res.string.game_kind_expanded_game
            GameKind.PORT -> Res.string.game_kind_port
            GameKind.FORK -> Res.string.game_kind_fork
            GameKind.MOD -> Res.string.game_kind_mod
            GameKind.OTHER -> Res.string.game_kind_other
        }
