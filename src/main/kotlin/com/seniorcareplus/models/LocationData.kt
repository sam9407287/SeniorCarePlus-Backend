package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * 設備位置數據模型
 */
@Serializable
data class LocationData(
    val deviceId: String,
    val patientId: String? = null,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val floor: Int = 1,
    val batteryLevel: Int? = null,
    val signal_strength: Int? = null,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 位置歷史記錄
 */
@Serializable
data class LocationHistory(
    val id: String,
    val deviceId: String,
    val patientId: String? = null,
    val x: Double,
    val y: Double,
    val z: Double = 0.0,
    val floor: Int = 1,
    val batteryLevel: Int? = null,
    val signal_strength: Int? = null,
    val timestamp: Long,
    val createdAt: Long = System.currentTimeMillis()
)

/**
 * Gateway設備信息
 */
@Serializable
data class GatewayInfo(
    val gatewayId: String,
    val name: String,
    val status: String, // online, offline, error
    val ipAddress: String? = null,
    val lastSeen: Long,
    val connectedDevices: Int = 0
)

/**
 * UWB錨點設備信息
 */
@Serializable
data class AnchorDevice(
    val anchorId: String,
    val gatewayId: String,
    val x: Double,
    val y: Double,
    val z: Double,
    val status: String, // active, inactive, error
    val batteryLevel: Int? = null,
    val lastSeen: Long
)

/**
 * UWB Tag設備信息
 */
@Serializable
data class TagDevice(
    val tagId: String,
    val gatewayId: String,
    val patientId: String? = null,
    val currentLocation: LocationData? = null,
    val status: String, // active, inactive, lost, error
    val batteryLevel: Int? = null,
    val lastSeen: Long,
    val config: TagConfig? = null
)

/**
 * Tag設備配置
 */
@Serializable
data class TagConfig(
    val fw_update: Boolean = false,
    val led: Boolean = true,
    val ble: Boolean = true,
    val location_engine: Boolean = true,
    val responsive_mode: String = "normal", // normal, fast, slow
    val stationary_detect: Boolean = true,
    val nominal_update_rate: Int = 1000, // milliseconds
    val stationary_update_rate: Int = 5000 // milliseconds
)

/**
 * Anchor設備配置
 */
@Serializable
data class AnchorConfig(
    val fw_update: Boolean = false,
    val led: Boolean = true,
    val ble: Boolean = true,
    val initiator: Boolean = false,
    val x: Double,
    val y: Double,
    val z: Double
) 