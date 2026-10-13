// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}

actual fun getPlatform(): Platform = AndroidPlatform()
