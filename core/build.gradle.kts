plugins {
    // Versionless: the KGP version is pinned once via the buildscript
    // classpath in the root build (catalog `kotlin` pin). Requesting a
    // version here collides with the classpath copy.
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.junit)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
