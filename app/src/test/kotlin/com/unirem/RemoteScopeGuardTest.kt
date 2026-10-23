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
 * Scope guards for Story 1.10: the remote shell adds no network/client
 * imports, no command rows, no second session owner, and the old status
 * surface entry is gone.
 */
class RemoteScopeGuardTest {

    private val repoRoot: Path = findRepoRoot()
    private val appMain: Path = repoRoot.resolve("app/src/main")
    private val remoteDir: Path = appMain.resolve("kotlin/com/unirem/remote")
    private val mainActivity: String by lazy {
        appMain.resolve("kotlin/com/unirem/MainActivity.kt").readText()
    }
    private val shellScreen: String by lazy {
        appMain.resolve("kotlin/com/unirem/ShellScreen.kt").readText()
    }

    private fun bannedImportTokens(): List<String> = listOf(
        "okhttp" + "3", "org." + "jupnp", "io.ktor", "com.squareup.okhttp",
        "firebase", "crashlytics", "com.google.android.gms",
        "segment", "mixpanel", "amplitude", "sentry", "datadog", "newrelic",
        "telemetry", "analytics",
    )

    @Test
    fun remoteHasNoNetworkOrKeyReferences() {
        val banned = bannedImportTokens()
        val keyToken = "KEY" + "_"
        for ((path, text) in kotlinSources(remoteDir)) {
            val badImports = text.lines().filter { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("import ") && banned.any { trimmed.contains(it) }
            }
            assertTrue("$path has banned imports: $badImports", badImports.isEmpty())
            for (token in banned) {
                assertFalse("$path references $token", text.contains(token))
            }
            assertFalse("$path references protocol constants", text.contains(keyToken))
            assertFalse(
                "$path reaches past the api package",
                text.contains("com.unirem.tv.internal"),
            )
            // Session-only trust stays in memory: the shell package never
            // touches Room, Keystore, or any other persisted store.
            for (token in listOf("Room", "Keystore", "DataStore", "SharedPreferences")) {
                assertFalse("$path references persisted store $token", text.contains(token))
            }
        }
    }

    @Test
    fun remoteHasNoPrintsDirectLogsOrBannedNames() {
        val directLog = Regex("""android\.util\.Log|(?<![A-Za-z.])Log\.[diwev]\(|println\(""")
        val bannedDeclaration = Regex(
            "(class|object|interface)\\s+[A-Za-z0-9_]*?(Manag|Help|Ut)(er|il)\\b",
        )
        for ((path, text) in kotlinSources(remoteDir)) {
            assertFalse("$path prints or logs directly", directLog.containsMatchIn(text))
            assertFalse("$path has banned declarations", bannedDeclaration.containsMatchIn(text))
            if (!text.contains("RedactedLog")) {
                assertFalse("$path uses raw Log", text.contains("Log."))
            }
        }
    }

    @Test
    fun remoteRendersDpadWithHonestWiring() {
        val remoteText = kotlinSources(remoteDir).joinToString("\n") { it.second }
        // The 3×3 cluster, the five-state outcome flow, and the user-confirm
        // path are the Story 1.11 surface: taps are the only submit source.
        for (token in listOf(
            "DpadCluster", "DirectionKey", "OkKey", "CommandOutcome", "RemoteCommand",
            "onDpad", "ItWorkedButton", "remote_it_worked",
        )) {
            assertTrue("remote must wire $token", remoteText.contains(token))
        }
        // No socket, service, or key work lives in the shell: one submit per
        // tap yields one Click frame in `:tv`, and holds belong to Story 1.13.
        for (token in listOf(
            "CommandTracker", "TvConnectionControl", "ConnectionSession",
            "PinnedTls", "WebSocket", "MulticastLock", "WifiManager",
            "FOREGROUND", "WakeLock", "startForeground", "NotificationManager",
            "android.app.Service", "androidx.compose.material",
            "\"Press\"", "\"Release\"", "VolumeUp", "PowerOff", "VoiceSearch",
            "PointerMode", "CastSession", "MirrorSession",
        )) {
            assertFalse("remote must not reference $token", remoteText.contains(token))
        }
    }

    @Test
    fun remoteReusesBannerDotAndLauncher() {
        val remoteText = kotlinSources(remoteDir).joinToString("\n") { it.second }
        assertTrue("remote reuses the oxblood banner", remoteText.contains("ConnectionBanner"))
        assertTrue("remote reuses the health dot", remoteText.contains("HealthDot"))
        assertTrue("remote reuses the disabled launcher", remoteText.contains("DisabledLauncher"))
        assertTrue("remote maps Idle vs Offline only", remoteText.contains("RemoteRules.ringFor"))
    }

    @Test
    fun checkConnectionEntryIsGone() {
        assertFalse("shell must not offer Check connection", shellScreen.contains("connection_check"))
        assertFalse("shell must not wire onCheckConnection", shellScreen.contains("onCheckConnection"))
        assertTrue("shell offers Open remote", shellScreen.contains("remote_open"))
        assertFalse("host must not reference the removed route", mainActivity.contains("SetupRoute.Connection"))
        assertFalse("host must not reference the removed entry", mainActivity.contains("connection_check"))
        assertFalse("host must not wire the removed entry", mainActivity.contains("onCheckConnection"))
        assertTrue("host opens the remote route", mainActivity.contains("SetupRoute.Remote"))
        assertTrue("host renders the remote shell", mainActivity.contains("RemoteScreen("))
    }

    @Test
    fun remoteCopyExists() {
        val xml = appMain.resolve("res/values/strings.xml").readText()
        for (name in listOf(
            "remote_open", "remote_open_cd",
            "remote_idle", "remote_offline",
            "remote_sent", "remote_waiting",
            "remote_tv_confirmed", "remote_you_confirmed",
            "remote_it_worked", "remote_it_worked_cd",
            "dpad_up", "dpad_down", "dpad_left", "dpad_right", "dpad_ok",
        )) {
            assertTrue("strings.xml must define $name", xml.contains("name=\"$name\""))
        }
        assertFalse("old status copy must be unwired", mainActivity.contains("R.string.connection_check"))
        // The ring stays purely visual: the state line carries the label, so
        // no ring content-description copy may exist or be referenced.
        assertFalse("ring must have no content-description copy", xml.contains("remote_ring"))
        val remoteText = kotlinSources(remoteDir).joinToString("\n") { it.second }
        assertFalse("ring must have no content-description reference", remoteText.contains("remote_ring"))
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
