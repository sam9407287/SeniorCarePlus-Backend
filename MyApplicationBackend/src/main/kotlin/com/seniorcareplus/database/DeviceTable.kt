package com.seniorcareplus.database

import com.seniorcareplus.models.*
import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.Table
import java.time.LocalDateTime

/**
 * 設備表
 */
object Devices : LongIdTable("devices") {
    val deviceId = varchar("device_id", 100).uniqueIndex()
    val deviceName = varchar("device_name", 200)
    val deviceType = enumerationByName("device_type", 50, DeviceType::class)
    val manufacturer = varchar("manufacturer", 100)
    val model = varchar("model", 100)
    val firmwareVersion = varchar("firmware_version", 50)
    val hardwareVersion = varchar("hardware_version", 50)
    val serialNumber = varchar("serial_number", 100).uniqueIndex()
    val macAddress = varchar("mac_address", 17).nullable()
    val connectionType = enumerationByName("connection_type", 20, ConnectionType::class)
    val status = enumerationByName("status", 20, DeviceStatus::class).default(DeviceStatus.INACTIVE)
    val batteryLevel = integer("battery_level").nullable()
    val signalStrength = integer("signal_strength").nullable()
    val lastHeartbeat = datetime("last_heartbeat").nullable()
    val location = varchar("location", 200).nullable()
    val description = text("description").nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

/**
 * 設備配置表
 */
object DeviceConfigurations : LongIdTable("device_configurations") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val configKey = varchar("config_key", 100)
    val configValue = text("config_value")
    val description = varchar("description", 500).nullable()
    val isEditable = bool("is_editable").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    init {
        uniqueIndex(deviceId, configKey)
    }
}

/**
 * 設備統計表
 */
object DeviceStatistics : Table("device_statistics") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val totalUptime = long("total_uptime").default(0)
    val totalDowntime = long("total_downtime").default(0)
    val averageBatteryLevel = double("average_battery_level").default(0.0)
    val averageSignalStrength = double("average_signal_strength").default(0.0)
    val dataPointsCount = long("data_points_count").default(0)
    val lastDataReceived = datetime("last_data_received").nullable()
    val errorCount = long("error_count").default(0)
    val maintenanceCount = long("maintenance_count").default(0)
    val calculatedAt = datetime("calculated_at").default(LocalDateTime.now())
    
    override val primaryKey = PrimaryKey(deviceId)
}

/**
 * 設備警報表
 */
object DeviceAlerts : LongIdTable("device_alerts") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val alertType = enumerationByName("alert_type", 50, AlertType::class)
    val severity = enumerationByName("severity", 20, AlertSeverity::class)
    val title = varchar("title", 200)
    val message = text("message")
    val isResolved = bool("is_resolved").default(false)
    val resolvedAt = datetime("resolved_at").nullable()
    val resolvedBy = long("resolved_by").nullable().references(Users.id)
    val metadata = text("metadata").nullable() // JSON格式存儲
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, isResolved)
        index(false, severity, isResolved)
        index(false, createdAt)
    }
}

/**
 * 設備心跳記錄表
 */
object DeviceHeartbeats : LongIdTable("device_heartbeats") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val timestamp = datetime("timestamp").default(LocalDateTime.now())
    val batteryLevel = integer("battery_level").nullable()
    val signalStrength = integer("signal_strength").nullable()
    val status = enumerationByName("status", 20, DeviceStatus::class)
    val metadata = text("metadata").nullable() // JSON格式存儲
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, timestamp)
        index(false, timestamp)
    }
}

/**
 * 設備狀態歷史表
 */
object DeviceStatusHistory : LongIdTable("device_status_history") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val previousStatus = enumerationByName("previous_status", 20, DeviceStatus::class)
    val newStatus = enumerationByName("new_status", 20, DeviceStatus::class)
    val reason = varchar("reason", 500).nullable()
    val changedBy = long("changed_by").nullable().references(Users.id)
    val metadata = text("metadata").nullable() // JSON格式存儲
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, createdAt)
        index(false, newStatus)
    }
}

/**
 * 設備維護記錄表
 */
object DeviceMaintenanceRecords : LongIdTable("device_maintenance_records") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val maintenanceType = varchar("maintenance_type", 50) // 維護類型：預防性、修復性、升級等
    val description = text("description")
    val performedBy = long("performed_by").references(Users.id)
    val startTime = datetime("start_time")
    val endTime = datetime("end_time").nullable()
    val status = varchar("status", 20).default("SCHEDULED") // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    val cost = decimal("cost", 10, 2).nullable()
    val notes = text("notes").nullable()
    val nextMaintenanceDate = datetime("next_maintenance_date").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, status)
        index(false, startTime)
        index(false, nextMaintenanceDate)
    }
}

/**
 * 設備固件更新記錄表
 */
object DeviceFirmwareUpdates : LongIdTable("device_firmware_updates") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val fromVersion = varchar("from_version", 50)
    val toVersion = varchar("to_version", 50)
    val updateStatus = varchar("update_status", 20) // PENDING, IN_PROGRESS, SUCCESS, FAILED, ROLLBACK
    val startTime = datetime("start_time")
    val endTime = datetime("end_time").nullable()
    val errorMessage = text("error_message").nullable()
    val initiatedBy = long("initiated_by").references(Users.id)
    val downloadUrl = varchar("download_url", 500).nullable()
    val checksum = varchar("checksum", 128).nullable()
    val fileSize = long("file_size").nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, updateStatus)
        index(false, startTime)
    }
}

/**
 * 設備數據傳輸記錄表
 */
object DeviceDataTransmissions : LongIdTable("device_data_transmissions") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val dataType = varchar("data_type", 50) // 數據類型：心率、血壓、體溫等
    val dataSize = long("data_size") // 數據大小（字節）
    val transmissionStatus = varchar("transmission_status", 20) // SUCCESS, FAILED, PARTIAL
    val errorMessage = text("error_message").nullable()
    val transmissionTime = datetime("transmission_time")
    val processingTime = long("processing_time").nullable() // 處理時間（毫秒）
    val metadata = text("metadata").nullable() // JSON格式存儲額外信息
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, transmissionTime)
        index(false, dataType, transmissionTime)
        index(false, transmissionStatus)
    }
}

/**
 * 設備位置歷史表
 */
object DeviceLocationHistory : LongIdTable("device_location_history") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val previousLocation = varchar("previous_location", 200).nullable()
    val newLocation = varchar("new_location", 200)
    val latitude = double("latitude").nullable()
    val longitude = double("longitude").nullable()
    val accuracy = double("accuracy").nullable() // GPS精度（米）
    val changedBy = long("changed_by").nullable().references(Users.id)
    val changeReason = varchar("change_reason", 200).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        index(false, deviceId, createdAt)
        index(false, latitude, longitude)
    }
}

/**
 * 設備權限表 - 控制哪些用戶可以訪問哪些設備
 */
object DevicePermissions : LongIdTable("device_permissions") {
    val deviceId = varchar("device_id", 100).references(Devices.deviceId)
    val userId = long("user_id").references(Users.id)
    val permissionType = varchar("permission_type", 20) // READ, WRITE, ADMIN, MONITOR
    val grantedBy = long("granted_by").references(Users.id)
    val grantedAt = datetime("granted_at").default(LocalDateTime.now())
    val expiresAt = datetime("expires_at").nullable()
    val isActive = bool("is_active").default(true)
    val notes = varchar("notes", 500).nullable()
    
    init {
        uniqueIndex(deviceId, userId, permissionType)
        index(false, userId, isActive)
        index(false, expiresAt)
    }
} 