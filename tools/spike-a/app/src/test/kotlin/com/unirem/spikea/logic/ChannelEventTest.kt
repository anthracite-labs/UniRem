package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ChannelEventTest {

    @Test
    fun parsesTizenEventAndToken() {
        val event = ChannelEvents.parse(
            """{"event":"ms.channel.connect","data":{"token":"abc123"}}""",
        )
        assertEquals("ms.channel.connect", event.name)
        assertEquals("abc123", event.token)
    }

    @Test
    fun acceptsMethodWhenFirmwareUsesIt() {
        val event = ChannelEvents.parse("""{"method":"ms.channel.unauthorized"}""")
        assertEquals("ms.channel.unauthorized", event.name)
        assertNull(event.token)
    }

    @Test
    fun blankOrMissingTokenIsNull() {
        assertNull(ChannelEvents.parse("""{"event":"ms.channel.ready"}""").token)
        assertNull(ChannelEvents.parse("""{"event":"ms.channel.connect","data":{"token":""}}""").token)
        assertNull(ChannelEvents.parse("""{"event":"ms.channel.connect","data":{}}""").token)
    }

    @Test
    fun invalidJsonIsNotAnEvent() {
        val event = ChannelEvents.parse("not json")
        assertEquals("", event.name)
        assertNull(event.token)
    }

    @Test
    fun tokenIsNeverPartOfTheName() {
        val event = ChannelEvents.parse("""{"event":"ms.channel.connect","data":{"token":"secret"}}""")
        assertEquals("ms.channel.connect", event.name)
    }
}
