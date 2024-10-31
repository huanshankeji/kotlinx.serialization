/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
import Java9Modularity.configureJava9ModuleInfo
import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    kotlin("jvm")
}

kotlin {
    jvmToolchain(jdkToolchainVersion)

    compilerOptions {
        jvmTarget = JvmTarget.JVM_1_8
        if (overriddenLanguageVersion != null) {
            languageVersion = KotlinVersion.fromVersion(overriddenLanguageVersion!!)
            freeCompilerArgs.add("-Xsuppress-version-warnings")
        }
        freeCompilerArgs.addAll("-Xjdk-release=1.8", "-Xjvm-default=all-compatibility")
    }

    sourceSets.all {
        languageSettings {
            progressiveMode = true

            optIn("kotlinx.serialization.InternalSerializationApi")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

configureJava9ModuleInfo()
