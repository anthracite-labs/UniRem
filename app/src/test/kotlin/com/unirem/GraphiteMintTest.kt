package com.unirem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.unirem.ui.graphitemint.GraphiteMint
import com.unirem.ui.graphitemint.GraphiteMintSpacing
import com.unirem.ui.graphitemint.GraphiteMintType
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.math.roundToInt
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.io.path.isRegularFile
import kotlin.io.path.readText

/**
 * Pins the Graphite Mint foundation values: token hexes, type sizes, spacing,
 * the `colors.xml` launch-background duplicate, and the theme's ground.
 * No new dependencies; Compose `Color`/`sp`/`dp` come from the `:app`
 * compile classpath the tests already see.
 */
class GraphiteMintTest {

    @Test
    fun groundTokensHold() {
        assertEquals(Color(0xFF151719), GraphiteMint.SurfaceBase)
        assertEquals(Color(0xFF1E2124), GraphiteMint.SurfaceRaised)
        assertEquals(Color(0xFF2A2E32), GraphiteMint.SurfaceKey)
        assertEquals(Color(0xFF343A3E), GraphiteMint.Hairline)
    }

    @Test
    fun inkAndSignalTokensHold() {
        assertEquals(Color(0xFFECEEF0), GraphiteMint.InkPrimary)
        assertEquals(Color(0xFF98A1A9), GraphiteMint.InkSecondary)
        assertEquals(Color(0xFF3DDC97), GraphiteMint.Accent)
    }

    @Test
    fun typeScaleHolds() {
        assertEquals(32.sp, GraphiteMintType.Display.fontSize)
        assertEquals(24.sp, GraphiteMintType.Title.fontSize)
        assertEquals(16.sp, GraphiteMintType.Body.fontSize)
        assertEquals(14.sp, GraphiteMintType.BodySmall.fontSize)
    }

    @Test
    fun spacingHolds() {
        assertEquals(20.dp, GraphiteMintSpacing.ScreenX)
        assertEquals(24.dp, GraphiteMintSpacing.Section)
        assertEquals(480.dp, GraphiteMintSpacing.MaxColumnWidth)
    }

    @Test
    fun xmlSurfaceBaseMatchesToken() {
        val root = findRepoRoot()
        val xml = root.resolve("app/src/main/res/values/colors.xml").readText()
        val xmlHex = Regex("""<color name="surface_base">([^<]+)</color>""")
            .find(xml)?.groupValues?.get(1)
        val token = GraphiteMint.SurfaceBase
        val tokenHex = "#%02X%02X%02X".format(
            (token.red * 255).roundToInt(),
            (token.green * 255).roundToInt(),
            (token.blue * 255).roundToInt(),
        )
        assertEquals("design ground", "#151719", tokenHex)
        assertEquals("colors.xml must not drift from the token", tokenHex, xmlHex)
    }

    @Test
    fun themePaintsSurfaceBase() {
        val root = findRepoRoot()
        val theme = root.resolve("app/src/main/kotlin/com/unirem/ui/graphitemint/Theme.kt").readText()
        assertTrue(
            "GraphiteMintTheme must paint GraphiteMint.SurfaceBase",
            theme.contains("GraphiteMint.SurfaceBase"),
        )
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
