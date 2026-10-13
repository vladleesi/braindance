// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import dev.vladleesi.braindanceapp.data.models.games.WebsiteType
import dev.vladleesi.braindanceapp.data.models.games.Websites
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_android
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_ios
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_playstation
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_xbox
import dev.vladleesi.braindanceapp.resources.ic_store_logo_epic_games
import dev.vladleesi.braindanceapp.resources.ic_store_logo_gog
import dev.vladleesi.braindanceapp.resources.ic_store_logo_steam
import org.jetbrains.compose.resources.DrawableResource

data class StoreTypeModel(
    val name: String,
    val image: DrawableResource?,
    val url: String,
)

fun List<Websites>.toStoreTypes(): List<StoreTypeModel> =
    this
        .validWebsites()
        .mapNotNull { it.storeType() }

private fun Websites.storeType(): StoreTypeModel? {
    val url = url.orEmpty()
    val storeType =
        StoreType.entries.firstOrNull { type ->
            type.websiteType == this.type
        }
    return storeType?.toStoreModel(url)
}

private enum class StoreType(
    val websiteType: WebsiteType,
    val storeName: String,
    val image: DrawableResource?,
) {
    PlayStationStore(
        websiteType = WebsiteType.PLAYSTATION,
        storeName = "PlayStation Store",
        image = Res.drawable.ic_platform_logo_playstation,
    ),
    XboxStore(
        websiteType = WebsiteType.XBOX,
        storeName = "Xbox Store",
        image = Res.drawable.ic_platform_logo_xbox,
    ),
    AppStore(
        websiteType = WebsiteType.IPHONE,
        storeName = "App Store",
        image = Res.drawable.ic_platform_logo_ios,
    ),
    GooglePlay(
        websiteType = WebsiteType.ANDROID,
        storeName = "Google Play",
        image = Res.drawable.ic_platform_logo_android,
    ),
    Steam(
        websiteType = WebsiteType.STEAM,
        storeName = "Steam",
        image = Res.drawable.ic_store_logo_steam,
    ),
    GOG(
        websiteType = WebsiteType.GOG,
        storeName = "GOG",
        image = Res.drawable.ic_store_logo_gog,
    ),
    EpicGamesStore(
        websiteType = WebsiteType.EPIC_GAMES,
        storeName = "Epic Games",
        image = Res.drawable.ic_store_logo_epic_games,
    ),
}

private fun List<Websites>.validWebsites(): List<Websites> =
    this.filter { !it.url.isNullOrEmpty() && deprecatedStoreUrls.none { url -> it.url.contains(url) } }

private fun StoreType.toStoreModel(url: String): StoreTypeModel =
    StoreTypeModel(
        name = this.storeName,
        image = this.image,
        url = url,
    )

private val deprecatedStoreUrls =
    hashSetOf(
        "marketplace.xbox.com",
    )
