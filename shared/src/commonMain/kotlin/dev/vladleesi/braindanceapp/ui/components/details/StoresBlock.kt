// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.details

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.game_details_screen_where_to_buy
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_playstation
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_windows
import dev.vladleesi.braindanceapp.resources.ic_platform_logo_xbox
import dev.vladleesi.braindanceapp.ui.components.LinkDivider
import dev.vladleesi.braindanceapp.ui.components.LinkRow
import dev.vladleesi.braindanceapp.ui.components.LinkRowTextInset
import dev.vladleesi.braindanceapp.ui.components.SurfaceCard
import dev.vladleesi.braindanceapp.ui.components.pageColumn
import dev.vladleesi.braindanceapp.ui.components.pageGutter
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.CornerShapes
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import dev.vladleesi.braindanceapp.utils.StoreTypeModel
import dev.vladleesi.braindanceapp.utils.openExternalUrl
import dev.vladleesi.braindanceapp.utils.placeholder
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.vectorResource

/** "Where to buy" and [stores] in the page column, a readable width wide, as one item of a lazy list. */
fun LazyListScope.storesBlockItem(
    stores: List<StoreTypeModel>,
    placeholder: Boolean = false,
) {
    item {
        StoresBlock(
            stores = stores,
            placeholder = placeholder,
            modifier =
                Modifier
                    .pageColumn(pageGutter())
                    .widthIn(max = Dimens.readableTextWidth)
                    .fillMaxWidth(),
        )
    }
}

/**
 * "Where to buy": the game's [stores] as one list on a card, a row per store with its logo, its name, and a mark that
 * it opens outside the app, in the order the API sends them. The list takes its container's width, so it also fits a
 * column beside other content, such as the game's media in the desktop layout. While [placeholder], another version's
 * stores are loading: the card shimmers and its rows do nothing.
 */
@Composable
fun StoresBlock(
    stores: List<StoreTypeModel>,
    modifier: Modifier = Modifier,
    placeholder: Boolean = false,
) {
    val uriHandler = LocalUriHandler.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = stringResource(Res.string.game_details_screen_where_to_buy),
            style = MaterialTheme.typography.h3,
            modifier = Modifier.semantics { heading() },
        )
        SurfaceCard(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .placeholder(visible = placeholder, shape = CornerShapes.large),
        ) {
            Column {
                stores.forEachIndexed { index, store ->
                    if (index > 0) LinkDivider(inset = LinkRowTextInset)
                    LinkRow(
                        title = store.name,
                        imageVector = store.image?.let { vectorResource(it) },
                        onClick = { uriHandler.openExternalUrl(store.url) },
                        enabled = !placeholder,
                    )
                }
            }
        }
    }
}

@Preview(widthDp = 360)
@Composable
internal fun StoresBlockPreview() {
    BraindanceTheme {
        StoresBlock(
            stores =
                listOf(
                    StoreTypeModel(name = "Steam", image = Res.drawable.ic_platform_logo_windows, url = ""),
                    StoreTypeModel(
                        name = "PlayStation Store",
                        image = Res.drawable.ic_platform_logo_playstation,
                        url = "",
                    ),
                    StoreTypeModel(name = "Xbox Store", image = Res.drawable.ic_platform_logo_xbox, url = ""),
                    StoreTypeModel(name = "Official site", image = null, url = ""),
                ),
            modifier =
                Modifier
                    .background(background)
                    .padding(Dimens.medium),
        )
    }
}
