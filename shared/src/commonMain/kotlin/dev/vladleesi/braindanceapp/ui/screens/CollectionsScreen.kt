// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.style.Dimens

@Composable
fun CollectionsScreen(modifier: Modifier) {
    Column(modifier = modifier) {
        SpacerTopBarWithStatusBarInsets()
        Text(
            modifier = Modifier.padding(horizontal = Dimens.medium),
            text = "Collections Screen",
            style = MaterialTheme.typography.h2,
        )
    }
}
