package com.unirem.tv

import com.unirem.tv.internal.KeyFrameWriter
import com.unirem.core.api.command.RemoteCommand
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Wire-code mapping for the Story 1.11 D-pad: directions plus Enter as OK. */
class KeyFrameWriterTest {

    @Test
    fun directionsMapToKeyCodesAndOkMapsToEnter() {
        assertEquals("KEY_UP", KeyFrameWriter.dataOfCmd(RemoteCommand.Up))
        assertEquals("KEY_DOWN", KeyFrameWriter.dataOfCmd(RemoteCommand.Down))
        assertEquals("KEY_LEFT", KeyFrameWriter.dataOfCmd(RemoteCommand.Left))
        assertEquals("KEY_RIGHT", KeyFrameWriter.dataOfCmd(RemoteCommand.Right))
        assertEquals("KEY_ENTER", KeyFrameWriter.dataOfCmd(RemoteCommand.Enter))
    }

    @Test
    fun oneSubmitYieldsOneClickFrame() {
        for (command in RemoteCommand.entries) {
            val frame = KeyFrameWriter.frameFor(command)
            assertTrue(frame.contains("\"method\":\"ms.remote.control\""))
            assertTrue(frame.contains("\"Cmd\":\"Click\""))
            assertTrue(
                frame.contains("\"DataOfCmd\":\"" + KeyFrameWriter.dataOfCmd(command) + "\""),
            )
            assertTrue(frame.contains("\"Option\":\"false\""))
            assertTrue(frame.contains("\"TypeOfRemote\":\"SendRemoteKey\""))
            assertEquals(1, "\"DataOfCmd\"".toRegex().findAll(frame).count())
        }
    }

    @Test
    fun clickFramesNeverCarryPressOrRelease() {
        for (command in RemoteCommand.entries) {
            val frame = KeyFrameWriter.frameFor(command)
            assertFalse(frame.contains("\"Press\""))
            assertFalse(frame.contains("\"Release\""))
        }
    }

    @Test
    fun everyCommandHasADistinctCode() {
        val codes = RemoteCommand.entries.map { KeyFrameWriter.dataOfCmd(it) }
        assertEquals(codes.size, codes.toSet().size)
    }
}
