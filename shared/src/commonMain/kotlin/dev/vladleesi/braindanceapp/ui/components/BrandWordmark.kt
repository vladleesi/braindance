// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.resources.Res
import dev.vladleesi.braindanceapp.resources.app_name
import dev.vladleesi.braindanceapp.resources.wordmark_ink
import dev.vladleesi.braindanceapp.ui.style.BraindanceTheme
import dev.vladleesi.braindanceapp.ui.style.icons.BraindanceWordmark
import dev.vladleesi.braindanceapp.ui.style.surface
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

// The brand kit's `wordmark-ink.png` (docs/branding/logo/png) is 2400 x 697 px with clear space around the letters.
// These are where its lettering sits in that file, measured in pixels: the left edge, the top of the i's dot, and the
// width.
private const val INK_IMAGE_WIDTH_PX = 2400f
private const val INK_IMAGE_HEIGHT_PX = 697f
private const val INK_LETTERS_LEFT_PX = 176f
private const val INK_LETTERS_TOP_PX = 172f
private const val INK_LETTERS_WIDTH_PX = 2004f

// The Echo wordmark's letters are this wide in its own units, and its whole drawing this tall, trail included.
private const val ECHO_LETTERS_WIDTH = 5624.52f
private const val ECHO_DRAWING_HEIGHT = 934f

// The Echo vector's intrinsic size. Given a taller box it does not grow: it stays this size, centered in the box.
private val EchoIntrinsicWidth = 148.dp
private val EchoIntrinsicHeight = 24.dp

/**
 * The brand wordmark in a box [echoHeight] tall. Light surfaces show the kit's one-color ink wordmark, with its
 * lettering laid exactly over the Echo wordmark's (same size, same place), so changing the theme moves nothing.
 */
@Composable
internal fun BrandWordmark(
    echoHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(Res.string.app_name)
    if (MaterialTheme.colors.isLight) {
        val drawnHeight = minOf(echoHeight, EchoIntrinsicHeight)
        val drawnWidth = EchoIntrinsicWidth * (drawnHeight / EchoIntrinsicHeight)
        val pixel = drawnHeight * (ECHO_LETTERS_WIDTH / ECHO_DRAWING_HEIGHT) / INK_LETTERS_WIDTH_PX
        val lettersTop = (echoHeight - drawnHeight) / 2 - pixel * INK_LETTERS_TOP_PX
        Box(modifier.size(width = drawnWidth, height = echoHeight)) {
            Image(
                painter = painterResource(Res.drawable.wordmark_ink),
                contentDescription = name,
                modifier =
                    Modifier
                        .wrapContentSize(Alignment.TopStart, unbounded = true)
                        .requiredSize(pixel * INK_IMAGE_WIDTH_PX, pixel * INK_IMAGE_HEIGHT_PX)
                        .offset(-pixel * INK_LETTERS_LEFT_PX, lettersTop),
            )
        }
    } else {
        Image(
            imageVector = BraindanceWordmark,
            contentDescription = name,
            modifier = modifier.height(echoHeight),
        )
    }
}

@Preview
@Composable
internal fun BrandWordmarkPreview() {
    BraindanceTheme(darkTheme = false) {
        Column(Modifier.background(surface)) { BrandWordmark(echoHeight = 24.dp) }
    }
}
