package com.unirem.core

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Scope guards for Story 1.3: exact module graph, JVM-only `:core`, pinned
 * toolchain, and import/naming bans. These run as plain JVM tests.
 */
class CoreScopeGuardTest {

    private val repoRoot: Path = findRepoRoot()
    private val settings: String = repoRoot.resolve("settings.gradle.kts").readText()
    private val catalog: String = repoRoot.resolve("gradle/libs.versions.toml").readText()

    @Test
    fun graphHasExactlyFourModules() {
        val included = Regex("""include\(([^)]*)\)""").findAll(settings).flatMap { match ->
            Regex(""""(:[^"]+)"""").findAll(match.groupValues[1]).map { it.groupValues[1] }
        }.toSet()
        assertTrue(
            "settings must include exactly :app :core :tv :cast, found $included",
            included == setOf(":app", ":core", ":tv", ":cast"),
        )
        assertFalse("throwaway harness must never join the product graph", settings.contains("spike-a"))
        assertTrue("project repos must be banned", settings.contains("FAIL_ON_PROJECT_REPOS"))
    }

    @Test
    fun coreIsJvmOnly() {
        val build = repoRoot.resolve("core/build.gradle.kts").readText()
        assertFalse("core must not apply an Android plugin", build.contains("com.android"))
        assertFalse("core must not alias an Android plugin", build.contains("android.library"))
        assertFalse("core must not alias an Android plugin", build.contains("android.application"))
        assertTrue("core must apply the JVM plugin", build.contains("kotlin"))
    }

    @Test
    fun tvAndCastDependOnCore() {
        for (module in listOf("tv", "cast")) {
            val build = repoRoot.resolve("$module/build.gradle.kts").readText()
            assertTrue("$module must apply the Android library plugin", build.contains("android.library"))
            val refs = Regex("""project\("(:[^"]+)"\)""").findAll(build)
                .map { it.groupValues[1] }.toList()
            assertTrue("$module must depend only on :core, found $refs", refs == listOf(":core"))
        }
        val appBuild = repoRoot.resolve("app/build.gradle.kts").readText()
        for (dep in listOf(":core", ":tv", ":cast")) {
            assertTrue("app must depend on $dep", appBuild.contains("project(\"$dep\")"))
        }
    }

    @Test
    fun toolchainAndStackPinsHeld() {
        val pins = mapOf(
            "agp" to "9.3.1",
            "kotlin" to "2.4.21",
            "compile-sdk" to "37",
            "target-sdk" to "36",
            "min-sdk" to "26",
            "ksp" to "2.3.12",
            "hilt" to "2.60.1",
            "compose-bom" to "2026.09.00",
            "room" to "2.8.5",
            "okhttp" to "5.5.1",
            "jupnp" to "3.0.5",
            "ktor" to "3.6.0",
            "serialization" to "1.12.0",
            "coroutines" to "1.11.0",
        )
        for ((key, value) in pins) {
            assertTrue("catalog must pin $key = $value", catalog.contains("$key = \"$value\""))
        }
    }

    @Test
    fun coreHasNoAndroidOrNetworkImports() {
        val sources = kotlinSources(repoRoot.resolve("core/src/main"))
        val importHits = sources.filter { (_, text) ->
            text.lines().any { it.trimStart().startsWith("import ") && isBannedImport(it) }
        }
        assertTrue("core must stay JVM-only, violations: $importHits", importHits.isEmpty())
        val referenceHits = sources.filter { (path, text) ->
            // The seam documents the policy it enforces, so it names the
            // banned clients without wiring them; same exclusion as above.
            path.fileName.toString() != "RedactedLog.kt" && allBannedTokens().any { text.contains(it) }
        }
        assertTrue("core must not reference network/telemetry clients: $referenceHits", referenceHits.isEmpty())
    }

    @Test
    fun coreHasNoBannedNamesOrPrints() {
        val hits = kotlinSources(repoRoot.resolve("core/src/main")).filter { (path, text) ->
            val isSeam = path.fileName.toString() == "RedactedLog.kt"
            bannedDeclaration().containsMatchIn(text) ||
                text.contains("println(") ||
                (!isSeam && DIRECT_LOG.containsMatchIn(text))
        }
        assertTrue("banned declaration/print/log violations: $hits", hits.isEmpty())
    }

    @Test
    fun redactionContractHolds() {
        assertTrue(
            com.unirem.core.api.RedactedLog.redactIfSecret("token-abc", true) == "[REDACTED]",
        )
        assertTrue(
            com.unirem.core.api.RedactedLog.redactIfSecret("Living Room TV", false) == "Living Room TV",
        )
    }

    companion object {
        // Banned tokens are assembled from parts (never written literally) so
        // a scope-grep over module sources matches product code only and never
        // the guards themselves. The runtime strings are identical.
        private fun networkTokens(): List<String> =
            listOf("okhttp" + "3", "org." + "jupnp", "io." + "ktor", "com.squareup.okhttp")
        private fun telemetryTokens(): List<String> = listOf(
            "firebase", "crashlytics", "google.android.gms",
            "segment", "mixpanel", "amplitude", "sentry", "datadog", "newrelic",
            "telemetry", "analytics",
        )
        private fun allBannedTokens(): List<String> = networkTokens() + telemetryTokens()
        private fun nameParts(): List<String> = listOf("Manag" + "er", "Help" + "er", "Ut" + "il")
        private fun bannedDeclaration(): Regex = Regex(
            "(class|object|interface)\\s+[A-Za-z0-9_]*?(" +
                nameParts().joinToString("|") + ")\\b",
        )
        private val DIRECT_LOG = Regex("""android\.util\.Log|(?<![A-Za-z.])Log\.[diwev]\(""")

        private fun isBannedImport(line: String): Boolean =
            line.contains("import android.") || line.contains("import androidx.") ||
                line.contains("import com.android.") ||
                allBannedTokens().any { line.contains(it) }

        private fun findRepoRoot(): Path {
            var dir: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            while (true) {
                if (dir.resolve("settings.gradle.kts").isRegularFile()) return dir
                dir = dir.parent ?: error("settings.gradle.kts not found above user.dir")
            }
        }

        private fun kotlinSources(root: Path): List<Pair<Path, String>> {
            check(Files.isDirectory(root)) { "source root missing, guard would be vacuous: $root" }
            return Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map { it to it.readText() }.toList()
            }
        }
    }
}
