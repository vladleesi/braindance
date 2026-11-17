// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

/**
 * Whether the device has a touch screen: always on Android and iOS, and on the Web when the browser reports touch
 * points, as phones, tablets, foldables, and touch laptops do. A browser's device emulation reports them too.
 */
internal expect fun hasTouchScreen(): Boolean
