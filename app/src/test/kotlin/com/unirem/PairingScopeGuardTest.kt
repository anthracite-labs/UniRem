package com.unirem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Scope-guard updates for Story 1.5: the single predicate, the single held
 * scope, the `:app` import ban over the new setup/persistence code, and the
 * backup-excluded pairing material.
 */
class PairingScopeGuardTest {

    private val repoRoot: Path = findRepoRoot()

    private fun sources(roots: List<String>): List<Pair<Path, String>> =
        roots.flatMap { root ->
            val dir = repoRoot.resolve(root)
            check(Files.isDirectory(dir)) { "source root missing, guard would be vacuous: $dir" }
            Files.walk(dir).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map { it to it.readText() }.toList()
            }
        }

    @Test
    fun singlePredicateLivesInCore() {
        val defs = sources(listOf("core/src/main", "tv/src/main", "app/src/main", "cast/src/main"))
            .filter { (_, text) -> text.contains("object PrivatePeer") }
        assertEquals("exactly one PrivatePeer predicate: $defs", 1, defs.size)
        assertTrue("predicate must live in :core", defs.single().first.toString().contains("core"))
        val allowHosts = sources(listOf("core/src/main"))
            .filter { (_, text) -> text.contains("fun allowsHost") }
        assertEquals("single allowsHost definition", 1, allowHosts.size)
    }

    @Test
    fun noSecondPredicateOrScopeOutsideGate() {
        val hits = sources(listOf("tv/src/main", "app/src/main", "cast/src/main", "core/src/main"))
            .filter { (path, text) ->
                val file = path.fileName.toString()
                (text.contains("MulticastLock") || text.contains("WifiManager")) &&
                    file != "LanAccessGate.kt"
            }.map { (path, _) -> path.toString() }
        assertTrue("no scope handle outside the gate file: $hits", hits.isEmpty())
        val predicateCopies = sources(listOf("tv/src/main", "app/src/main", "cast/src/main"))
            .filter { (_, text) -> text.contains("RFC1918") }
            .map { (path, _) -> path.toString() }
        assertTrue("the subnet rule is not duplicated: $predicateCopies", predicateCopies.isEmpty())
    }

    @Test
    fun appHasNoProductClientOrSecretReferences() {
        val appSources = sources(listOf("app/src/main"))
        val clientHits = appSources.filter { (_, text) ->
            text.contains("okhttp" + "3") || text.contains("org." + "jupnp") ||
                text.contains("com.squareup.okhttp")
        }
        assertTrue("app must not reference network clients: $clientHits", clientHits.isEmpty())
        val keyHits = appSources.filter { (_, text) -> text.contains("KEY" + "_") }
        assertTrue("app must not reference protocol key constants: $keyHits", keyHits.isEmpty())
    }

    @Test
    fun newCodeLogsOnlyThroughRedactedLog() {
        val hits = sources(listOf("app/src/main", "tv/src/main", "core/src/main"))
            .filter { (path, _) -> path.fileName.toString() != "RedactedLog.kt" }
            .filter { (_, text) ->
                text.contains("println(") || text.contains("android.util.Log") ||
                    Regex("""(?<![A-Za-z.])Log\.[diwev]\(""").containsMatchIn(text)
            }.map { (path, _) -> path.toString() }
        assertTrue("RedactedLog-only logging: $hits", hits.isEmpty())
    }

    @Test
    fun newCodeHasNoBannedNamesOrTelemetry() {
        val hits = sources(listOf("app/src/main", "tv/src/main", "core/src/main"))
            .filter { (path, _) -> path.fileName.toString() != "RedactedLog.kt" }
            .filter { (_, text) ->
                Regex("""(class|object|interface)\s+[A-Za-z0-9_]*?(Manag|Help|Ut)(er|il)\b""")
                    .containsMatchIn(text) ||
                    text.contains("telemetry") || text.contains("analytics") ||
                    text.contains("firebase") || text.contains("crashlytics")
            }.map { (path, _) -> path.toString() }
        assertTrue("no banned names or telemetry: $hits", hits.isEmpty())
    }

    @Test
    fun pairingMaterialIsBackupExcludedWithNoNewPermission() {
        val manifest = repoRoot.resolve("app/src/main/AndroidManifest.xml").readText()
            .replace(Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL), "")
        val declared = Regex("""<uses-permission[^>]*android:name="([^"]+)"""")
            .findAll(manifest).map { it.groupValues[1] }.toSet()
        assertEquals(
            setOf(
                "android.permission.ACCESS_LOCAL_NETWORK",
                "android.permission.ACCESS_NETWORK_STATE",
                "android.permission.CHANGE_WIFI_MULTICAST_STATE",
            ),
            declared,
        )
        assertTrue("manifest must exclude pairing prefs", manifest.contains("backup_rules"))
        assertTrue("manifest must exclude via extraction rules", manifest.contains("data_extraction_rules"))
        val backup = repoRoot.resolve("app/src/main/res/xml/backup_rules.xml").readText()
        assertTrue("backup rules must exclude the pairing file", backup.contains("unirem_pairing"))
        val extraction = repoRoot.resolve("app/src/main/res/xml/data_extraction_rules.xml").readText()
        assertTrue("extraction rules must exclude the pairing file", extraction.contains("unirem_pairing"))
    }

    @Test
    fun crossModuleSurfaceStaysInApiPackages() {
        val internalImports = sources(listOf("app/src/main"))
            .filter { (_, text) -> text.contains("com.unirem.tv.internal") }
            .map { (path, _) -> path.toString() }
        assertTrue("app must stay on the tv api package: $internalImports", internalImports.isEmpty())
    }

    companion object {
        private fun findRepoRoot(): Path {
            var dir: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
            while (true) {
                if (dir.resolve("settings.gradle.kts").isRegularFile()) return dir
                dir = dir.parent ?: error("settings.gradle.kts not found above user.dir")
            }
        }
    }
}
