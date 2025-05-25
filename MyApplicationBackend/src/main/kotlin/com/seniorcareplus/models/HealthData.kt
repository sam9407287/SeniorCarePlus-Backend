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
    val accuracy: Double = 0.0,
    val timestamp: Long = Instant.now().epochSecond,
    val area: String? = null,
    val deviceId: String? = null
)

/**
 * 患者基本信息模型
 */
@Serializable
data class Patient(
    val id: String,
    val name: String,
    val room: String,
    val deviceId: String? = null,
    val age: Int? = null,
    val gender: String? = null,
    val emergencyContact: String? = null
)

/**
 * 綜合健康數據模型
 */
@Serializable
data class HealthDataPacket(
    val patientId: String,
    val timestamp: Long = Instant.now().epochSecond,
    val heartRate: Int? = null,
    val temperature: Double? = null,
    val diaperStatus: String? = null,
    val location: LocationData? = null,
    val deviceId: String? = null
)

/**
 * API響應模型
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val message: String? = null,
    val timestamp: Long = Instant.now().epochSecond
)

/**
 * 患者健康統計模型
 */
@Serializable
data class HealthStats(
    val patientId: String,
    val avgHeartRate: Double? = null,
    val avgTemperature: Double? = null,
    val lastDiaperChange: Long? = null,
    val dataCount: Int = 0,
    val timeRange: String = "24h"
)