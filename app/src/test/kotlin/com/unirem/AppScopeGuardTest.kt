package com.unirem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Scope guards for the `:app` shell: no network/client imports, no telemetry,
 * no banned names, shell-only manifest, provisional identity.
 */
class AppScopeGuardTest {

    private val repoRoot: Path = findRepoRoot()
    private val appMain: Path = repoRoot.resolve("app/src/main")

    // Banned tokens are assembled from parts (never written literally) so a
    // scope-grep over module sources matches product code only and never the
    // guards themselves. The runtime strings are identical.
    private fun bannedImportTokens(): List<String> = listOf(
        "okhttp" + "3", "org." + "jupnp", "io.ktor", "com.squareup.okhttp",
        "firebase", "crashlytics", "google.android.gms",
        "segment", "mixpanel", "amplitude", "sentry", "datadog", "newrelic",
        "telemetry", "analytics",
    )
    private fun bannedNameParts(): List<String> = listOf("Manag" + "er", "Help" + "er", "Ut" + "il")
    private fun keyToken(): String = "KEY" + "_"

    @Test
    fun appHasNoNetworkOrTelemetryReferences() {
        val banned = bannedImportTokens()
        val sources = kotlinSources()
        val importHits = sources.filter { (_, text) ->
            text.lines().any { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("import ") && banned.any { trimmed.contains(it) }
            }
        }
        assertTrue("banned network/telemetry imports: $importHits", importHits.isEmpty())
        val referenceHits = sources.filter { (_, text) -> banned.any { text.contains(it) } }
        assertTrue("banned network/telemetry references: $referenceHits", referenceHits.isEmpty())
    }

    @Test
    fun appHasNoKeyOrFrameReferences() {
        val token = keyToken()
        val hits = kotlinSources().filter { (_, text) -> text.contains(token) }
        assertTrue("app must not reference protocol key constants: $hits", hits.isEmpty())
    }

    @Test
    fun appHasNoBannedNamesPrintsOrDirectLogs() {
        val bannedDeclaration = Regex(
            "(class|object|interface)\\s+[A-Za-z0-9_]*?(" +
                bannedNameParts().joinToString("|") + ")\\b",
        )
        val directLog = Regex("""android\.util\.Log|(?<![A-Za-z.])Log\.[diwev]\(|println\(""")
        val hits = kotlinSources().filter { (_, text) ->
            bannedDeclaration.containsMatchIn(text) || directLog.containsMatchIn(text)
        }
        assertTrue("banned declaration/print/log violations: $hits", hits.isEmpty())
    }

    @Test
    fun appWiresNoNetworkClients() {
        val build = repoRoot.resolve("app/build.gradle.kts").readText()
        for (wiring in listOf("libs.okhttp", "libs.ktor", "libs.jupnp")) {
            assertFalse("app must not wire $wiring", build.contains(wiring))
        }
    }

    @Test
    fun manifestIsShellOnly() {
        val manifest = appMain.resolve("AndroidManifest.xml").readText()
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        assertFalse("shell requests no permissions", manifest.contains("<uses-permission"))
        for (permission in listOf(
            "RECORD_AUDIO", "POST_NOTIFICATIONS", "FOREGROUND_SERVICE",
            "MediaProjection", "NEARBY_WIFI_DEVICES",
            "ACCESS_FINE_LOCATION", "ACCESS_COARSE_LOCATION", "BLUETOOTH",
        )) {
            assertFalse("manifest must not mention $permission", manifest.contains(permission))
        }
        assertFalse("manifest must declare no service", manifest.contains("<service"))
        assertFalse("manifest must not use foreground services", manifest.contains("foregroundServiceType"))
        assertTrue("shell must be launchable", manifest.contains("android.intent.category.LAUNCHER"))
        assertTrue("launcher activity must be exported", manifest.contains("android:exported=\"true\""))
    }

    @Test
    fun identityIsProvisionalComUnirem() {
        val build = repoRoot.resolve("app/build.gradle.kts").readText()
        assertTrue("applicationId must be com.unirem", build.contains("applicationId = \"com.unirem\""))
        assertTrue("namespace must be com.unirem", build.contains("namespace = \"com.unirem\""))
        val identity = repoRoot.resolve("docs/app-identity.md").readText()
        assertTrue("identity record must exist", identity.contains("com.unirem"))
        assertTrue("confirmation must be pending", identity.contains("NOT YET"))
        assertTrue("signing owner must be TBD", identity.contains("TBD"))
    }

    companion object {
        private fun findRepoRoot(): Path {
            var dir: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            while (true) {
                if (dir.resolve("settings.gradle.kts").isRegularFile()) return dir
                dir = dir.parent ?: error("settings.gradle.kts not found above user.dir")
            }
        }

        private fun kotlinSources(): List<Pair<Path, String>> {
            var dir: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            while (true) {
                if (dir.resolve("settings.gradle.kts").isRegularFile()) break
                dir = dir.parent ?: error("settings.gradle.kts not found above user.dir")
            }
            val root = dir.resolve("app/src/main")
            check(Files.isDirectory(root)) { "source root missing, guard would be vacuous: $root" }
            return Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map { it to it.readText() }.toList()
            }
        }
    }
}
