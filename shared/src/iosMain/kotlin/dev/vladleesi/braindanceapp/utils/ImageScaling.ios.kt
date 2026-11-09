// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.utils

// Coil scales decoded images with Skia's default nearest-neighbour sampling, which makes edges jagged, so keep the
// download's size and let Compose filter the image when it draws it.
internal actual val scalesDecodedImagesSmoothly: Boolean = false
