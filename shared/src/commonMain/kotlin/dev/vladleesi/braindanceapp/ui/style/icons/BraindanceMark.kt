// SPDX-License-Identifier: MPL-2.0
// The outline in MARK_PATH is Braindance brand artwork (docs/branding), which the MPL grant does not cover; see
// THIRD_PARTY_NOTICES.md. Only the code around it is MPL-2.0.

package dev.vladleesi.braindanceapp.ui.style.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathNode
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.graphics.vector.group
import androidx.compose.ui.unit.dp
import dev.vladleesi.braindanceapp.ui.style.ghost
import dev.vladleesi.braindanceapp.ui.style.hotMagenta
import dev.vladleesi.braindanceapp.ui.style.neonCyan
import dev.vladleesi.braindanceapp.ui.style.signalYellow
import dev.vladleesi.braindanceapp.ui.style.void

// The "bd" letters from docs/branding/logo/svg/mark-small-on-ink.svg, in the font's 1000-unit em.
private const val MARK_PATH =
    "M405 -566Q474 -566 530.0 -531.0Q586 -496 618.5 -431.0Q651 -366 651 -280Q651 -194 618.5 -128.5Q586 " +
        "-63 530.0 -27.5Q474 8 405 8Q346 8 302.0 -15.5Q258 -39 233 -78V0H62V-740H233V-479Q257 -518 302.0 " +
        "-542.0Q347 -566 405 -566ZM354 -417Q303 -417 267.5 -380.0Q232 -343 232 -279Q232 -215 267.5 -178.0Q303 " +
        "-141 354 -141Q405 -141 441.0 -178.5Q477 -216 477 -280Q477 -344 441.5 -380.5Q406 -417 354 " +
        "-417ZM915.9629629629629 -566Q970.9629629629629 -566 1016.462962962963 -543.0Q1061.962962962963 -520 " +
        "1087.962962962963 -481V-740H1258.962962962963V0H1087.962962962963V-80Q1063.962962962963 -40 " +
        "1019.462962962963 -16.0Q974.9629629629629 8 915.9629629629629 8Q846.9629629629629 8 " +
        "790.9629629629629 -27.5Q734.9629629629629 -63 702.4629629629629 -128.5Q669.9629629629629 -194 " +
        "669.9629629629629 -280Q669.9629629629629 -366 702.4629629629629 -431.0Q734.9629629629629 -496 " +
        "790.9629629629629 -531.0Q846.9629629629629 -566 915.9629629629629 -566ZM965.9629629629629 " +
        "-417Q914.9629629629629 -417 879.4629629629629 -380.5Q843.9629629629629 -344 843.9629629629629 " +
        "-280Q843.9629629629629 -216 879.4629629629629 -178.5Q914.9629629629629 -141 965.9629629629629 " +
        "-141Q1016.9629629629629 -141 1052.462962962963 -178.0Q1087.962962962963 -215 1087.962962962963 " +
        "-279Q1087.962962962963 -343 1052.462962962963 -380.0Q1016.9629629629629 -417 965.9629629629629 -417Z"

// The kit's 1024-unit Ink tile with 205-unit corners, and the transform that places the letters on it.
private const val TILE_SIZE = 1024f
private const val TILE_PATH =
    "M205 0H819A205 205 0 0 1 1024 205V819A205 205 0 0 1 819 1024H205A205 205 0 0 1 0 819V205A205 205 0 0 1 205 0Z"
private const val LETTERS_X = 102.40f
private const val LETTERS_Y = 234.04f
private const val LETTERS_SCALE = 0.58641f
private const val LETTERS_ORIGIN_X = -62f
private const val LETTERS_ORIGIN_Y = 740f

// The small mark's deeper trail: each copy moves 1/15 em down and right, so the trail survives small sizes.
private const val TRAIL_STEP = 200f / 3

/**
 * The Braindance "bd" mark for small sizes, as the brand kit's `mark-small-on-ink.svg` draws it: the white letters
 * over a deeper cyan, magenta, and yellow trail, on an Ink tile. The kit asks for this version at 48 px and under, so
 * the trail stays visible. Its colors are part of the artwork, so show it untinted, at 24 dp wide or more.
 */
val BraindanceMark: ImageVector
    get() {
        braindanceMark?.let { return it }
        val letters = addPathNodes(MARK_PATH)
        val imageVector =
            ImageVector
                .Builder(
                    name = "BraindanceMark",
                    defaultWidth = 40.dp,
                    defaultHeight = 40.dp,
                    viewportWidth = TILE_SIZE,
                    viewportHeight = TILE_SIZE,
                ).apply {
                    addPath(pathData = addPathNodes(TILE_PATH), fill = SolidColor(void))
                    group(
                        translationX = LETTERS_X,
                        translationY = LETTERS_Y,
                        scaleX = LETTERS_SCALE,
                        scaleY = LETTERS_SCALE,
                    ) {
                        group(translationX = LETTERS_ORIGIN_X, translationY = LETTERS_ORIGIN_Y) {
                            copy(letters, signalYellow, TRAIL_STEP * 3)
                            copy(letters, hotMagenta, TRAIL_STEP * 2)
                            copy(letters, neonCyan, TRAIL_STEP)
                            copy(letters, ghost, 0f)
                        }
                    }
                }.build()
        braindanceMark = imageVector
        return imageVector
    }

private var braindanceMark: ImageVector? = null

private fun ImageVector.Builder.copy(
    letters: List<PathNode>,
    color: Color,
    offset: Float,
) {
    group(translationX = offset, translationY = offset) {
        addPath(pathData = letters, fill = SolidColor(color))
    }
}
