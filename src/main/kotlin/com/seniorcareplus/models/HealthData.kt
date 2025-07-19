package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import java.time.Instant
import kotlinx.serialization.SerialName

/**
 * 心率數據模型
 */
@Serializable
data class HeartRateData(
    val patientId: String,
    val heartRate: Int,
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String? = null,
    val quality: String = "good" // good, fair, poor
)

/**
 * 體溫數據模型
 */
@Serializable
data class TemperatureData(
    val patientId: String,
    val temperature: Double,
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String? = null,
    val unit: String = "celsius" // celsius, fahrenheit
)

/**
 * 尿布狀態數據模型
 */
@Serializable
data class DiaperData(
    val patientId: String,
    val status: String, // dry, wet, soiled
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String? = null,
    val moistureLevel: Int? = null // 0-100
)

/**
 * UWB位置數據（實際接收的格式）
 */
@Serializable
data class UWBLocationData(
    val content: String,
    @SerialName("gateway id") val gatewayId: Long,
    val node: String,
    val id: Int,
    @SerialName("id(Hex)") val idHex: String,
    val position: UWBPosition,
    val time: String,
    @SerialName("sf number") val sfNumber: Int,
    @SerialName("serial no") val serialNo: Int
) {
    /**
     * 轉換為標準LocationData格式
     */
    fun toLocationData(): LocationData {
        return LocationData(
            patientId = mapTagIdToPatientId(id),
            x = position.x,
            y = position.y,
            z = position.z,
            accuracy = position.quality.toDouble(),
            area = "UWB覆蓋區域",
            deviceId = "UWB_TAG_$id",
            timestamp = System.currentTimeMillis() / 1000
        )
    }
    
    /**
     * 將標籤ID映射到患者ID
     */
    private fun mapTagIdToPatientId(tagId: Int): String {
        return when (tagId) {
            1770 -> "device_001"  // 0x06EA
            13402 -> "device_002" // 0x345A
            else -> "device_unknown_$tagId"
        }
    }
}

@Serializable
data class UWBPosition(
    val x: Double,
    val y: Double,
    val z: Double,
    val quality: Int
)

/**
 * 位置數據
 */
@Serializable
data class LocationData(
    val patientId: String,
    val x: Double,
    val y: Double,
    val z: Double? = null,
    val accuracy: Double? = null,
    val area: String? = null,
    val deviceId: String? = null,
    val timestamp: Long = System.currentTimeMillis() / 1000
)

/**
 * 通用健康數據模型
 */
@Serializable
data class HealthDataMessage(
    val type: String, // heart_rate, temperature, diaper, location
    val patientId: String,
    val data: String, // JSON格式的具體數據
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String? = null
)

/**
 * 警報數據模型
 */
@Serializable
data class AlertData(
    val patientId: String,
    val alertType: String, // emergency, warning, info
    val title: String,
    val message: String,
    val severity: String = "medium", // high, medium, low
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String? = null
) 