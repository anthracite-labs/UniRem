package com.unirem

import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Guards the shell's static copy: the three shell strings exist, the version
 * line carries a slot for the build version, and no shell string claims a
 * connection or command state the shell cannot observe. Gate copy carries
 * its own settled wording (pinned by LanGateCopyTest) and is out of scope.
 */
class ShellStringsTest {

    private val xml: String by lazy {
        findRepoRoot().resolve("app/src/main/res/values/strings.xml").readText()
    }

    @Test
    fun shellStringsExist() {
        for (name in listOf("shell_title", "shell_subtitle", "shell_version")) {
            assertTrue("strings.xml must define $name", xml.contains("name=\"$name\""))
        }
    }

    @Test
    fun versionStringCarriesSlot() {
        val version = Regex("""<string name="shell_version">([^<]+)</string>""")
            .find(xml)?.groupValues?.get(1)
        assertTrue("shell_version must carry a version slot", version != null && version.contains("%1\$s"))
    }

    @Test
    fun shellStringsMakeNoStateClaims() {
        val values = Regex("""<string name="(shell_[^"]+)">([^<]+)</string>""").findAll(xml)
            .map { it.groupValues[2].lowercase() }.toList()
        assertTrue("shell strings must exist", values.isNotEmpty())
        for (claim in listOf("connect", "pair", "confirm", "offline", "online", "waiting", "idle", "sent")) {
            assertFalse(
                "no shell string may claim '$claim'",
                values.any { it.contains(claim) },
            )
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
    }
}
