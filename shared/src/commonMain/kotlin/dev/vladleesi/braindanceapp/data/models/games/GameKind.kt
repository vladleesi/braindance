// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * What a game details entry is, as the API's `kind` names it in snake case, such as `expanded_game`. A value this
 * client does not know yet, from a newer API, is [OTHER] rather than a decoding failure.
 */
@Serializable(GameKindSerializer::class)
enum class GameKind {
    GAME,
    EDITION,
    UPDATE,
    DLC,
    EXPANSION,
    PACK,
    EPISODE,
    SEASON,
    BUNDLE,
    STANDALONE_EXPANSION,
    REMAKE,
    REMASTER,
    EXPANDED_GAME,
    PORT,
    FORK,
    MOD,
    OTHER,
    ;

    /** The API's name for this kind. */
    val apiName: String get() = name.lowercase()

    companion object {
        private val byApiName = entries.associateBy(GameKind::apiName)

        /** The kind the API names [apiName]; a missing or unknown name is [OTHER]. */
        fun fromApi(apiName: String?): GameKind = apiName?.let(byApiName::get) ?: OTHER
    }
}

/** Reads and writes [GameKind] as the API's name, mapping unknown names to [GameKind.OTHER]. */
object GameKindSerializer : KSerializer<GameKind> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(serialName = "GameKind", kind = PrimitiveKind.STRING)

    override fun deserialize(decoder: Decoder): GameKind = GameKind.fromApi(decoder.decodeString())

    override fun serialize(
        encoder: Encoder,
        value: GameKind,
    ) {
        encoder.encodeString(value.apiName)
    }
}
