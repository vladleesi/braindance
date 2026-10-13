// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
