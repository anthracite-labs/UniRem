package com.unirem

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Scope guards for Story 1.8: the capability surface stays inside the
 * architecture allowlist, with one owner, one resolver, one NetworkType,
 * no stored label, and no release-visible harness.
 */
class CapabilityLabelGuardTest {

    private val repoRoot: Path = findRepoRoot()
    private val appMain: Path = repoRoot.resolve("app/src/main")
    private val coreMain: Path = repoRoot.resolve("core/src/main")

    private fun kotlinSources(root: Path): List<Pair<Path, String>> {
        check(Files.isDirectory(root)) { "source root missing, guard would be vacuous: $root" }
        return Files.walk(root).use { stream ->
            stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                .map { it to it.readText() }.toList()
        }
    }

    @Test
    fun singleCapabilityOwner() {
        val owners = kotlinSources(appMain)
            .filter { (_, text) -> text.contains("class CapabilityLabelTracker") }
            .map { (path, _) -> path.toString() }
        assertEquals("exactly one CapabilityLabelTracker owner, found $owners", 1, owners.size)
    }

    @Test
    fun singleResolver() {
        val resolvers = kotlinSources(coreMain)
            .filter { (_, text) -> text.contains("fun resolve(): Map<CapabilityKey") }
            .map { (path, _) -> path.toString() }
        assertEquals("exactly one matrix resolve() definition, found $resolvers", 1, resolvers.size)
        val callers = kotlinSources(appMain)
            .filter { (_, text) -> text.contains(".resolve()") }
            .map { (path, _) -> path.toString() }
        assertEquals("exactly one resolve() caller, found $callers", 1, callers.size)
    }

    @Test
    fun noSecondNetworkType() {
        val defs = listOf(coreMain, repoRoot.resolve("tv/src/main"), appMain, repoRoot.resolve("cast/src/main"))
            .flatMap { kotlinSources(it) }
            .filter { (_, text) -> text.contains("enum class NetworkType") }
            .map { (path, _) -> path.toString() }
        assertEquals("exactly one NetworkType, found $defs", 1, defs.size)
    }

    @Test
    fun capabilityPackageStoresNoLabel() {
        val capability = kotlinSources(appMain.resolve("kotlin/com/unirem/capability"))
        assertTrue("capability package must exist", capability.isNotEmpty())
        val hits = capability.filter { (_, text) ->
            text.contains("SharedPreferences") || text.contains("DataStore") ||
                text.contains("getSharedPreferences") || text.contains("putString") ||
                text.contains("@Entity") || text.contains("@Dao") ||
                text.contains("Room.databaseBuilder")
        }.map { (path, _) -> path.toString() }
        assertTrue("no stored label in the capability package: $hits", hits.isEmpty())
    }

    @Test
    fun releaseBuildsExcludeOverlayRowsAndHarnessRoute() {
        val activity = appMain.resolve("kotlin/com/unirem/MainActivity.kt").readText()
        assertTrue("tracker must be debug-gated", activity.contains("CapabilityLabelTracker(scope, BuildConfig.DEBUG)"))
        assertTrue("overlay store must be debug-gated", activity.contains("tvStore.overlayStore(BuildConfig.DEBUG)"))
        assertTrue("harness entry must be debug-gated", activity.contains("if (BuildConfig.DEBUG && harnessHex != null"))
        assertTrue("harness route must be debug-gated", activity.contains("if (BuildConfig.DEBUG) {"))
        val tracker = appMain.resolve("kotlin/com/unirem/capability/CapabilityLabelTracker.kt").readText()
        assertTrue("release must read the asset only", tracker.contains("debug = debug"))
    }

    @Test
    fun capabilitySurfaceHasNoNetworkKeyOrLogViolations() {
        val keyToken = "KEY" + "_"
        val directLog = Regex("""android\.util\.Log|(?<![A-Za-z.])Log\.[diwev]\(|println\(""")
        val banned = listOf("okhttp" + "3", "org." + "jupnp", "io.ktor")
        val sources = kotlinSources(appMain.resolve("kotlin/com/unirem/capability"))
        for ((path, text) in sources) {
            for (token in banned) {
                assertTrue("$path must not reference $token", !text.contains(token))
            }
            assertTrue("$path must not reference protocol keys", !text.contains(keyToken))
            assertTrue("$path prints or logs directly", !directLog.containsMatchIn(text))
        }
    }

    @Test
    fun copyMappingLivesInAppNeverInCore() {
        val cardCopyInCore = kotlinSources(coreMain)
            .filter { (_, text) -> text.contains("Full control") || text.contains("Best-effort") }
        assertTrue("no UI copy in :core: $cardCopyInCore", cardCopyInCore.isEmpty())
        val copyInApp = kotlinSources(appMain)
            .filter { (_, text) -> text.contains("Full control — confirms keys") }
        assertTrue("card copy must live in :app", copyInApp.isNotEmpty())
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
