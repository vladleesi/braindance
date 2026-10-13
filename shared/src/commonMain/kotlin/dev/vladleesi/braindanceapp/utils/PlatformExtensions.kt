// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

import dev.vladleesi.braindanceapp.data.models.games.Platform
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_android
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_ios
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_linux
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_macos
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_playstation
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_switch
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_windows
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_xbox
import org.jetbrains.compose.resources.DrawableResource

fun List<Platform>.parentPlatformTypes(): List<ParentPlatformType> = this.map { it.parentPlatformType() }.distinct()

fun Platform.parentPlatformType(): ParentPlatformType {
    val platformName = this.name ?: return ParentPlatformType.Other
    return when {
        platformName.contains("PlayStation", ignoreCase = true) -> ParentPlatformType.PlayStation
        platformName.contains("Xbox", ignoreCase = true) -> ParentPlatformType.Xbox
        platformName.contains("PC", ignoreCase = true) -> ParentPlatformType.PC
        platformName.contains("Apple", ignoreCase = true) -> ParentPlatformType.MacOS
        platformName.contains("Nintendo", ignoreCase = true) -> ParentPlatformType.Switch
        platformName.contains("iOS", ignoreCase = true) -> ParentPlatformType.IOS
        platformName.contains("Android", ignoreCase = true) -> ParentPlatformType.Android
        platformName.contains("Linux", ignoreCase = true) -> ParentPlatformType.Linux
        else -> ParentPlatformType.Other
    }
}

fun String.giveawayPlatforms(): List<ParentPlatformType> = split(", ").map { Platform(null, it) }.parentPlatformTypes()

enum class ParentPlatformType(
    val iconRes: DrawableResource?,
) {
    PlayStation(iconRes = Res.drawable.ic_platform_logo_playstation),
    Xbox(iconRes = Res.drawable.ic_platform_logo_xbox),
    PC(iconRes = Res.drawable.ic_platform_logo_windows),
    MacOS(iconRes = Res.drawable.ic_platform_logo_macos),
    Switch(iconRes = Res.drawable.ic_platform_logo_switch),
    IOS(iconRes = Res.drawable.ic_platform_logo_ios),
    Android(iconRes = Res.drawable.ic_platform_logo_android),
    Linux(iconRes = Res.drawable.ic_platform_logo_linux),
    Other(iconRes = null),
}
