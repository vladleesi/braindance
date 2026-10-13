// SPDX-License-Identifier: MPL-2.0

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        }
    }
}

rootProject.name = "braindance"
include(":shared")
include(":androidApp")
include(":webApp")
include(":backend")
