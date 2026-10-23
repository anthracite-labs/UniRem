package com.unirem.spikea.logic

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChannelUrlTest {

    @Test
    fun urlIsAlwaysWssAndNeverFallsBackToWs() {
        val withoutToken = ChannelUrl.build("192.168.1.20")
        val withToken = ChannelUrl.build("192.168.1.20", "token-value")
        assertTrue(withoutToken.startsWith("wss://"))
        assertTrue(withToken.startsWith("wss://"))
        assertFalse(withoutToken.startsWith("ws://"))
        assertFalse(withToken.contains("ws://192"))
    }

    @Test
    fun tokenIsAttachedOnlyWhenProvided() {
        assertFalse(ChannelUrl.build("192.168.1.20").contains("token="))
        assertTrue(ChannelUrl.build("192.168.1.20", "token-value").contains("&token=token-value"))
    }

    @Test
    fun appNameIsBase64AndUrlEncoded() {
        val url = ChannelUrl.build("192.168.1.20")
        assertTrue(url.contains("?name="))
        assertTrue(url.contains("%3D"))
        assertTrue(url.contains(":8002/api/v2/channels/samsung.remote.control"))
    }
}
