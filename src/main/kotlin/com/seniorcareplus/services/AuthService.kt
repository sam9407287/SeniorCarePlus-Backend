package com.seniorcareplus.services

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.*

class AuthService {
    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val jwtSecret = System.getenv("JWT_SECRET") ?: "senior-care-plus-secret-key-2024"
    private val jwtIssuer = "SeniorCarePlus"
    private val jwtAlgorithm = Algorithm.HMAC256(jwtSecret)
    private val jwtVerifier: JWTVerifier = JWT.require(jwtAlgorithm)
        .withIssuer(jwtIssuer)
        .build()
    
    companion object {
        private const val TOKEN_EXPIRY_HOURS = 24L
        private const val REFRESH_TOKEN_EXPIRY_DAYS = 30L
    }

    /**
     * 用戶註冊
     */
    suspend fun registerUser(request: RegisterRequest): Result<UserProfile> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            // 檢查用戶名和郵箱是否已存在
            val existingUser = Users.select { 
                (Users.username eq request.username) or (Users.email eq request.email) 
            }.singleOrNull()
            
            if (existingUser != null) {
                val existingUsername = existingUser[Users.username]
                val existingEmail = existingUser[Users.email]
                val message = when {
                    existingUsername == request.username -> "用戶名已存在"
                    existingEmail == request.email -> "郵箱已被註冊"
                    else -> "用戶已存在"
                }
                return@newSuspendedTransaction Result.failure(Exception(message))
            }

            // 創建新用戶
            val passwordHash = hashPassword(request.password)
            val now = LocalDateTime.now()
            
            val userId = Users.insertAndGetId {
                it[username] = request.username
                it[email] = request.email
                it[passwordHash] = passwordHash
                it[fullName] = request.fullName
                it[phoneNumber] = request.phoneNumber
                it[role] = request.role.name
                it[isActive] = true
                it[createdAt] = now
                it[updatedAt] = now
            }

            // 創建默認用戶偏好設置
            UserPreferences.insert {
                it[userId] = userId
                it[language] = "zh-TW"
                it[timezone] = "Asia/Taipei"
                it[notificationEnabled] = true
                it[emailNotification] = true
                it[smsNotification] = false
                it[createdAt] = now
                it[updatedAt] = now
            }

            // 返回用戶信息
            val userProfile = UserProfile(
                id = userId.value,
                username = request.username,
                email = request.email,
                fullName = request.fullName,
                phoneNumber = request.phoneNumber,
                role = request.role,
                isActive = true,
                createdAt = now.toString()
            )
            
            logUserActivity(userId.value, "USER_REGISTERED", "用戶註冊成功")
            Result.success(userProfile)
        }
    } catch (e: Exception) {
        logger.error("用戶註冊失敗", e)
        Result.failure(e)
    }

    /**
     * 用戶登錄
     */
    suspend fun login(request: LoginRequest, ipAddress: String? = null, userAgent: String? = null): Result<LoginResponse> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            // 查找用戶
            val userRow = Users.select { 
                (Users.username eq request.username) or (Users.email eq request.username) 
            }.singleOrNull()
            
            if (userRow == null) {
                return@newSuspendedTransaction Result.failure(Exception("用戶名或密碼錯誤"))
            }

            // 驗證密碼
            val storedHash = userRow[Users.passwordHash]
            if (!verifyPassword(request.password, storedHash)) {
                logUserActivity(userRow[Users.id].value, "LOGIN_FAILED", "密碼錯誤", ipAddress, userAgent)
                return@newSuspendedTransaction Result.failure(Exception("用戶名或密碼錯誤"))
            }

            // 檢查用戶是否被停用
            if (!userRow[Users.isActive]) {
                return@newSuspendedTransaction Result.failure(Exception("用戶帳號已被停用"))
            }

            val userId = userRow[Users.id].value
            val role = UserRole.valueOf(userRow[Users.role])
            
            // 生成JWT token
            val now = LocalDateTime.now()
            val expiresAt = now.plusHours(TOKEN_EXPIRY_HOURS)
            val refreshExpiresAt = now.plusDays(REFRESH_TOKEN_EXPIRY_DAYS)
            
            val token = generateJwtToken(userId, userRow[Users.username], role)
            val refreshToken = generateRefreshToken()

            // 保存會話信息
            UserSessions.insert {
                it[UserSessions.userId] = userId
                it[UserSessions.token] = token
                it[UserSessions.refreshToken] = refreshToken
                it[UserSessions.expiresAt] = expiresAt
                it[UserSessions.refreshExpiresAt] = refreshExpiresAt
                it[UserSessions.ipAddress] = ipAddress
                it[UserSessions.userAgent] = userAgent
                it[createdAt] = now
            }

            // 更新最後登錄時間
            Users.update({ Users.id eq userId }) {
                it[lastLoginAt] = now
            }

            // 獲取緊急聯絡人信息
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
                id = userId,
                username = userRow[Users.username],
                email = userRow[Users.email],
                fullName = userRow[Users.fullName],
                phoneNumber = userRow[Users.phoneNumber],
                role = role,
                isActive = userRow[Users.isActive],
                createdAt = userRow[Users.createdAt].toString(),
                lastLoginAt = now.toString(),
                profileImageUrl = userRow[Users.profileImageUrl],
                emergencyContact = emergencyContact
            )

            logUserActivity(userId, "LOGIN_SUCCESS", "用戶登錄成功", ipAddress, userAgent)
            
            Result.success(LoginResponse(
                success = true,
                message = "登錄成功",
                token = token,
                user = userProfile,
                expiresAt = expiresAt.toEpochSecond(ZoneOffset.UTC) * 1000
            ))
        }
    } catch (e: Exception) {
        logger.error("用戶登錄失敗", e)
        Result.failure(e)
    }

    /**
     * 驗證JWT token
     */
    fun verifyToken(token: String): Result<TokenClaims> = try {
        val decodedJWT = jwtVerifier.verify(token)
        val userId = decodedJWT.getClaim("userId").asLong()
        val username = decodedJWT.getClaim("username").asString()
        val role = UserRole.valueOf(decodedJWT.getClaim("role").asString())
        
        Result.success(TokenClaims(userId, username, role))
    } catch (e: JWTVerificationException) {
        logger.warn("Token驗證失敗: ${e.message}")
        Result.failure(e)
    }

    /**
     * 刷新token
     */
    suspend fun refreshToken(refreshToken: String): Result<LoginResponse> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val sessionRow = UserSessions.innerJoin(Users).select {
                (UserSessions.refreshToken eq refreshToken) and 
                (UserSessions.isRevoked eq false) and
                (UserSessions.refreshExpiresAt greater LocalDateTime.now())
            }.singleOrNull()
            
            if (sessionRow == null) {
                return@newSuspendedTransaction Result.failure(Exception("刷新token無效或已過期"))
            }

            val userId = sessionRow[Users.id].value
            val username = sessionRow[Users.username]
            val role = UserRole.valueOf(sessionRow[Users.role])
            
            // 生成新的token
            val now = LocalDateTime.now()
            val expiresAt = now.plusHours(TOKEN_EXPIRY_HOURS)
            val newToken = generateJwtToken(userId, username, role)
            val newRefreshToken = generateRefreshToken()
            val newRefreshExpiresAt = now.plusDays(REFRESH_TOKEN_EXPIRY_DAYS)

            // 撤銷舊的會話
            UserSessions.update({ UserSessions.refreshToken eq refreshToken }) {
                it[isRevoked] = true
            }

            // 創建新的會話
            UserSessions.insert {
                it[UserSessions.userId] = userId
                it[token] = newToken
                it[UserSessions.refreshToken] = newRefreshToken
                it[UserSessions.expiresAt] = expiresAt
                it[refreshExpiresAt] = newRefreshExpiresAt
                it[createdAt] = now
            }

            Result.success(LoginResponse(
                success = true,
                message = "Token刷新成功",
                token = newToken,
                expiresAt = expiresAt.toEpochSecond(ZoneOffset.UTC) * 1000
            ))
        }
    } catch (e: Exception) {
        logger.error("Token刷新失敗", e)
        Result.failure(e)
    }

    /**
     * 用戶登出
     */
    suspend fun logout(token: String): Result<String> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            UserSessions.update({ UserSessions.token eq token }) {
                it[isRevoked] = true
            }
            Result.success("登出成功")
        }
    } catch (e: Exception) {
        logger.error("用戶登出失敗", e)
        Result.failure(e)
    }

    /**
     * 更改密碼
     */
    suspend fun changePassword(userId: Long, request: ChangePasswordRequest): Result<String> = try {
        newSuspendedTransaction(Dispatchers.IO) {
            val userRow = Users.select { Users.id eq userId }.singleOrNull()
                ?: return@newSuspendedTransaction Result.failure(Exception("用戶不存在"))

            // 驗證當前密碼
            val currentHash = userRow[Users.passwordHash]
            if (!verifyPassword(request.currentPassword, currentHash)) {
                return@newSuspendedTransaction Result.failure(Exception("當前密碼錯誤"))
            }

            // 更新密碼
            val newPasswordHash = hashPassword(request.newPassword)
            Users.update({ Users.id eq userId }) {
                it[passwordHash] = newPasswordHash
                it[updatedAt] = LocalDateTime.now()
            }

            // 撤銷所有現有會話（強制重新登錄）
            UserSessions.update({ UserSessions.userId eq userId }) {
                it[isRevoked] = true
            }

            logUserActivity(userId, "PASSWORD_CHANGED", "用戶更改密碼")
            Result.success("密碼更改成功")
        }
    } catch (e: Exception) {
        logger.error("密碼更改失敗", e)
        Result.failure(e)
    }

    /**
     * 生成JWT token
     */
    private fun generateJwtToken(userId: Long, username: String, role: UserRole): String {
        val now = Date()
        val expiresAt = Date(now.time + TOKEN_EXPIRY_HOURS * 60 * 60 * 1000)
        
        return JWT.create()
            .withIssuer(jwtIssuer)
            .withSubject(username)
            .withClaim("userId", userId)
            .withClaim("username", username)
            .withClaim("role", role.name)
            .withIssuedAt(now)
            .withExpiresAt(expiresAt)
            .sign(jwtAlgorithm)
    }

    /**
     * 生成刷新token
     */
    private fun generateRefreshToken(): String {
        val random = SecureRandom()
        val bytes = ByteArray(32)
        random.nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /**
     * 密碼加密
     */
    private fun hashPassword(password: String): String {
        val salt = generateSalt()
        val hash = MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray())
        return "${Base64.getEncoder().encodeToString(hash)}:$salt"
    }

    /**
     * 密碼驗證
     */
    private fun verifyPassword(password: String, hash: String): Boolean {
        val parts = hash.split(":")
        if (parts.size != 2) return false
        
        val storedHash = parts[0]
        val salt = parts[1]
        
        val newHash = MessageDigest.getInstance("SHA-256")
            .digest((password + salt).toByteArray())
        val newHashString = Base64.getEncoder().encodeToString(newHash)
        
        return storedHash == newHashString
    }

    /**
     * 生成鹽值
     */
    private fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(16)
        random.nextBytes(salt)
        return Base64.getEncoder().encodeToString(salt)
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
                }
            }
        } catch (e: Exception) {
            logger.error("記錄用戶活動失敗", e)
        }
    }
}

/**
 * Token聲明
 */
data class TokenClaims(
    val userId: Long,
    val username: String,
    val role: UserRole
) 