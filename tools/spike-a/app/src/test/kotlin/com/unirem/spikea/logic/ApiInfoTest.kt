package com.unirem.spikea.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiInfoTest {

    private fun body(deviceJson: String): String = "{\"device\":{$deviceJson}}"

    @Test
    fun idWinsOverDuid() {
        val api = ApiInfo.parse(body("\"id\":\"DEV-1\",\"duid\":\"DUID-9\""))!!
        assertEquals("DEV-1", api.deviceId)
        assertEquals("device.id", api.deviceIdField)
    }

    @Test
    fun blankIdFallsBackToDuid() {
        val api = ApiInfo.parse(body("\"id\":\"\",\"duid\":\"DUID-9\""))!!
        assertEquals("DUID-9", api.deviceId)
        assertEquals("device.duid", api.deviceIdField)
    }

    @Test
    fun neitherIdNorDuidLeavesIdentityBlank() {
        val api = ApiInfo.parse(body("\"modelName\":\"QN90C\""))!!
        assertEquals("", api.deviceId)
        assertEquals("", api.deviceIdField)
    }

    @Test
    fun tokenAuthSupportUppercaseCasing() {
        assertTrue(ApiInfo.parse(body("\"TokenAuthSupport\":true"))!!.tokenAuthSupport)
    }

    @Test
    fun tokenAuthSupportLowercaseCasing() {
        assertTrue(ApiInfo.parse(body("\"tokenAuthSupport\":true"))!!.tokenAuthSupport)
    }

    @Test
    fun tokenAuthDefaultsFalseWhenAbsent() {
        assertFalse(ApiInfo.parse(body("\"modelName\":\"QN90C\""))!!.tokenAuthSupport)
    }

    @Test
    fun fieldsMapFromDeviceObject() {
        val api = ApiInfo.parse(
            body("\"name\":\"[TV] Samsung\",\"modelName\":\"QN90C\",\"model\":\"QE65QN90C\"," +
                "\"softwareVersion\":\"T-KTM2DEUC_1520.1\",\"wifiMac\":\"AA:BB:CC:DD:EE:01\"," +
                "\"PowerState\":\"on\",\"networkType\":\"wireless\""),
        )!!
        assertEquals("[TV] Samsung", api.name)
        assertEquals("QN90C", api.modelName)
        assertEquals("T-KTM2DEUC_1520.1", api.firmware)
        assertEquals("on", api.powerState)
        assertEquals("wireless", api.reportedNetworkType)
    }

    @Test
    fun rootLevelDeviceWithoutWrapper() {
        val api = ApiInfo.parse("{\"id\":\"DEV-7\",\"modelName\":\"Q60B\"}")!!
        assertEquals("DEV-7", api.deviceId)
        assertEquals("device.id", api.deviceIdField)
    }

    @Test
    fun malformedJsonReturnsNull() {
        assertNull(ApiInfo.parse("not json"))
    }
}
