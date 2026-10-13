// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

class WebPlatform : Platform {
    override val name: String = "Web"
}

actual fun getPlatform(): Platform = WebPlatform()
