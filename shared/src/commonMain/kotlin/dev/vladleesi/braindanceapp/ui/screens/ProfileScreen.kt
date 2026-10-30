// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import dev.vladleesi.braindanceapp.BuildKonfig
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_screen_version
import dev.vladleesi.braindanceapp.ui.components.SpacerTopBarWithStatusBarInsets
import dev.vladleesi.braindanceapp.ui.style.Dimens
import org.jetbrains.compose.resources.stringResource

@Composable
fun ProfileScreen(modifier: Modifier) {
    Column(modifier = modifier.fillMaxSize()) {
        SpacerTopBarWithStatusBarInsets()
        Text(
            modifier = Modifier.padding(horizontal = Dimens.medium),
            text = "Profile Screen",
            style = MaterialTheme.typography.h2,
        )
        Spacer(modifier = Modifier.weight(1f))
        Text(
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(Dimens.medium),
            text = stringResource(Res.string.profile_screen_version, BuildKonfig.APP_VERSION),
            style = MaterialTheme.typography.caption,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
        )
    }
}
