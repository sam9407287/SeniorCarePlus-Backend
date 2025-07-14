package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import java.time.Instant

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
 * 位置數據模型
 */
@Serializable
data class LocationData(
    val patientId: String,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val accuracy: Double? = null,
    val area: String? = null,
    val timestamp: Long = Instant.now().epochSecond,
    val deviceId: String? = null
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