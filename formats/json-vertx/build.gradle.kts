/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

plugins {
    kotlin("jvm")
    kotlin("plugin.serialization")
    `jvm-only-conventions`
}

dependencies {
    api(project(":kotlinx-serialization-core"))
    api(project(":kotlinx-serialization-json"))
    implementation(libs.vertx.core)

    //testImplementation(libs.kotlin.test)
}

kotlin {
    sourceSets {
        configureEach {
            languageSettings {
                optIn("kotlinx.serialization.json.internal.JsonFriendModuleApi")
            }
        }
    }
}
