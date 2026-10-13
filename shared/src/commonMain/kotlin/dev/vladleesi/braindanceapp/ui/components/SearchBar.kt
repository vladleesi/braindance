// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.secondary
import dev.vladleesi.braindanceapp.ui.style.white

@Composable
fun SearchBar(
    modifier: Modifier = Modifier,
    placeholder: String = "Search for games",
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    TextField(
        value = query,
        onValueChange = { value ->
            query = value
            onQueryChange(value)
        },
        singleLine = true,
        textStyle = MaterialTheme.typography.body1,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.body2,
            )
        },
        shape = CircleShape,
        colors =
            TextFieldDefaults.textFieldColors(
                backgroundColor = secondary,
                cursorColor = white,
                leadingIconColor = white,
                textColor = white,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        keyboardOptions =
            KeyboardOptions(
                imeAction = ImeAction.Search,
            ),
        keyboardActions =
            KeyboardActions(
                onSearch = { onSearch(query) },
            ),
        modifier = modifier.fillMaxWidth(),
    )
}

@Preview
@Composable
internal fun SearchBarPreview() {
    BraindanceTheme {
        SearchBar(onQueryChange = {}, onSearch = {})
    }
}
