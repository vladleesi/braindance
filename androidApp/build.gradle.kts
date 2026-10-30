// SPDX-License-Identifier: MPL-2.0

import com.android.build.api.variant.ApplicationVariantBuilder
import com.android.build.api.variant.HostTestBuilder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

val mobileVersionProperties =
    Properties().apply {
        rootProject.file("version.xcconfig").inputStream().use(::load)
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

android {
    namespace = "dev.vladleesi.braindanceapp"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.vladleesi.braindanceapp"
        targetSdk = 37
        minSdk = 24
        versionCode = mobileVersionProperties.getProperty("CURRENT_PROJECT_VERSION").toInt()
        versionName = mobileVersionProperties.getProperty("MARKETING_VERSION")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildFeatures {
        compose = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        debug {
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

androidComponents {
    beforeVariants { variant -> enableComposePreviewUnitTests(variant) }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.koin.android)
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.tests.ext.junit)
    androidTestImplementation(libs.androidx.tests.runner)
}

// AGP 9.5 Compose previews package resources through every variant's unit-test component.
@Suppress("UnstableApiUsage")
fun enableComposePreviewUnitTests(variant: ApplicationVariantBuilder) {
    variant.hostTests.getValue(HostTestBuilder.UNIT_TEST_TYPE).enable = true
}
