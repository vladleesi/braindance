// SPDX-License-Identifier: MPL-2.0

@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.vladleesi.braindanceapp.system

internal actual fun hasTouchScreen(): Boolean = js("navigator.maxTouchPoints > 0")
