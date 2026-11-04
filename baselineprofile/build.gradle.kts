// SPDX-License-Identifier: MPL-2.0

import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Generates androidApp's baseline profile on a connected device or emulator:
// ./gradlew :androidApp:generateReleaseBaselineProfile
plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "dev.vladleesi.braindanceapp.baselineprofile"
    compileSdk = 37
    defaultConfig {
        minSdk = 28
        targetSdk = 37
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    targetProjectPath = ":androidApp"
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

baselineProfile {
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.tests.ext.junit)
    implementation(libs.androidx.tests.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}
