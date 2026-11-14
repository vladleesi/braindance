// SPDX-License-Identifier: MPL-2.0

import com.android.build.api.dsl.AndroidSourceSet
import com.android.build.api.variant.ApplicationVariantBuilder
import com.android.build.api.variant.HostTestBuilder
import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.StringReader
import java.util.Properties

val mobileVersionProperties =
    Properties().apply {
        rootProject.file("version.xcconfig").inputStream().use(::load)
    }

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.baselineprofile)
    alias(libs.plugins.kotlin.compose.compiler)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

// A Gradle property (-P, ORG_GRADLE_PROJECT_*) wins over the ignored root local.properties.
val localProperties =
    providers
        .fileContents(rootProject.layout.projectDirectory.file("local.properties"))
        .asText
        .map { contents -> Properties().apply { load(StringReader(contents)) } }

fun configuredProperty(name: String): String =
    providers
        .gradleProperty(name)
        .orElse(localProperties.map { properties -> properties.getProperty(name).orEmpty() })
        .orNull
        .orEmpty()
        .trim()

// Signing for staging tester builds (docs/firebase.md). Without these values, as in CI, staging APKs stay
// unsigned; a partial set fails instead of silently producing an unsigned APK. Release builds stay unsigned.
val stagingBuildType = "staging"
val stagingSigning =
    listOf("STAGING_STORE_FILE", "STAGING_STORE_PASSWORD", "STAGING_KEY_ALIAS", "STAGING_KEY_PASSWORD")
        .associateWith(::configuredProperty)
val hasStagingSigning = stagingSigning.values.all(String::isNotEmpty)
require(hasStagingSigning || stagingSigning.values.all(String::isEmpty)) {
    "Set all of ${stagingSigning.keys.joinToString()} or none of them."
}

// Firebase reads androidApp/google-services.json, which stays out of Git. Builds without it, such as CI and
// contributor builds, skip its resources and the mapping upload, so the app starts with Firebase off.
val hasFirebaseConfig = file("google-services.json").isFile

googleServices {
    missingGoogleServicesStrategy = MissingGoogleServicesStrategy.IGNORE
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
        manifestPlaceholders["crashlyticsCollectionEnabled"] = "true"
    }
    signingConfigs {
        if (hasStagingSigning) {
            create(stagingBuildType) {
                storeFile = rootProject.file(stagingSigning.getValue("STAGING_STORE_FILE"))
                storePassword = stagingSigning.getValue("STAGING_STORE_PASSWORD")
                keyAlias = stagingSigning.getValue("STAGING_KEY_ALIAS")
                keyPassword = stagingSigning.getValue("STAGING_KEY_PASSWORD")
            }
        }
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
            // Development crashes stay out of the Crashlytics dashboard.
            manifestPlaceholders["crashlyticsCollectionEnabled"] = "false"
        }
        release {
            // R8 shrinks and optimizes the release build; the baseline profile precompiles startup and browsing code.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
        // Tester builds sent through Firebase App Distribution: optimized like release, with their own application
        // ID so they install next to a store release signed with a different key.
        create(stagingBuildType) {
            initWith(getByName("release"))
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            signingConfig = if (hasStagingSigning) signingConfigs.getByName(stagingBuildType) else null
        }
        // Only builds that reach users upload R8 mappings, and only with the Firebase configuration. This also covers
        // the benchmark and non-minified types the baseline profile plugin adds, which would otherwise try to upload.
        configureEach {
            val uploadsMapping = hasFirebaseConfig && name in setOf("release", stagingBuildType)
            configure<CrashlyticsExtension> { mappingFileUploadEnabled = uploadsMapping }
        }
    }
    sourceSets {
        useReleaseBaselineProfile(getByName(stagingBuildType))
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
    implementation(libs.sharekit)
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.runtime)
    implementation(libs.compose.material)
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.koin.android)
    implementation(libs.androidx.profileinstaller)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    baselineProfile(project(":baselineprofile"))
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.tests.ext.junit)
    androidTestImplementation(libs.androidx.tests.runner)
}

// AGP 9.5 Compose previews package resources through every variant's unit-test component.
@Suppress("UnstableApiUsage")
fun enableComposePreviewUnitTests(variant: ApplicationVariantBuilder) {
    variant.hostTests.getValue(HostTestBuilder.UNIT_TEST_TYPE).enable = true
}

// Staging installs the same startup and browsing profile as release. AGP's baselineProfiles source set is incubating.
@Suppress("UnstableApiUsage")
fun useReleaseBaselineProfile(sourceSet: AndroidSourceSet) {
    sourceSet.baselineProfiles.directories.add("src/release/generated/baselineProfiles")
}
