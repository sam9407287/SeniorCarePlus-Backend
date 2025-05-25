package com.example.models

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * UWB定位数据模型
 */
@Serializable
data class LocationData(
    val deviceId: String,                  // 设备ID
    val x: Double,                         // X坐标
    val y: Double,                         // Y坐标
    val z: Double = 0.0,                   // Z坐标 (可选)
    val accuracy: Double = 0.0,            // 精度 (单位：米)
    val timestamp: Long = Instant.now().epochSecond,  // 时间戳
    val area: String? = null,              // 区域标识
    val batteryLevel: Int? = null,         // 电池电量 (百分比)
    val additionalInfo: Map<String, String>? = null   // 附加信息
)

/**
 * 区域定义模型
 */
@Serializable
data class Area(
    val id: String,                        // 区域ID
    val name: String,                      // 区域名称
    val description: String? = null,       // 区域描述
    val boundaryPoints: List<Point>,       // 边界点列表
    val level: Int = 0                     // 楼层级别
)

/**
 * 坐标点模型
 */
@Serializable
data class Point(
    val x: Double,
    val y: Double
) 