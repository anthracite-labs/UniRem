package com.unirem.tv

import com.unirem.tv.internal.ApiInfoMapper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiInfoMapperTest {

    private fun body(deviceJson: String): String = "{\"device\":{$deviceJson}}"

    @Test
    fun idWinsOverDuid() {
        val api = ApiInfoMapper.parse(body("\"id\":\"DEV-1\",\"duid\":\"DUID-9\""))!!
        assertEquals("DEV-1", api.deviceId)
        assertEquals("device.id", api.deviceIdField)
    }

    @Test
    fun blankIdFallsBackToDuid() {
        val api = ApiInfoMapper.parse(body("\"id\":\"\",\"duid\":\"DUID-9\""))!!
        assertEquals("DUID-9", api.deviceId)
        assertEquals("device.duid", api.deviceIdField)
    }

    @Test
    fun neitherIdNorDuidLeavesIdentityBlank() {
        val api = ApiInfoMapper.parse(body("\"modelName\":\"QN90C\""))!!
        assertEquals("", api.deviceId)
        assertEquals("", api.deviceIdField)
    }

    @Test
    fun tokenAuthSupportUppercaseCasing() {
        assertTrue(ApiInfoMapper.parse(body("\"TokenAuthSupport\":true"))!!.tokenAuthSupport)
    }

    @Test
    fun tokenAuthSupportLowercaseCasing() {
        assertTrue(ApiInfoMapper.parse(body("\"tokenAuthSupport\":true"))!!.tokenAuthSupport)
    }

    @Test
    fun tokenAuthDefaultsFalseWhenAbsent() {
        assertFalse(ApiInfoMapper.parse(body("\"modelName\":\"QN90C\""))!!.tokenAuthSupport)
    }

    @Test
    fun fieldsMapFromDeviceObject() {
        val api = ApiInfoMapper.parse(
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
        val api = ApiInfoMapper.parse("{\"id\":\"DEV-7\",\"modelName\":\"Q60B\"}")!!
        assertEquals("DEV-7", api.deviceId)
        assertEquals("device.id", api.deviceIdField)
    }

    @Test
    fun malformedJsonReturnsNull() {
        assertNull(ApiInfoMapper.parse("not json"))
        assertNull(ApiInfoMapper.parse(""))
    }

    @Test
    fun firmwareIsByteExact() {
        val api = ApiInfoMapper.parse(body("\"softwareVersion\":\"T-KTM2DEUC_1520.1\""))!!
        assertEquals("T-KTM2DEUC_1520.1", api.firmware)
    }
}
