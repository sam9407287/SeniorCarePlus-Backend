package com.seniorcareplus.database

import org.jetbrains.exposed.dao.id.LongIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime

// 用戶表
object Users : LongIdTable("users") {
    val username = varchar("username", 50).uniqueIndex()
    val email = varchar("email", 100).uniqueIndex()
    val passwordHash = varchar("password_hash", 255)
    val fullName = varchar("full_name", 100)
    val phoneNumber = varchar("phone_number", 20).nullable()
    val role = varchar("role", 20)
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
    val lastLoginAt = datetime("last_login_at").nullable()
    val profileImageUrl = varchar("profile_image_url", 500).nullable()
}

// 緊急聯絡人表
object EmergencyContacts : LongIdTable("emergency_contacts") {
    val userId = reference("user_id", Users)
    val name = varchar("name", 100)
    val phoneNumber = varchar("phone_number", 20)
    val relationship = varchar("relationship", 50)
    val isPrimary = bool("is_primary").default(false)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
}

// 用戶設備綁定表
object UserDeviceBindings : LongIdTable("user_device_bindings") {
    val userId = reference("user_id", Users)
    val deviceId = varchar("device_id", 100)
    val deviceType = varchar("device_type", 50)
    val deviceName = varchar("device_name", 100).nullable()
    val isActive = bool("is_active").default(true)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    
    init {
        uniqueIndex(userId, deviceId)
    }
}

// 用戶偏好設置表
object UserPreferences : LongIdTable("user_preferences") {
    val userId = reference("user_id", Users).uniqueIndex()
    val language = varchar("language", 10).default("zh-TW")
    val timezone = varchar("timezone", 50).default("Asia/Taipei")
    val notificationEnabled = bool("notification_enabled").default(true)
    val emailNotification = bool("email_notification").default(true)
    val smsNotification = bool("sms_notification").default(false)
    val heartRateMin = integer("heart_rate_min").default(60)
    val heartRateMax = integer("heart_rate_max").default(100)
    val temperatureMin = double("temperature_min").default(36.0)
    val temperatureMax = double("temperature_max").default(37.5)
    val bloodPressureSystolicMax = integer("blood_pressure_systolic_max").default(140)
    val bloodPressureDiastolicMax = integer("blood_pressure_diastolic_max").default(90)
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

// 用戶會話表（用於JWT token管理）
object UserSessions : LongIdTable("user_sessions") {
    val userId = reference("user_id", Users)
    val token = varchar("token", 500).uniqueIndex()
    val refreshToken = varchar("refresh_token", 500).uniqueIndex()
    val expiresAt = datetime("expires_at")
    val refreshExpiresAt = datetime("refresh_expires_at")
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val isRevoked = bool("is_revoked").default(false)
}

// 用戶操作日誌表
object UserActivityLogs : LongIdTable("user_activity_logs") {
    val userId = reference("user_id", Users)
    val action = varchar("action", 100)
    val description = text("description").nullable()
    val ipAddress = varchar("ip_address", 45).nullable()
    val userAgent = varchar("user_agent", 500).nullable()
    val metadata = text("metadata").nullable() // JSON格式的額外信息
    val createdAt = datetime("created_at").default(LocalDateTime.now())
} 