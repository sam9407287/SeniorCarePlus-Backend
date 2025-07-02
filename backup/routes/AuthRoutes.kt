package com.seniorcareplus.routes

import com.seniorcareplus.models.*
import com.seniorcareplus.services.AuthService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.auth.jwt.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("AuthRoutes")

fun Route.authRoutes(authService: AuthService) {
    route("/auth") {
        
        /**
         * 用戶註冊
         */
        post("/register") {
            try {
                val request = call.receive<RegisterRequest>()
                
                // 基本驗證
                if (request.username.isBlank() || request.email.isBlank() || request.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "用戶名、郵箱和密碼不能為空"))
                    return@post
                }
                
                if (request.password.length < 6) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "密碼長度至少6位"))
                    return@post
                }
                
                val result = authService.registerUser(request)
                
                result.fold(
                    onSuccess = { user ->
                        logger.info("用戶註冊成功: ${user.username}")
                        call.respond(HttpStatusCode.Created, mapOf(
                            "message" to "註冊成功",
                            "user" to user
                        ))
                    },
                    onFailure = { error ->
                        logger.error("用戶註冊失敗: ${error.message}")
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                    }
                )
            } catch (e: Exception) {
                logger.error("註冊請求處理失敗", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
            }
        }
        
        /**
         * 用戶登錄
         */
        post("/login") {
            try {
                val request = call.receive<LoginRequest>()
                
                if (request.username.isBlank() || request.password.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "用戶名和密碼不能為空"))
                    return@post
                }
                
                val clientIp = call.request.headers["X-Forwarded-For"] 
                    ?: call.request.headers["X-Real-IP"] 
                    ?: call.request.origin.remoteHost
                val userAgent = call.request.headers["User-Agent"]
                
                val result = authService.login(request, clientIp, userAgent)
                
                result.fold(
                    onSuccess = { loginResponse ->
                        logger.info("用戶登錄成功: ${request.username}")
                        call.respond(HttpStatusCode.OK, loginResponse)
                    },
                    onFailure = { error ->
                        logger.error("用戶登錄失敗: ${error.message}")
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to error.message))
                    }
                )
            } catch (e: Exception) {
                logger.error("登錄請求處理失敗", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
            }
        }
        
        /**
         * 刷新Token
         */
        post("/refresh") {
            try {
                val requestBody = call.receive<Map<String, String>>()
                val refreshToken = requestBody["refreshToken"]
                
                if (refreshToken.isNullOrBlank()) {
                    call.respond(HttpStatusCode.BadRequest, mapOf("error" to "刷新Token不能為空"))
                    return@post
                }
                
                val result = authService.refreshToken(refreshToken)
                
                result.fold(
                    onSuccess = { loginResponse ->
                        logger.info("Token刷新成功")
                        call.respond(HttpStatusCode.OK, loginResponse)
                    },
                    onFailure = { error ->
                        logger.error("Token刷新失敗: ${error.message}")
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to error.message))
                    }
                )
            } catch (e: Exception) {
                logger.error("Token刷新請求處理失敗", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
            }
        }
        
        /**
         * 驗證Token
         */
        get("/verify") {
            try {
                val authHeader = call.request.headers["Authorization"]
                if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                    call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "缺少認證Token"))
                    return@get
                }
                
                val token = authHeader.substring(7)
                val result = authService.verifyToken(token)
                
                result.fold(
                    onSuccess = { userId ->
                        call.respond(HttpStatusCode.OK, mapOf(
                            "valid" to true,
                            "userId" to userId
                        ))
                    },
                    onFailure = { error ->
                        call.respond(HttpStatusCode.Unauthorized, mapOf(
                            "valid" to false,
                            "error" to error.message
                        ))
                    }
                )
            } catch (e: Exception) {
                logger.error("Token驗證請求處理失敗", e)
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
            }
        }
    }
    
    /**
     * 需要認證的路由
     */
    authenticate("auth-jwt") {
        route("/auth") {
            
            /**
             * 用戶登出
             */
            post("/logout") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@post
                    }
                    
                    val authHeader = call.request.headers["Authorization"]
                    val token = authHeader?.substring(7) // 移除 "Bearer " 前綴
                    
                    if (token != null) {
                        val result = authService.logout(userId, token)
                        
                        result.fold(
                            onSuccess = { message ->
                                logger.info("用戶登出成功: $userId")
                                call.respond(HttpStatusCode.OK, mapOf("message" to message))
                            },
                            onFailure = { error ->
                                logger.error("用戶登出失敗: ${error.message}")
                                call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                            }
                        )
                    } else {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "無效的Token"))
                    }
                } catch (e: Exception) {
                    logger.error("登出請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 修改密碼
             */
            post("/change-password") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@post
                    }
                    
                    val request = call.receive<ChangePasswordRequest>()
                    
                    if (request.oldPassword.isBlank() || request.newPassword.isBlank()) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "舊密碼和新密碼不能為空"))
                        return@post
                    }
                    
                    if (request.newPassword.length < 6) {
                        call.respond(HttpStatusCode.BadRequest, mapOf("error" to "新密碼長度至少6位"))
                        return@post
                    }
                    
                    val result = authService.changePassword(userId, request)
                    
                    result.fold(
                        onSuccess = { message ->
                            logger.info("用戶修改密碼成功: $userId")
                            call.respond(HttpStatusCode.OK, mapOf("message" to message))
                        },
                        onFailure = { error ->
                            logger.error("用戶修改密碼失敗: ${error.message}")
                            call.respond(HttpStatusCode.BadRequest, mapOf("error" to error.message))
                        }
                    )
                } catch (e: Exception) {
                    logger.error("修改密碼請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
            
            /**
             * 獲取當前用戶信息
             */
            get("/me") {
                try {
                    val principal = call.principal<JWTPrincipal>()
                    val userId = principal?.payload?.getClaim("userId")?.asLong()
                    
                    if (userId == null) {
                        call.respond(HttpStatusCode.Unauthorized, mapOf("error" to "無效的Token"))
                        return@get
                    }
                    
                    call.respond(HttpStatusCode.OK, mapOf(
                        "userId" to userId,
                        "username" to principal.payload.getClaim("username")?.asString(),
                        "role" to principal.payload.getClaim("role")?.asString()
                    ))
                } catch (e: Exception) {
                    logger.error("獲取用戶信息請求處理失敗", e)
                    call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "服務器內部錯誤"))
                }
            }
        }
    }
} 