// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.profile_appearance
import dev.vladleesi.braindanceapp.system.ThemeMode
import dev.vladleesi.braindanceapp.ui.components.ThemeSelector
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.background
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ProfileAppearance(
    mode: ThemeMode,
    onChooseTheme: (ThemeMode) -> Unit,
) {
    ProfileSection(title = stringResource(Res.string.profile_appearance)) {
        ThemeSelector(
            selectedMode = mode,
            onSelect = onChooseTheme,
            modifier = Modifier.fillMaxWidth().padding(Dimens.medium),
            showLabels = true,
        )
    }
}

@Preview
@Composable
internal fun ProfileAppearancePreview() {
    BraindanceTheme(darkTheme = false) {
        Row(Modifier.background(background).padding(Dimens.medium)) {
            ProfileAppearance(mode = ThemeMode.Light, onChooseTheme = {})
        }
    }
}
