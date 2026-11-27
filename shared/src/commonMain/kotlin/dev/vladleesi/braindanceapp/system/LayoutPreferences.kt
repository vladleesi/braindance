// SPDX-License-Identifier: MPL-2.0

package dev.vladleesi.braindanceapp.system

/**
 * Layout choices that outlast a reload in the Web desktop layout, such as a collapsed sidebar. They are remembered
 * on the device only. Platforms without that layout provide `null`.
 */
internal interface LayoutPreferences {
    var isSidebarCollapsed: Boolean
}

internal expect val layoutPreferences: LayoutPreferences?
