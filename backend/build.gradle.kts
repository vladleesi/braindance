// SPDX-License-Identifier: MPL-2.0

import io.gitlab.arturbosch.detekt.Detekt

plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.detekt)
}

group = "dev.vladleesi.braindanceapp"
version = "0.6.2"

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

application {
    mainClass.set("dev.vladleesi.braindanceapp.backend.MainKt")
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.client.cio)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines)
    implementation(libs.redis.jedis)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.unit.tests.kotlin)
    testImplementation(libs.unit.tests.jupiter)
    testRuntimeOnly(libs.unit.tests.launcher)
}

tasks.jar {
    manifest.attributes("Implementation-Version" to project.version.toString())
}

tasks.named<JavaExec>("run") {
    systemProperty("backendVersion", project.version.toString())
}

tasks.test {
    useJUnitPlatform()
    systemProperty("backendVersion", project.version.toString())
}

tasks.named("detekt") {
    dependsOn("detektMain", "detektTest")
}

listOf("detektMain", "detektTest").forEach { taskName ->
    tasks.named<Detekt>(taskName) {
        config.setFrom(rootProject.files("config/detekt-nullability.yml"))
    }
}
