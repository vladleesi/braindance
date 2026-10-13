// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_screen_where_to_buy
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_playstation
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_switch
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_windows
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_xbox
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryVariant
import dev.vladleesi.braindanceapp.ui.style.white
import dev.vladleesi.braindanceapp.utils.StoreTypeModel
import dev.vladleesi.braindanceapp.utils.toContentDescription
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

fun LazyListScope.storesBlockItem(stores: List<StoreTypeModel>) {
    item {
        Text(
            text = stringResource(Res.string.game_details_screen_where_to_buy),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(start = Dimens.medium, end = Dimens.medium),
        )
    }
    item {
        StoreButtonsRow(stores)
    }
}

@Composable
fun StoreButtonsRow(storeList: List<StoreTypeModel>) {
    Spacer(modifier = Modifier.size(Dimens.small))
    FlowRow(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.medium),
        horizontalArrangement = Arrangement.spacedBy(Dimens.small),
        verticalArrangement = Arrangement.spacedBy(Dimens.small),
    ) {
        storeList.forEach { store ->
            StoreButton(
                store = store.name,
                image = store.image,
                url = store.url,
            )
        }
    }
}

@Composable
fun StoreButton(
    store: String,
    image: DrawableResource?,
    url: String,
    modifier: Modifier = Modifier,
    uriHandler: UriHandler = LocalUriHandler.current,
) {
    Button(
        modifier = modifier.height(48.dp),
        colors = ButtonDefaults.buttonColors(backgroundColor = secondaryVariant),
        onClick = { uriHandler.openUri(url) },
    ) {
        Row(Modifier.align(Alignment.CenterVertically)) {
            Text(text = store, style = MaterialTheme.typography.subtitle1)
            if (image != null) {
                Spacer(Modifier.width(Dimens.small))
                Image(
                    modifier = Modifier.size(22.dp),
                    colorFilter = ColorFilter.tint(white),
                    imageVector = vectorResource(image),
                    contentDescription = store.toContentDescription(),
                )
            }
        }
    }
}

@Preview
@Composable
internal fun StoreButtonPreview() {
    BraindanceTheme {
        StoreButton(
            store = "PlayStation Store",
            image = Res.drawable.ic_platform_logo_playstation,
            url = "",
        )
    }
}

@Preview
@Composable
internal fun StoreButtonsPreview() {
    BraindanceTheme {
        LazyColumn {
            storesBlockItem(
                listOf(
                    StoreTypeModel(
                        name = "PlayStation Store",
                        image = Res.drawable.ic_platform_logo_playstation,
                        url = "",
                    ),
                    StoreTypeModel(
                        name = "Windows Store",
                        image = Res.drawable.ic_platform_logo_windows,
                        url = "",
                    ),
                    StoreTypeModel(
                        name = "Xbox Store",
                        image = Res.drawable.ic_platform_logo_xbox,
                        url = "",
                    ),
                    StoreTypeModel(
                        name = "Nintendo Store",
                        image = Res.drawable.ic_platform_logo_switch,
                        url = "",
                    ),
                ),
            )
        }
    }
}
