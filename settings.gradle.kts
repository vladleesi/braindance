// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "braindance"
include(":shared")
include(":androidApp")
include(":baselineprofile")
include(":webApp")
