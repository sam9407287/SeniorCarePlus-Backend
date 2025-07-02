package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

/**
 * 設備類型枚舉
 */
enum class DeviceType(val displayName: String) {
    HEART_RATE_MONITOR("心率監測器"),
    BLOOD_PRESSURE_MONITOR("血壓監測器"),
    TEMPERATURE_SENSOR("體溫感測器"),
    GLUCOSE_METER("血糖儀"),
    OXIMETER("血氧儀"),
    WEIGHT_SCALE("體重秤"),
    ACTIVITY_TRACKER("活動追蹤器"),
    EMERGENCY_BUTTON("緊急按鈕"),
    SMART_WATCH("智能手錶"),
    MEDICATION_DISPENSER("藥物分配器"),
    FALL_DETECTOR("跌倒檢測器"),
    SLEEP_MONITOR("睡眠監測器"),
    OTHER("其他設備")
}

/**
 * 設備狀態枚舉
 */
enum class DeviceStatus(val displayName: String) {
    ONLINE("在線"),
    OFFLINE("離線"),
    MAINTENANCE("維護中"),
    ERROR("故障"),
    LOW_BATTERY("電量不足"),
    INACTIVE("未激活")
}

/**
 * 設備連接類型
 */
enum class ConnectionType(val displayName: String) {
    BLUETOOTH("藍牙"),
    WIFI("WiFi"),
    CELLULAR("蜂窩網絡"),
    USB("USB"),
    ZIGBEE("ZigBee"),
    LORA("LoRa"),
    OTHER("其他")
}

/**
 * 設備信息
 */
@Serializable
data class Device(
    val id: Long = 0,
    val deviceId: String,                    // 設備唯一標識符
    val deviceName: String,                  // 設備名稱
    val deviceType: DeviceType,              // 設備類型
    val manufacturer: String,                // 製造商
    val model: String,                       // 型號
    val firmwareVersion: String,             // 固件版本
    val hardwareVersion: String,             // 硬件版本
    val serialNumber: String,                // 序列號
    val macAddress: String? = null,          // MAC地址
    val connectionType: ConnectionType,       // 連接類型
    val status: DeviceStatus,                // 設備狀態
    val batteryLevel: Int? = null,           // 電池電量 (0-100)
    val signalStrength: Int? = null,         // 信號強度 (0-100)
    val lastHeartbeat: LocalDateTime? = null, // 最後心跳時間
    val location: String? = null,            // 設備位置
    val description: String? = null,         // 設備描述
    val isActive: Boolean = true,            // 是否激活
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 設備註冊請求
 */
@Serializable
data class DeviceRegistrationRequest(
    val deviceId: String,
    val deviceName: String,
    val deviceType: DeviceType,
    val manufacturer: String,
    val model: String,
    val firmwareVersion: String,
    val hardwareVersion: String,
    val serialNumber: String,
    val macAddress: String? = null,
    val connectionType: ConnectionType,
    val location: String? = null,
    val description: String? = null
)

/**
 * 設備更新請求
 */
@Serializable
data class DeviceUpdateRequest(
    val deviceName: String? = null,
    val status: DeviceStatus? = null,
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null,
    val location: String? = null,
    val description: String? = null,
    val firmwareVersion: String? = null,
    val isActive: Boolean? = null
)

/**
 * 設備狀態更新
 */
@Serializable
data class DeviceStatusUpdate(
    val deviceId: String,
    val status: DeviceStatus,
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val metadata: Map<String, String>? = null
)

/**
 * 設備心跳數據
 */
@Serializable
data class DeviceHeartbeat(
    val deviceId: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val batteryLevel: Int? = null,
    val signalStrength: Int? = null,
    val status: DeviceStatus = DeviceStatus.ONLINE,
    val metadata: Map<String, String>? = null
)

/**
 * 設備配置
 */
@Serializable
data class DeviceConfiguration(
    val id: Long = 0,
    val deviceId: String,
    val configKey: String,
    val configValue: String,
    val description: String? = null,
    val isEditable: Boolean = true,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 設備配置更新請求
 */
@Serializable
data class DeviceConfigurationRequest(
    val configurations: Map<String, String>
)

/**
 * 設備統計信息
 */
@Serializable
data class DeviceStatistics(
    val deviceId: String,
    val totalUptime: Long,                   // 總在線時間（分鐘）
    val totalDowntime: Long,                 // 總離線時間（分鐘）
    val averageBatteryLevel: Double,         // 平均電池電量
    val averageSignalStrength: Double,       // 平均信號強度
    val dataPointsCount: Long,               // 數據點總數
    val lastDataReceived: LocalDateTime?,    // 最後接收數據時間
    val errorCount: Long,                    // 錯誤次數
    val maintenanceCount: Long,              // 維護次數
    val calculatedAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 設備警報
 */
@Serializable
data class DeviceAlert(
    val id: Long = 0,
    val deviceId: String,
    val alertType: AlertType,
    val severity: AlertSeverity,
    val title: String,
    val message: String,
    val isResolved: Boolean = false,
    val resolvedAt: LocalDateTime? = null,
    val resolvedBy: Long? = null,           // 解決者用戶ID
    val metadata: Map<String, String>? = null,
    val createdAt: LocalDateTime = LocalDateTime.now()
)

/**
 * 警報類型
 */
enum class AlertType(val displayName: String) {
    DEVICE_OFFLINE("設備離線"),
    LOW_BATTERY("電量不足"),
    DEVICE_ERROR("設備故障"),
    SIGNAL_WEAK("信號弱"),
    MAINTENANCE_REQUIRED("需要維護"),
    FIRMWARE_UPDATE("固件更新"),
    CONFIGURATION_ERROR("配置錯誤"),
    DATA_ANOMALY("數據異常"),
    SECURITY_BREACH("安全漏洞"),
    OTHER("其他")
}

/**
 * 警報嚴重程度
 */
enum class AlertSeverity(val displayName: String, val level: Int) {
    LOW("低", 1),
    MEDIUM("中", 2),
    HIGH("高", 3),
    CRITICAL("緊急", 4)
}

/**
 * 設備列表響應
 */
@Serializable
data class DeviceListResponse(
    val devices: List<Device>,
    val total: Int,
    val page: Int,
    val pageSize: Int,
    val totalPages: Int
)

/**
 * 設備詳情響應
 */
@Serializable
data class DeviceDetailResponse(
    val device: Device,
    val configurations: List<DeviceConfiguration>,
    val statistics: DeviceStatistics?,
    val recentAlerts: List<DeviceAlert>,
    val boundUsers: List<UserProfile>
)

/**
 * 設備搜索過濾器
 */
@Serializable
data class DeviceSearchFilter(
    val deviceType: DeviceType? = null,
    val status: DeviceStatus? = null,
    val manufacturer: String? = null,
    val connectionType: ConnectionType? = null,
    val isActive: Boolean? = null,
    val batteryLevelMin: Int? = null,
    val batteryLevelMax: Int? = null,
    val signalStrengthMin: Int? = null,
    val signalStrengthMax: Int? = null,
    val location: String? = null,
    val searchTerm: String? = null
) 