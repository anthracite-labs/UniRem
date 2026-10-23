// Throwaway Spike A harness. This build is standalone and is never included from
// the production settings.gradle.kts. AGP 9 has built-in Kotlin, so the
// kotlin-android plugin is deliberately not applied; the KGP version on the
// buildscript classpath pins the compiler to the catalog's Kotlin version.
buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
}
