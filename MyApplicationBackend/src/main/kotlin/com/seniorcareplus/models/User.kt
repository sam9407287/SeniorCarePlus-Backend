package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

@Serializable
data class User(
    val id: Long? = null,
    val username: String,
    val email: String,
    val passwordHash: String? = null, // 不在序列化中包含密碼
    val fullName: String,
    val phoneNumber: String? = null,
    val role: UserRole,
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val lastLoginAt: String? = null,
    val profileImageUrl: String? = null,
    val emergencyContact: EmergencyContact? = null
)

@Serializable
data class UserProfile(
    val id: Long,
    val username: String,
    val email: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val role: UserRole,
    val isActive: Boolean,
    val createdAt: String? = null,
    val lastLoginAt: String? = null,
    val profileImageUrl: String? = null,
    val emergencyContact: EmergencyContact? = null
)

@Serializable
enum class UserRole(val displayName: String, val permissions: List<String>) {
    ELDER("長者", listOf("view_own_data", "update_profile")),
    CAREGIVER("照護者", listOf("view_assigned_data", "manage_alerts", "update_profile")),
    FAMILY_MEMBER("家庭成員", listOf("view_family_data", "receive_alerts", "update_profile")),
    MEDICAL_STAFF("醫護人員", listOf("view_patient_data", "manage_health_records", "prescribe_medications")),
    ADMIN("管理員", listOf("manage_users", "manage_devices", "view_all_data", "system_config"))
}

@Serializable
data class EmergencyContact(
    val name: String,
    val phoneNumber: String,
    val relationship: String,
    val isPrimary: Boolean = false
)

@Serializable
data class LoginRequest(
    val username: String,
    val password: String
)

@Serializable
data class LoginResponse(
    val success: Boolean,
    val message: String,
    val token: String? = null,
    val user: UserProfile? = null,
    val expiresAt: Long? = null
)

@Serializable
data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val fullName: String,
    val phoneNumber: String? = null,
    val role: UserRole = UserRole.ELDER
)

@Serializable
data class ChangePasswordRequest(
    val currentPassword: String,
    val newPassword: String
)

@Serializable
data class UpdateProfileRequest(
    val fullName: String? = null,
    val phoneNumber: String? = null,
    val profileImageUrl: String? = null,
    val emergencyContact: EmergencyContact? = null
)

@Serializable
data class UserDeviceBinding(
    val id: Long? = null,
    val userId: Long,
    val deviceId: String,
    val deviceType: String,
    val deviceName: String? = null,
    val isActive: Boolean = true,
    val createdAt: String? = null
)

@Serializable
data class UserPreferences(
    val id: Long? = null,
    val userId: Long,
    val language: String = "zh-TW",
    val timezone: String = "Asia/Taipei",
    val notificationEnabled: Boolean = true,
    val emailNotification: Boolean = true,
    val smsNotification: Boolean = false,
    val alertThresholds: AlertThresholds? = null
)

@Serializable
data class AlertThresholds(
    val heartRateMin: Int = 60,
    val heartRateMax: Int = 100,
    val temperatureMin: Double = 36.0,
    val temperatureMax: Double = 37.5,
    val bloodPressureSystolicMax: Int = 140,
    val bloodPressureDiastolicMax: Int = 90
) 