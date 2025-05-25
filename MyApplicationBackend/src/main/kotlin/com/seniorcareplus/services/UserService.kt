package com.seniorcareplus.services

import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class UserService {
    private val logger = LoggerFactory.getLogger(UserService::class.java)

    /**
     * 獲取用戶資料
     */
    suspend fun getUserProfile(userId: Long): Result<UserProfile> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val userRow = Users.select { Users.id eq userId }.singleOrNull()
                ?: return@newSuspendedTransaction Result.failure(Exception("用戶不存在"))

            // 獲取緊急聯絡人
            val emergencyContact = EmergencyContacts.select { 
                (EmergencyContacts.userId eq userId) and (EmergencyContacts.isPrimary eq true) 
            }.singleOrNull()?.let {
                EmergencyContact(
                    name = it[EmergencyContacts.name],
                    phoneNumber = it[EmergencyContacts.phoneNumber],
                    relationship = it[EmergencyContacts.relationship],
                    isPrimary = it[EmergencyContacts.isPrimary]
                )
            }

            val userProfile = UserProfile(
                id = userRow[Users.id].value,
                username = userRow[Users.username],
                email = userRow[Users.email],
                fullName = userRow[Users.fullName],
                phoneNumber = userRow[Users.phoneNumber],
                role = UserRole.valueOf(userRow[Users.role]),
                isActive = userRow[Users.isActive],
                createdAt = userRow[Users.createdAt].toString(),
                lastLoginAt = userRow[Users.lastLoginAt]?.toString(),
                profileImageUrl = userRow[Users.profileImageUrl],
                emergencyContact = emergencyContact
            )

            Result.success(userProfile)
        }
    } catch (e: Exception) {
        logger.error("獲取用戶資料失敗", e)
        Result.failure(e)
    }

    /**
     * 更新用戶資料
     */
    suspend fun updateUserProfile(userId: Long, request: UpdateProfileRequest): Result<UserProfile> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val userExists = Users.select { Users.id eq userId }.count() > 0
            if (!userExists) {
                return@newSuspendedTransaction Result.failure(Exception("用戶不存在"))
            }

            // 檢查郵箱是否被其他用戶使用
            if (request.email != null) {
                val emailExists = Users.select { 
                    (Users.email eq request.email) and (Users.id neq userId) 
                }.count() > 0
                if (emailExists) {
                    return@newSuspendedTransaction Result.failure(Exception("郵箱已被其他用戶使用"))
                }
            }

            // 更新用戶信息
            Users.update({ Users.id eq userId }) {
                request.fullName?.let { fullName -> it[Users.fullName] = fullName }
                request.email?.let { email -> it[Users.email] = email }
                request.phoneNumber?.let { phoneNumber -> it[Users.phoneNumber] = phoneNumber }
                request.profileImageUrl?.let { profileImageUrl -> it[Users.profileImageUrl] = profileImageUrl }
                it[updatedAt] = LocalDateTime.now()
            }

            // 更新緊急聯絡人
            request.emergencyContact?.let { contact ->
                // 先刪除現有的主要緊急聯絡人
                EmergencyContacts.deleteWhere { 
                    (EmergencyContacts.userId eq userId) and (EmergencyContacts.isPrimary eq true) 
                }
                
                // 插入新的緊急聯絡人
                EmergencyContacts.insert {
                    it[EmergencyContacts.userId] = userId
                    it[name] = contact.name
                    it[phoneNumber] = contact.phoneNumber
                    it[relationship] = contact.relationship
                    it[isPrimary] = true
                    it[createdAt] = LocalDateTime.now()
                }
            }

            // 記錄活動
            logUserActivity(userId, "PROFILE_UPDATED", "用戶更新個人資料")

            // 返回更新後的用戶資料
            getUserProfile(userId)
        }
    } catch (e: Exception) {
        logger.error("更新用戶資料失敗", e)
        Result.failure(e)
    }

    /**
     * 獲取用戶偏好設置
     */
    suspend fun getUserPreferences(userId: Long): Result<UserPreferences> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val preferencesRow = UserPreferences.select { UserPreferences.userId eq userId }.singleOrNull()
                ?: return@newSuspendedTransaction Result.failure(Exception("用戶偏好設置不存在"))

            val preferences = UserPreferences(
                userId = preferencesRow[UserPreferences.userId].value,
                language = preferencesRow[UserPreferences.language],
                timezone = preferencesRow[UserPreferences.timezone],
                notificationEnabled = preferencesRow[UserPreferences.notificationEnabled],
                emailNotification = preferencesRow[UserPreferences.emailNotification],
                smsNotification = preferencesRow[UserPreferences.smsNotification],
                alertThresholds = AlertThresholds(
                    heartRateMin = preferencesRow[UserPreferences.heartRateMin],
                    heartRateMax = preferencesRow[UserPreferences.heartRateMax],
                    temperatureMin = preferencesRow[UserPreferences.temperatureMin],
                    temperatureMax = preferencesRow[UserPreferences.temperatureMax],
                    bloodPressureSystolicMax = preferencesRow[UserPreferences.bloodPressureSystolicMax],
                    bloodPressureDiastolicMax = preferencesRow[UserPreferences.bloodPressureDiastolicMax]
                )
            )

            Result.success(preferences)
        }
    } catch (e: Exception) {
        logger.error("獲取用戶偏好設置失敗", e)
        Result.failure(e)
    }

    /**
     * 更新用戶偏好設置
     */
    suspend fun updateUserPreferences(userId: Long, preferences: UserPreferences): Result<UserPreferences> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val userExists = Users.select { Users.id eq userId }.count() > 0
            if (!userExists) {
                return@newSuspendedTransaction Result.failure(Exception("用戶不存在"))
            }

            val now = LocalDateTime.now()
            
            // 檢查偏好設置是否存在
            val preferencesExists = UserPreferences.select { UserPreferences.userId eq userId }.count() > 0
            
            if (preferencesExists) {
                // 更新現有偏好設置
                UserPreferences.update({ UserPreferences.userId eq userId }) {
                    it[language] = preferences.language
                    it[timezone] = preferences.timezone
                    it[notificationEnabled] = preferences.notificationEnabled
                    it[emailNotification] = preferences.emailNotification
                    it[smsNotification] = preferences.smsNotification
                    it[heartRateMin] = preferences.alertThresholds.heartRateMin
                    it[heartRateMax] = preferences.alertThresholds.heartRateMax
                    it[temperatureMin] = preferences.alertThresholds.temperatureMin
                    it[temperatureMax] = preferences.alertThresholds.temperatureMax
                    it[bloodPressureSystolicMax] = preferences.alertThresholds.bloodPressureSystolicMax
                    it[bloodPressureDiastolicMax] = preferences.alertThresholds.bloodPressureDiastolicMax
                    it[updatedAt] = now
                }
            } else {
                // 創建新的偏好設置
                UserPreferences.insert {
                    it[userId] = userId
                    it[language] = preferences.language
                    it[timezone] = preferences.timezone
                    it[notificationEnabled] = preferences.notificationEnabled
                    it[emailNotification] = preferences.emailNotification
                    it[smsNotification] = preferences.smsNotification
                    it[heartRateMin] = preferences.alertThresholds.heartRateMin
                    it[heartRateMax] = preferences.alertThresholds.heartRateMax
                    it[temperatureMin] = preferences.alertThresholds.temperatureMin
                    it[temperatureMax] = preferences.alertThresholds.temperatureMax
                    it[bloodPressureSystolicMax] = preferences.alertThresholds.bloodPressureSystolicMax
                    it[bloodPressureDiastolicMax] = preferences.alertThresholds.bloodPressureDiastolicMax
                    it[createdAt] = now
                    it[updatedAt] = now
                }
            }

            logUserActivity(userId, "PREFERENCES_UPDATED", "用戶更新偏好設置")
            Result.success(preferences)
        }
    } catch (e: Exception) {
        logger.error("更新用戶偏好設置失敗", e)
        Result.failure(e)
    }

    /**
     * 綁定設備
     */
    suspend fun bindDevice(userId: Long, deviceBinding: UserDeviceBinding): Result<UserDeviceBinding> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val userExists = Users.select { Users.id eq userId }.count() > 0
            if (!userExists) {
                return@newSuspendedTransaction Result.failure(Exception("用戶不存在"))
            }

            // 檢查設備是否已被綁定
            val existingBinding = UserDeviceBindings.select { 
                (UserDeviceBindings.userId eq userId) and (UserDeviceBindings.deviceId eq deviceBinding.deviceId) 
            }.singleOrNull()

            val now = LocalDateTime.now()

            if (existingBinding != null) {
                // 更新現有綁定
                UserDeviceBindings.update({ 
                    (UserDeviceBindings.userId eq userId) and (UserDeviceBindings.deviceId eq deviceBinding.deviceId) 
                }) {
                    it[deviceType] = deviceBinding.deviceType
                    it[deviceName] = deviceBinding.deviceName
                    it[isActive] = deviceBinding.isActive
                }
            } else {
                // 創建新綁定
                UserDeviceBindings.insert {
                    it[userId] = userId
                    it[deviceId] = deviceBinding.deviceId
                    it[deviceType] = deviceBinding.deviceType
                    it[deviceName] = deviceBinding.deviceName
                    it[isActive] = deviceBinding.isActive
                    it[createdAt] = now
                }
            }

            logUserActivity(userId, "DEVICE_BOUND", "用戶綁定設備: ${deviceBinding.deviceName}")
            Result.success(deviceBinding)
        }
    } catch (e: Exception) {
        logger.error("綁定設備失敗", e)
        Result.failure(e)
    }

    /**
     * 解除設備綁定
     */
    suspend fun unbindDevice(userId: Long, deviceId: String): Result<String> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val deletedCount = UserDeviceBindings.deleteWhere { 
                (UserDeviceBindings.userId eq userId) and (UserDeviceBindings.deviceId eq deviceId) 
            }

            if (deletedCount == 0) {
                return@newSuspendedTransaction Result.failure(Exception("設備綁定不存在"))
            }

            logUserActivity(userId, "DEVICE_UNBOUND", "用戶解除設備綁定: $deviceId")
            Result.success("設備解除綁定成功")
        }
    } catch (e: Exception) {
        logger.error("解除設備綁定失敗", e)
        Result.failure(e)
    }

    /**
     * 獲取用戶綁定的設備列表
     */
    suspend fun getUserDevices(userId: Long): Result<List<UserDeviceBinding>> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val devices = UserDeviceBindings.select { UserDeviceBindings.userId eq userId }
                .map { row ->
                    UserDeviceBinding(
                        deviceId = row[UserDeviceBindings.deviceId],
                        deviceType = row[UserDeviceBindings.deviceType],
                        deviceName = row[UserDeviceBindings.deviceName],
                        isActive = row[UserDeviceBindings.isActive]
                    )
                }

            Result.success(devices)
        }
    } catch (e: Exception) {
        logger.error("獲取用戶設備列表失敗", e)
        Result.failure(e)
    }

    /**
     * 獲取用戶活動記錄
     */
    suspend fun getUserActivityLogs(userId: Long, limit: Int = 50): Result<List<UserActivityLog>> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val logs = UserActivityLogs.select { UserActivityLogs.userId eq userId }
                .orderBy(UserActivityLogs.createdAt, SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    UserActivityLog(
                        id = row[UserActivityLogs.id].value,
                        userId = row[UserActivityLogs.userId].value,
                        action = row[UserActivityLogs.action],
                        description = row[UserActivityLogs.description],
                        ipAddress = row[UserActivityLogs.ipAddress],
                        userAgent = row[UserActivityLogs.userAgent],
                        metadata = row[UserActivityLogs.metadata],
                        createdAt = row[UserActivityLogs.createdAt].toString()
                    )
                }

            Result.success(logs)
        }
    } catch (e: Exception) {
        logger.error("獲取用戶活動記錄失敗", e)
        Result.failure(e)
    }

    /**
     * 停用/啟用用戶帳號
     */
    suspend fun toggleUserStatus(userId: Long, isActive: Boolean): Result<String> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val updatedCount = Users.update({ Users.id eq userId }) {
                it[Users.isActive] = isActive
                it[updatedAt] = LocalDateTime.now()
            }

            if (updatedCount == 0) {
                return@newSuspendedTransaction Result.failure(Exception("用戶不存在"))
            }

            val action = if (isActive) "USER_ACTIVATED" else "USER_DEACTIVATED"
            val description = if (isActive) "用戶帳號已啟用" else "用戶帳號已停用"
            
            logUserActivity(userId, action, description)
            Result.success(description)
        }
    } catch (e: Exception) {
        logger.error("切換用戶狀態失敗", e)
        Result.failure(e)
    }

    /**
     * 獲取所有用戶列表（管理員功能）
     */
    suspend fun getAllUsers(page: Int = 1, pageSize: Int = 20): Result<List<UserProfile>> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val offset = (page - 1) * pageSize
            
            val users = Users.selectAll()
                .orderBy(Users.createdAt, SortOrder.DESC)
                .limit(pageSize, offset.toLong())
                .map { row ->
                    UserProfile(
                        id = row[Users.id].value,
                        username = row[Users.username],
                        email = row[Users.email],
                        fullName = row[Users.fullName],
                        phoneNumber = row[Users.phoneNumber],
                        role = UserRole.valueOf(row[Users.role]),
                        isActive = row[Users.isActive],
                        createdAt = row[Users.createdAt].toString(),
                        lastLoginAt = row[Users.lastLoginAt]?.toString(),
                        profileImageUrl = row[Users.profileImageUrl]
                    )
                }

            Result.success(users)
        }
    } catch (e: Exception) {
        logger.error("獲取用戶列表失敗", e)
        Result.failure(e)
    }

    /**
     * 記錄用戶活動
     */
    private suspend fun logUserActivity(
        userId: Long,
        action: String,
        description: String? = null,
        ipAddress: String? = null,
        userAgent: String? = null,
        metadata: Any? = null
    ) {
        try {
            newSuspendedTransaction(Dispatchers.IO) {
                UserActivityLogs.insert {
                    it[UserActivityLogs.userId] = userId
                    it[UserActivityLogs.action] = action
                    it[UserActivityLogs.description] = description
                    it[UserActivityLogs.ipAddress] = ipAddress
                    it[UserActivityLogs.userAgent] = userAgent
                    it[UserActivityLogs.metadata] = metadata?.let { Json.encodeToString(it) }
                    it[createdAt] = LocalDateTime.now()
                }
            }
        } catch (e: Exception) {
            logger.error("記錄用戶活動失敗", e)
        }
    }
}

/**
 * 用戶活動記錄
 */
data class UserActivityLog(
    val id: Long,
    val userId: Long,
    val action: String,
    val description: String?,
    val ipAddress: String?,
    val userAgent: String?,
    val metadata: String?,
    val createdAt: String
) 