// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components.profile

import Github
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.components.LinkDivider
import dev.vladleesi.braindanceapp.ui.components.LinkRow
import dev.vladleesi.braindanceapp.ui.components.LinkRowTextInset
import dev.vladleesi.braindanceapp.ui.components.SurfaceCard
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.Dimens
import dev.vladleesi.braindanceapp.ui.style.secondaryText

/**
 * A titled group of related rows on one rounded surface, the settings-list pattern: a short, specific [title] above
 * the group names what the rows have in common. Its rows are [LinkRow]s, separated by [LinkDivider].
 */
@Composable
fun ProfileSection(
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Dimens.small)) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle2,
            color = secondaryText,
            modifier = Modifier.padding(horizontal = Dimens.medium).semantics { heading() },
        )
        SurfaceCard(modifier = Modifier.fillMaxWidth()) {
            Column(content = content)
        }
    }
}

@Preview
@Composable
internal fun ProfileSectionPreview() {
    BraindanceTheme {
        ProfileSection(title = "Community", modifier = Modifier.padding(Dimens.medium)) {
            LinkRow(
                title = "Star on GitHub",
                supportingText = "Braindance is open source",
                imageVector = Github,
                onClick = {},
            )
            LinkDivider(inset = LinkRowTextInset)
            LinkRow(title = "Share", imageVector = Github, onClick = {}, opensExternally = false)
        }
    }
}
