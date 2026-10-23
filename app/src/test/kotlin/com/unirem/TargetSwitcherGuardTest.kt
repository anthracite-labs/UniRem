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
 * Scope guards for Story 1.9: one target/health/label owner, no Room
 * migration, no network work inside the switcher surfaces, no secret or
 * address material in the switcher, and the new user copy.
 */
class TargetSwitcherGuardTest {

    private val repoRoot: Path = findRepoRoot()
    private val appMain: Path = repoRoot.resolve("app/src/main")
    private val switcherSheet: String by lazy {
        appMain.resolve("kotlin/com/unirem/setup/TvSwitcherSheet.kt").readText()
    }
    private val setupChrome: String by lazy {
        appMain.resolve("kotlin/com/unirem/setup/SetupChrome.kt").readText()
    }

    @Test
    fun singleTargetOwner() {
        val owners = kotlinSources(appMain)
            .filter { (_, text) -> text.contains("class TargetSessionHolder") }
        assertTrue(
            "exactly one TargetSessionHolder owner, found ${owners.map { it.first }}",
            owners.size == 1,
        )
        val instantiations = kotlinSources(appMain)
            .flatMap { (_, text) ->
                text.lines().filter { it.contains("TargetSessionHolder(") }
            }
        assertTrue(
            "exactly one TargetSessionHolder instantiation, found $instantiations",
            instantiations.size == 1,
        )
    }

    @Test
    fun singleCapabilityLabelOwner() {
        val instantiations = kotlinSources(appMain)
            .flatMap { (_, text) ->
                text.lines().filter {
                    it.contains("CapabilityLabelTracker(") && !it.contains("class CapabilityLabelTracker")
                }
            }
        assertTrue(
            "exactly one CapabilityLabelTracker instantiation, found $instantiations",
            instantiations.size == 1,
        )
    }

    @Test
    fun switcherOpensNoNetworkSession() {
        val banned = listOf(
            "TvGateways", "DatagramSocket", "MulticastLock", "WifiManager",
            "okhttp3", "jupnp", "ktor", "Socket", "WebSocket", "connect(",
        )
        for ((name, text) in listOf("TvSwitcherSheet" to switcherSheet, "SetupChrome" to setupChrome)) {
            for (token in banned) {
                assertFalse(
                    "$name must not reference $token",
                    text.contains(token),
                )
            }
        }
    }

    @Test
    fun noRoomMigrationAndNoLastUsedColumn() {
        val store = appMain.resolve("kotlin/com/unirem/tv/RoomTvStore.kt").readText()
        assertTrue("TvDatabase must stay at version 3", store.contains("version = 3"))
        assertFalse("no new Room migration", store.contains("MIGRATION_3"))
        for (token in listOf("isActive", "lastUsed", "last_used", "last-used")) {
            assertFalse("TV record must not gain $token", store.contains(token))
        }
        val holder = appMain.resolve("kotlin/com/unirem/connection/TargetSession.kt").readText()
        for (token in listOf("DataStore", "SharedPreferences", "isActive", "lastUsed")) {
            assertFalse("TargetSession must not persist $token", holder.contains(token))
        }
    }

    @Test
    fun switcherCarriesNoSecretsOrAddresses() {
        val secret = Regex("""\b(token|pin|spki)\b""", RegexOption.IGNORE_CASE)
        for ((name, text) in listOf("TvSwitcherSheet" to switcherSheet, "SetupChrome" to setupChrome)) {
            assertFalse("$name must not name secrets", secret.containsMatchIn(text))
            assertFalse("$name must not read the stored MAC", text.contains("wifiMac"))
            assertFalse("$name must not read the stored IP", text.contains("lastIp"))
        }
    }

    @Test
    fun switcherHasNoPrintsOrDirectLogs() {
        val directLog = Regex("""android\.util\.Log|(?<![A-Za-z.])Log\.[diwev]\(|println\(""")
        for ((name, text) in listOf("TvSwitcherSheet" to switcherSheet, "SetupChrome" to setupChrome)) {
            assertFalse("$name prints or logs directly", directLog.containsMatchIn(text))
        }
    }

    @Test
    fun switcherCopyExists() {
        val xml = appMain.resolve("res/values/strings.xml").readText()
        for (name in listOf(
            "switcher_tvs", "switcher_current", "switcher_open_cd",
            "switcher_not_connected", "switcher_unavailable",
            "switcher_pair_again", "switcher_pair_again_cd",
        )) {
            assertTrue("strings.xml must define $name", xml.contains("name=\"$name\""))
        }
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
            check(Files.isDirectory(root)) { "source root missing, guard would be vacuous: $root" }
            return Files.walk(root).use { stream ->
                stream.filter { Files.isRegularFile(it) && it.toString().endsWith(".kt") }
                    .map { it to it.readText() }.toList()
            }
        }
    }
}
