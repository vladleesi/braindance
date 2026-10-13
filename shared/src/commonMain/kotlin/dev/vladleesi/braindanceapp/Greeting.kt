// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
