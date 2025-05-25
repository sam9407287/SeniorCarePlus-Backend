package com.seniorcareplus.routes

import com.seniorcareplus.models.*
import com.seniorcareplus.services.UserService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("UserRoutes")

fun Route.userRoutes(userService: UserService) {
    authenticate("auth-jwt") {
        route("/users") {
            
            /**
             * 獲取當前用戶資料
             */
            get("/profile") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@get
                    }
                    
                    val result = userService.getUserProfile(userId)
                    
                    result.fold(
                        onSuccess = { userProfile ->
                            call.respond(HttpStatusCode.OK, userProfile)
                        },
                        onFailure = { error ->
                            logger.error("獲取用戶資料失敗: ${error.message}")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("獲取用戶資料請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 更新用戶資料
             */
            put("/profile") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@put
                    }
                    
                    val request = call.receive<UpdateProfileRequest>()
                    val result = userService.updateUserProfile(userId, request)
                    
                    result.fold(
                        onSuccess = { userProfile ->
                            logger.info("用戶資料更新成功: $userId")
                            call.respond(HttpStatusCode.OK, mapOf(
                                "message" to "用戶資料更新成功",
                                "user" to userProfile
                            ))
                        },
                        onFailure = { error ->
                            logger.error("用戶資料更新失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("更新用戶資料請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 獲取用戶偏好設置
             */
            get("/preferences") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@get
                    }
                    
                    val result = userService.getUserPreferences(userId)
                    
                    result.fold(
                        onSuccess = { preferences ->
                            call.respond(HttpStatusCode.OK, preferences)
                        },
                        onFailure = { error ->
                            logger.error("獲取用戶偏好設置失敗: ${error.message}")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("獲取用戶偏好設置請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 更新用戶偏好設置
             */
            put("/preferences") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@put
                    }
                    
                    val preferences = call.receive<UserPreferences>()
                    val result = userService.updateUserPreferences(userId, preferences)
                    
                    result.fold(
                        onSuccess = { updatedPreferences ->
                            logger.info("用戶偏好設置更新成功: $userId")
                            call.respond(HttpStatusCode.OK, mapOf(
                                "message" to "偏好設置更新成功",
                                "preferences" to updatedPreferences
                            ))
                        },
                        onFailure = { error ->
                            logger.error("用戶偏好設置更新失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("更新用戶偏好設置請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 綁定設備
             */
            post("/devices") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@post
                    }
                    
                    val deviceBinding = call.receive<UserDeviceBinding>()
                    val result = userService.bindDevice(userId, deviceBinding)
                    
                    result.fold(
                        onSuccess = { binding ->
                            logger.info("設備綁定成功: $userId - ${binding.deviceId}")
                            call.respond(HttpStatusCode.Created, mapOf(
                                "message" to "設備綁定成功",
                                "device" to binding
                            ))
                        },
                        onFailure = { error ->
                            logger.error("設備綁定失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("設備綁定請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 獲取用戶設備列表
             */
            get("/devices") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@get
                    }
                    
                    val result = userService.getUserDevices(userId)
                    
                    result.fold(
                        onSuccess = { devices ->
                            call.respond(HttpStatusCode.OK, mapOf(
                                "devices" to devices,
                                "count" to devices.size
                            ))
                        },
                        onFailure = { error ->
                            logger.error("獲取用戶設備列表失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("獲取用戶設備列表請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 解除設備綁定
             */
            delete("/devices/{deviceId}") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    val deviceId = call.parameters["deviceId"]
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@delete
                    }
                    
                    if (deviceId.isNullOrBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "設備ID不能為空"))
                        return@delete
                    }
                    
                    val result = userService.unbindDevice(userId, deviceId)
                    
                    result.fold(
                        onSuccess = { message ->
                            logger.info("設備解除綁定成功: $userId - $deviceId")
                            call.respond(HttpStatusCode.OK, mapOf("message" to message))
                        },
                        onFailure = { error ->
                            logger.error("設備解除綁定失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("設備解除綁定請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 獲取用戶活動記錄
             */
            get("/activity-logs") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@get
                    }
                    
                    val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 50
                    val result = userService.getUserActivityLogs(userId, limit)
                    
                    result.fold(
                        onSuccess = { logs ->
                            call.respond(HttpStatusCode.OK, mapOf(
                                "logs" to logs,
                                "count" to logs.size
                            ))
                        },
                        onFailure = { error ->
                            logger.error("獲取用戶活動記錄失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("獲取用戶活動記錄請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 管理員功能：獲取所有用戶列表
             */
            get("/all") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.payload?.getClaim("role")?.asString()
                    
                    // 檢查權限
                    if (userRole != "ADMIN" && userRole != "MEDICAL_STAFF") {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "權限不足"))
                        return@get
                    }
                    
                    val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                    val pageSize = call.request.queryParameters["pageSize"]?.toIntOrNull() ?: 20
                    
                    val result = userService.getAllUsers(page, pageSize)
                    
                    result.fold(
                        onSuccess = { users ->
                            call.respond(HttpStatusCode.OK, mapOf(
                                "users" to users,
                                "page" to page,
                                "pageSize" to pageSize,
                                "count" to users.size
                            ))
                        },
                        onFailure = { error ->
                            logger.error("獲取用戶列表失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("獲取用戶列表請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 管理員功能：啟用/停用用戶帳號
             */
            patch("/{userId}/status") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.payload?.getClaim("role")?.asString()
                    val targetUserId = call.parameters["userId"]?.toLongOrNull()
                    
                    // 檢查權限
                    if (userRole != "ADMIN") {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "權限不足"))
                        return@patch
                    }
                    
                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "無效的用戶ID"))
                        return@patch
                    }
                    
                    val requestBody = call.receive<Map<String, Boolean>>()
                    val isActive = requestBody["isActive"] ?: true
                    
                    val result = userService.toggleUserStatus(targetUserId, isActive)
                    
                    result.fold(
                        onSuccess = { message ->
                            logger.info("用戶狀態切換成功: $targetUserId - $isActive")
                            call.respond(HttpStatusCode.OK, mapOf("message" to message))
                        },
                        onFailure = { error ->
                            logger.error("用戶狀態切換失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("用戶狀態切換請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 管理員功能：獲取指定用戶資料
             */
            get("/{userId}/profile") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userRole = principal?.payload?.getClaim("role")?.asString()
                    val currentUserId = principal?.payload?.getClaim("userId")?.asLong()
                    val targetUserId = call.parameters["userId"]?.toLongOrNull()
                    
                    if (targetUserId == null) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "無效的用戶ID"))
                        return@get
                    }
                    
                    // 檢查權限：管理員、醫護人員或本人可以查看
                    if (userRole != "ADMIN" && userRole != "MEDICAL_STAFF" && currentUserId != targetUserId) {
                        call.respond(HttpStatusCode.Forbidden, mapOf("error" to "權限不足"))
                        return@get
                    }
                    
                    val result = userService.getUserProfile(targetUserId)
                    
                    result.fold(
                        onSuccess = { userProfile ->
                            call.respond(HttpStatusCode.OK, userProfile)
                        },
                        onFailure = { error ->
                            logger.error("獲取指定用戶資料失敗: ${error.message}")
                            call.respond(HttpStatusCode.NotFound, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("獲取指定用戶資料請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
        }
    }
} 