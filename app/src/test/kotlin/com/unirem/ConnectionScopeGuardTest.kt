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
 * Scope guards for Story 1.7: the status surface adds no network/client
 * imports, no telemetry, no banned names, no second health owner, and no
 * background socket, service, wake lock, or notification.
 */
class ConnectionScopeGuardTest {

    private val repoRoot: Path = findRepoRoot()
    private val appMain: Path = repoRoot.resolve("app/src/main")
    private val tvMain: Path = repoRoot.resolve("tv/src/main")

    private fun bannedImportTokens(): List<String> = listOf(
        "okhttp" + "3", "org." + "jupnp", "io.ktor", "com.squareup.okhttp",
        "firebase", "crashlytics", "google.android.gms",
        "segment", "mixpanel", "amplitude", "sentry", "datadog", "newrelic",
        "telemetry", "analytics",
    )
    private fun bannedNameParts(): List<String> = listOf("Manag" + "er", "Help" + "er", "Ut" + "il")
    private fun keyToken(): String = "KEY" + "_"

    @Test
    fun connectionSurfaceHasNoNetworkOrKeyReferences() {
        val banned = bannedImportTokens()
        val sources = kotlinSources(appMain.resolve("kotlin/com/unirem/connection"))
        for ((path, text) in sources) {
            val badImports = text.lines().filter { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("import ") && banned.any { trimmed.contains(it) }
            }
            assertTrue("$path has banned imports: $badImports", badImports.isEmpty())
            assertFalse("$path references protocol key constants", text.contains(keyToken()))
        }
    }

    @Test
    fun connectionSurfaceHasNoPrintsOrDirectLogs() {
        val directLog = Regex("""android\.util\.Log|(?<![A-Za-z.])Log\.[diwev]\(|println\(""")
        val sources = kotlinSources(appMain.resolve("kotlin/com/unirem/connection"))
        for ((path, text) in sources) {
            assertFalse("$path prints or logs directly", directLog.containsMatchIn(text))
        }
    }

    @Test
    fun connectionSurfaceHasNoBannedNames() {
        val bannedDeclaration = Regex(
            "(class|object|interface)\\s+[A-Za-z0-9_]*?(" +
                bannedNameParts().joinToString("|") + ")\\b",
        )
        val sources = kotlinSources(appMain.resolve("kotlin/com/unirem/connection")) +
            kotlinSources(tvMain.resolve("kotlin/com/unirem/tv/channel")) +
            kotlinSources(tvMain.resolve("kotlin/com/unirem/tv/internal"))
                .filter { (path, _) -> path.fileName.toString().contains("Connection") }
        val hits = sources.filter { (_, text) -> bannedDeclaration.containsMatchIn(text) }
        assertTrue("banned declarations: $hits", hits.isEmpty())
    }

    @Test
    fun singleHealthOwner() {
        val owners = kotlinSources(tvMain)
            .filter { (_, text) -> text.contains("class ConnectionHealth") }
            .map { (path, _) -> path.toString() }
        assertTrue("exactly one ConnectionHealth owner, found $owners", owners.size == 1)
    }

    @Test
    fun noBackgroundSocketOrService() {
        val hits = kotlinSources(appMain.resolve("kotlin/com/unirem/connection"))
            .filter { (_, text) ->
                text.contains("FOREGROUND_SERVICE") || text.contains("WakeLock") ||
                    text.contains("startForeground") || text.contains("NotificationManager") ||
                    text.contains("android.app.Service")
            }
        assertTrue("background socket/service hits: $hits", hits.isEmpty())
        val manifest = appMain.resolve("AndroidManifest.xml").readText()
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        assertFalse("manifest must declare no service", manifest.contains("<service"))
    }

    @Test
    fun connectionLogsThroughRedactedLogOnly() {
        val sources = kotlinSources(appMain.resolve("kotlin/com/unirem/connection"))
        val nonCompliant = sources.filter { (_, text) ->
            text.contains("Log.") && !text.contains("RedactedLog")
        }
        assertTrue("non-RedactedLog logging: $nonCompliant", nonCompliant.isEmpty())
    }

    companion object {
        private fun findRepoRoot(): Path {
            var dir: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            while (true) {
                if (dir.resolve("settings.gradle.kts").isRegularFile()) return dir
                dir = dir.parent ?: error("settings.gradle.kts not found above user.dir")
            }
        }

        private fun kotlinSources(root: Path): List<Pair<Path, String>> {
            if (!Files.isDirectory(root)) return emptyList()
            return Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map { it to it.readText() }.toList()
            }
        }
    }
}
