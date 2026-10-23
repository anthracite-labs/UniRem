package com.unirem

import java.nio.file.Path
import java.nio.file.Paths
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Pins the settled gate copy byte-for-byte: explainer, denied, card, and
 * readout strings. Sentence case, no exclamation marks.
 */
class LanGateCopyTest {

    private val xml: String by lazy {
        findRepoRoot().resolve("app/src/main/res/values/strings.xml").readText()
    }

    private fun value(name: String): String =
        Regex("""<string name="$name">([^<]+)</string>""")
            .find(xml)?.groupValues?.get(1)
            ?: error("strings.xml must define $name")

    @Test
    fun explainerCopyIsSettled() {
        assertEquals(
            "UniRem needs to find your TV on this Wi-Fi. Nothing is sent to the internet.",
            value("gate_explainer_body"),
        )
        assertEquals("Continue", value("gate_explainer_continue"))
        assertEquals("Not now", value("gate_not_now"))
    }

    @Test
    fun deniedCopyIsSettled() {
        assertEquals(
            "UniRem needs local network access to reach your TV.",
            value("gate_denied_body"),
        )
        assertEquals("Open settings", value("gate_denied_settings"))
    }

    @Test
    fun cardAndReadoutCopyIsSettled() {
        assertEquals("Local network access", value("gate_card_title"))
        assertEquals(
            "Allow access to find your TV on this Wi-Fi.",
            value("gate_card_needed"),
        )
        assertEquals("Local network access ready", value("gate_card_ready"))
        assertEquals("Not scanned", value("gate_readout_not_scanned"))
        assertEquals("Wi-Fi", value("gate_net_wifi"))
        assertEquals("Ethernet", value("gate_net_ethernet"))
        assertEquals("Unknown", value("gate_net_unknown"))
    }

    @Test
    fun gateCopyHasNoExclamations() {
        val gateValues = Regex("""<string name="gate_[^"]+">([^<]+)</string>""")
            .findAll(xml).map { it.groupValues[1] }.toList()
        assertTrue("gate strings must exist", gateValues.isNotEmpty())
        for (text in gateValues) {
            assertFalse("gate copy must not exclaim: $text", text.contains("!"))
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
