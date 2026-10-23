plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.unirem"
    compileSdk = libs.versions.compile.sdk.get().toInt()

    defaultConfig {
        applicationId = "com.unirem"
        minSdk = libs.versions.min.sdk.get().toInt()
        targetSdk = libs.versions.target.sdk.get().toInt()
        versionCode = 1
        versionName = "0.3.0"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
        }
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":tv"))
    implementation(project(":cast"))
    implementation(libs.coroutines.core)
    implementation(libs.datastore.preferences)
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.activity.compose)
    testImplementation(libs.junit)
}

// Story 1.8: build-time matrix gate. A structural mirror of the :core
// MatrixRules checks that fails the build on a malformed asset, so a bad
// matrix can never ship. It is a backstop, not the oracle: the same file is
// parsed and validated by the real MatrixRules parser in release load and in
// MatrixAssetTest, which fails check on any divergence the mirror misses.
tasks.register("verifyMatrix") {
    group = "verification"
    description = "Validates verification-matrix.json against the matrix rules."
    val assetFile = layout.projectDirectory.file("src/main/assets/verification-matrix.json")
    inputs.file(assetFile)
    doLast {
        val file = assetFile.asFile
        if (!file.isFile) throw GradleException("matrix asset missing: ${file.path}")
        val json = file.readText()
        val failures = mutableListOf<String>()
        // Neutralize braces inside string literals before structural
        // scans so a brace inside an identity value cannot corrupt the
        // shape checks. Keys and other value content are preserved.
        val neutralized = Regex("\"(?:[^\"\\\\]|\\\\.)*\"").replace(json) { m ->
            m.value.replace(Regex("[{}]"), " ")
        }
        if (neutralized.count { it == '{' } != neutralized.count { it == '}' }) {
            failures.add("asset has unbalanced braces")
        }
        if (!neutralized.trimEnd().endsWith("}")) {
            failures.add("asset has trailing content after the root object")
        }
        val versions = Regex(""""matrixVersion"\s*:\s*(-?\d+)""").findAll(json)
            .map { it.groupValues[1].toInt() }.toList()
        val version = versions.firstOrNull() ?: 0
        if (version <= 0) failures.add("asset misses a positive top-level matrixVersion")
        val blocks = Regex("""\{[^{}]*\}""").findAll(neutralized).map { it.value }.toList()
        val rows = blocks.filter { it.contains("\"capability\"") }
        if (rows.isEmpty()) failures.add("asset rows must not be empty")
        if (versions.drop(1).any { it != version }) {
            failures.add("row pins a different matrixVersion")
        }
        if (versions.size != rows.size + 1) {
            failures.add("every row must declare its own matrixVersion")
        }
        val allowedCapability = setOf("Keys", "Text", "Pointer", "Wake", "Cast", "Mirror", "Voice")
        val allowedScope = setOf(
            "Device", "NativeField", "AppGrid", "PointerBrowser", "PointerAppGrid",
            "FormatJpeg", "FormatMp3", "FormatH264Aac", "FormatHevc", "Subtitles", "Seek",
        )
        val allowedNetwork = setOf("Wifi", "Ethernet", "Unknown")
        val allowedResult = setOf("Pass", "Partial", "Ambiguous", "Fail", "NotTested")
        val allowedPairs = mapOf(
            "Keys" to setOf("Device"),
            "Text" to setOf("NativeField", "AppGrid"),
            "Pointer" to setOf("PointerBrowser", "PointerAppGrid"),
            "Wake" to setOf("Device"),
            "Cast" to setOf("Device", "FormatJpeg", "FormatMp3", "FormatH264Aac", "FormatHevc", "Subtitles", "Seek"),
            "Mirror" to setOf("Device"),
            "Voice" to setOf("Device"),
        )
        fun field(block: String, name: String): String? =
            Regex(""""$name"\s*:\s*"((?:[^"\\\\]|\\\\.)*)"""").find(block)?.groupValues?.get(1)
        val seen = HashSet<String>()
        var fallback = false
        for (block in rows) {
            val model = field(block, "model")
            val firmware = field(block, "firmware")
            val network = field(block, "networkType")
            val capability = field(block, "capability")
            val scope = field(block, "scope")
            val result = field(block, "result")
            if (model == null || firmware == null || network == null ||
                capability == null || scope == null || result == null
            ) {
                failures.add("row misses a required string field")
                continue
            }
            if (model.isBlank() || firmware.isBlank()) failures.add("row has blank identity")
            if (capability !in allowedCapability) failures.add("row has unknown capability")
            if (scope !in allowedScope) failures.add("row has unknown scope")
            if (network !in allowedNetwork) failures.add("row has unknown networkType")
            if (result !in allowedResult) failures.add("row has unknown result")
            if (allowedPairs[capability]?.contains(scope) == false) {
                failures.add("row pairs an unknown capability with scope")
            }
            if ((model == "*") != (firmware == "*unknown*")) {
                failures.add("row mixes a wildcard with an exact identity")
            }
            if ((model == "*" || firmware == "*unknown*") && network != "Unknown") {
                failures.add("row pins a wildcard to an exact transport")
            }
            if (model.contains("://") || firmware.contains("://")) {
                failures.add("row carries a URL-like identity")
            }
            val key = listOf(model, firmware, network, capability, scope).joinToString("")
            if (!seen.add(key)) failures.add("asset has duplicate exact keys")
            if (model == "*" && firmware == "*unknown*" && network == "Unknown" &&
                capability == "Keys" && scope == "Device" && result == "Partial"
            ) {
                fallback = true
            }
        }
        if (!fallback) failures.add("asset misses the Keys/Device Partial fallback")
        val lowered = json.lowercase()
        // Same denylist as MatrixAssetTest: no secret, URL, or identity
        // material belongs in the versioned asset.
        for (token in listOf("eyj", "begin", "http", "token", "\"pin\"", "spki", "mac")) {
            if (lowered.contains(token)) failures.add("asset must not carry $token material")
        }
        if (failures.isNotEmpty()) {
            throw GradleException("verification-matrix.json invalid:\n" + failures.joinToString("\n"))
        }
    }
}

afterEvaluate {
    tasks.findByName("check")?.dependsOn("verifyMatrix")
}
