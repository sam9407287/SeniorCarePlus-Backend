package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// API響應包裝類
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis()
)

// 患者信息
@Serializable
data class Patient(
    val id: String,
    val name: String,
    val room: String,
    val deviceId: String,
    val age: Int,
    val gender: String,
    val emergencyContact: String,
    val createdAt: Long,
    val updatedAt: Long
)

// 健康記錄
@Serializable
data class HealthRecord(
    val id: Long,
    val patientId: String,
    val dataType: String,
    val value: JsonElement,
    val deviceId: String,
    val quality: String,
    val unit: String,
    val timestamp: Long,
    val createdAt: Long
)

// 位置記錄
@Serializable
data class LocationRecord(
    val id: Long,
    val patientId: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val accuracy: Double,
    val area: String,
    val deviceId: String,
    val timestamp: Long,
    val createdAt: Long
)

// 設備信息
@Serializable
data class Device(
    val deviceId: String,
    val type: String,
    val status: String,
    val lastSeen: Long,
    val batteryLevel: Int,
    val firmwareVersion: String,
    val createdAt: Long,
    val updatedAt: Long
)

// 警報信息
@Serializable
data class Alert(
    val id: Long,
    val patientId: String,
    val alertType: String,
    val title: String,
    val message: String,
    val severity: String,
    val status: String,
    val deviceId: String,
    val triggeredAt: Long,
    val acknowledgedAt: Long?,
    val resolvedAt: Long?,
    val createdAt: Long
)

// 提醒信息
@Serializable
data class Reminder(
    val id: Long,
    val patientId: String,
    val title: String,
    val description: String,
    val reminderType: String,
    val scheduledTime: Long,
    val isCompleted: Boolean,
    val completedAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)

// MQTT消息數據類
@Serializable
data class HeartRateData(
    val patientId: String,
    val deviceId: String,
    val heartRate: Int,
    val quality: String = "good",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class TemperatureData(
    val patientId: String,
    val deviceId: String,
    val temperature: Double,
    val unit: String = "celsius",
    val quality: String = "good",
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DiaperData(
    val patientId: String,
    val deviceId: String,
    val status: String, // "clean", "wet", "soiled"
    val moistureLevel: Int, // 0-100
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class LocationData(
    val patientId: String,
    val deviceId: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val accuracy: Double,
    val area: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Serializable
data class DeviceStatusData(
    val deviceId: String,
    val status: String, // "online", "offline", "low_battery"
    val batteryLevel: Int,
    val signalStrength: Int,
    val timestamp: Long = System.currentTimeMillis()
)

// 健康數據摘要
@Serializable
data class HealthDataSummary(
    val patientId: String,
    val patientName: String,
    val room: String,
    val latestHeartRate: Int?,
    val latestTemperature: Double?,
    val latestDiaperStatus: String?,
    val latestLocation: LocationData?,
    val deviceStatus: String?,
    val batteryLevel: Int?,
    val lastUpdate: Long,
    val alertCount: Int
)

// 統計數據
@Serializable
data class HealthStats(
    val totalPatients: Int,
    val activeDevices: Int,
    val activeAlerts: Int,
    val averageHeartRate: Double?,
    val averageTemperature: Double?,
    val lastUpdateTime: Long
)

// 警報統計
@Serializable
data class AlertStats(
    val total: Int,
    val critical: Int,
    val warning: Int,
    val info: Int,
    val resolved: Int,
    val pending: Int
)

// 設備統計
@Serializable
data class DeviceStats(
    val total: Int,
    val online: Int,
    val offline: Int,
    val lowBattery: Int,
    val averageBatteryLevel: Double
)