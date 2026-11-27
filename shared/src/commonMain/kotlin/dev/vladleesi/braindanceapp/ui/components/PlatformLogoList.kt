// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.foreground
import dev.vladleesi.braindanceapp.utils.ParentPlatformType
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.painterResource

/** The platforms' logos in one row, wrapping onto further rows, [horizontalSpacing] apart, when they do not fit. */
@Composable
fun PlatformLogoList(
    platforms: List<ParentPlatformType>,
    modifier: Modifier = Modifier,
    imageSize: Dp,
    horizontalSpacing: Dp,
) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(horizontalSpacing),
        verticalArrangement = Arrangement.spacedBy(horizontalSpacing),
    ) {
        platforms.forEach { platform ->
            if (platform.iconRes == null) return@forEach
            Image(
                modifier = Modifier.size(imageSize),
                painter = painterResource(platform.iconRes),
                contentDescription = platform.name.toContentDescription(),
                colorFilter = ColorFilter.tint(foreground),
            )
        }
    }
}

@Preview
@Composable
internal fun PlatformLogoListPreview() {
    BraindanceTheme {
        PlatformLogoList(
            platforms = ParentPlatformType.entries,
            imageSize = 24.dp,
            horizontalSpacing = Dimens.small,
        )
    }
}
