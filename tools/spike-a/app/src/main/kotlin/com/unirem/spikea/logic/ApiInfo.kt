package com.unirem.spikea.logic

import org.json.JSONObject

/**
 * Raw device fields from `GET <tv>:8001/api/v2/`, before identity resolution.
 * Pure mapping with no Android dependency, so the id/duid fallback and the
 * `TokenAuthSupport` casings are covered by JVM tests.
 */
data class ApiDevice(
    val name: String,
    val modelName: String,
    val model: String,
    val firmware: String,
    val tokenAuthSupport: Boolean,
    val wifiMac: String,
    val deviceId: String,
    val deviceIdField: String,
    val powerState: String,
    val reportedNetworkType: String,
)

object ApiInfo {

    fun parse(body: String): ApiDevice? {
        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            return null
        }
        val device = root.optJSONObject("device") ?: root
        val reportedId = device.optString("id")
        val reportedDuid = device.optString("duid")
        val deviceId = reportedId.ifBlank { reportedDuid }
        val deviceIdField = when {
            reportedId.isNotBlank() -> "device.id"
            reportedDuid.isNotBlank() -> "device.duid"
            else -> ""
        }
        val tokenAuth = when {
            device.has("TokenAuthSupport") -> device.optBoolean("TokenAuthSupport")
            device.has("tokenAuthSupport") -> device.optBoolean("tokenAuthSupport")
            else -> false
        }
        return ApiDevice(
            name = device.optString("name"),
            modelName = device.optString("modelName"),
            model = device.optString("model"),
            firmware = device.optString("softwareVersion"),
            tokenAuthSupport = tokenAuth,
            wifiMac = device.optString("wifiMac"),
            deviceId = deviceId,
            deviceIdField = deviceIdField,
            powerState = device.optString("PowerState"),
            reportedNetworkType = device.optString("networkType"),
        )
    }
}
