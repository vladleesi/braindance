// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.data.models.games

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable
data class Websites(
    val id: Int?,
    val url: String?,
    val type: WebsiteType?,
)

@Serializable(WebsiteTypeSerializer::class)
enum class WebsiteType(
    val id: Int,
) {
    OFFICIAL(1),
    WIKIA(2),
    WIKIPEDIA(3),
    FACEBOOK(4),
    TWITTER(5),
    TWITCH(6),
    INSTAGRAM(8),
    YOUTUBE(9),
    IPHONE(10),
    IPAD(11),
    ANDROID(12),
    STEAM(13),
    REDDIT(14),
    ITCH(15),
    EPIC_GAMES(16),
    GOG(17),
    DISCORD(18),
    BLUESKY(19),
    XBOX(22),
    PLAYSTATION(23),
    UNKNOWN(-1),
    ;

    companion object {
        private val idMap = entries.associateBy(WebsiteType::id)

        fun fromId(id: Int): WebsiteType = idMap[id] ?: UNKNOWN
    }
}

object WebsiteTypeSerializer : KSerializer<WebsiteType> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor(
            serialName = WebsiteType::class.simpleName.orEmpty(),
            kind = PrimitiveKind.INT,
        )

    override fun deserialize(decoder: Decoder): WebsiteType {
        val id = decoder.decodeInt()
        return WebsiteType.fromId(id)
    }

    override fun serialize(
        encoder: Encoder,
        value: WebsiteType,
    ) {
        encoder.encodeInt(value.id)
    }
}
