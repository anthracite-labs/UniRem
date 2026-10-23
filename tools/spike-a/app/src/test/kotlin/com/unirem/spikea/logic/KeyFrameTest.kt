package com.unirem.spikea.logic

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyFrameTest {

    @Test
    fun clickFrameHasMethodCmdAndSendRemoteKey() {
        val parsed = KeyFrame.parse(KeyFrame.click(MvpKey.OK))
        assertNotNull(parsed)
        assertEquals("Click", parsed!!.cmd)
        assertEquals("KEY_ENTER", parsed.dataOfCmd)
        val raw = JSONObject(KeyFrame.click(MvpKey.OK))
        assertEquals("ms.remote.control", raw.getString("method"))
        val params = raw.getJSONObject("params")
        assertEquals("false", params.getString("Option"))
        assertEquals("SendRemoteKey", params.getString("TypeOfRemote"))
    }

    @Test
    fun pressAndReleaseFramesCarryTheSameCode() {
        assertEquals("Press", KeyFrame.parse(KeyFrame.press(MvpKey.DPAD_UP))!!.cmd)
        assertEquals("KEY_UP", KeyFrame.parse(KeyFrame.press(MvpKey.DPAD_UP))!!.dataOfCmd)
        assertEquals("Release", KeyFrame.parse(KeyFrame.release(MvpKey.DPAD_UP))!!.cmd)
        assertEquals("KEY_UP", KeyFrame.parse(KeyFrame.release(MvpKey.DPAD_UP))!!.dataOfCmd)
    }

    @Test
    fun mvpTableCoversFifteenKeysWithDistinctCodes() {
        val keys = MvpKey.entries
        assertEquals(15, keys.size)
        val codes = keys.map { it.dataOfCmd }
        assertEquals(codes.size, codes.toSet().size)
    }

    @Test
    fun oneGestureMapsToExactlyOneFrame() {
        MvpKey.entries.forEach { key ->
            val frame = KeyFrame.click(key)
            assertEquals(1, "\"DataOfCmd\"".toRegex().findAll(frame).count())
            assertEquals(key.dataOfCmd, KeyFrame.parse(frame)!!.dataOfCmd)
        }
    }

    @Test
    fun powerIsTheDistinctDiscretePowerKey() {
        assertEquals("KEY_POWER", MvpKey.POWER.dataOfCmd)
        assertEquals("power", MvpKey.POWER.group)
        assertEquals(MvpKey.POWER, MvpKey.fromDataOfCmd("KEY_POWER"))
    }

    @Test
    fun byNameResolvesScreenLabels() {
        assertEquals(MvpKey.DPAD_UP, MvpKey.byName("UP"))
        assertEquals(MvpKey.VOL_DOWN, MvpKey.byName("VOL_DOWN"))
        assertEquals(MvpKey.OK, MvpKey.byName("ok"))
        assertEquals(MvpKey.POWER, MvpKey.byName("POWER"))
        assertNull(MvpKey.byName("CHANNEL_SURF"))
    }

    @Test
    fun foreignFramesAndCodesAreRejected() {
        assertNull(KeyFrame.parse("""{"method":"ms.channel.emit","params":{}}"""))
        assertNull(KeyFrame.parse("not json"))
        assertNull(KeyFrame.parse(KeyFrame.frame("Click", "KEY_NOPE").replace("SendRemoteKey", "Other")))
        assertNull(MvpKey.fromDataOfCmd("KEY_NOPE"))
    }

    @Test
    fun everyGroupIsRepresented() {
        val groups = MvpKey.entries.map { it.group }.toSet()
        assertTrue(groups.containsAll(setOf("nav", "system", "volume", "transport", "power")))
    }

    @Test
    fun onlyDpadDirectionsRepeat() {
        MvpKey.entries.forEach { key ->
            val expected = key in setOf(MvpKey.DPAD_UP, MvpKey.DPAD_DOWN, MvpKey.DPAD_LEFT, MvpKey.DPAD_RIGHT)
            assertEquals("repeatable mismatch for ${key.name}", expected, KeyFrame.isRepeatable(key))
        }
    }
}
