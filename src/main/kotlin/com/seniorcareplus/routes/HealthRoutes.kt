package com.seniorcareplus.routes

import com.seniorcareplus.models.ApiResponse
import com.seniorcareplus.services.DataStorageService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

fun Route.healthRoutes() {
    val logger = LoggerFactory.getLogger("HealthRoutes")
    val dataStorageService = DataStorageService()
    
    route("/api/health") {
        
        // 獲取所有患者列表
        get("/patients") {
            try {
                val patients = dataStorageService.getAllPatients()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = patients,
                        message = "患者列表獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取患者列表失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取患者列表失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取特定患者的最新健康數據
        get("/patient/{patientId}") {
            try {
                val patientId = call.parameters["patientId"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Any>(
                            success = false,
                            data = null,
                            message = "患者ID不能為空",
                            timestamp = LocalDateTime.now()
                        )
                    )
                    return@get
                }
                
                val healthData = dataStorageService.getLatestHealthData(patientId)
                
                if (healthData != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = healthData,
                            message = "健康數據獲取成功",
                            timestamp = LocalDateTime.now()
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Any>(
                            success = false,
                            data = null,
                            message = "未找到患者數據",
                            timestamp = LocalDateTime.now()
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("獲取患者健康數據失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取健康數據失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取活躍警報
        get("/alerts") {
            try {
                val alerts = dataStorageService.getActiveAlerts()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = alerts,
                        message = "警報列表獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取警報列表失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取警報列表失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 健康檢查端點
        get("/status") {
            call.respond(
                HttpStatusCode.OK,
                ApiResponse(
                    success = true,
                    data = mapOf(
                        "status" to "healthy",
                        "timestamp" to LocalDateTime.now(),
                        "service" to "SeniorCarePlus Backend"
                    ),
                    message = "服務運行正常",
                    timestamp = LocalDateTime.now()
                )
            )
        }
        
        // 觸發數據清理
        post("/cleanup") {
            try {
                dataStorageService.cleanupOldData()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = mapOf("message" to "數據清理已完成"),
                        message = "數據清理成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("數據清理失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "數據清理失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
    }
}