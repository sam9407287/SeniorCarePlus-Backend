package com.seniorcareplus.routes

import com.seniorcareplus.models.*
import com.seniorcareplus.services.LocationService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

fun Route.locationRoutes() {
    val logger = LoggerFactory.getLogger("LocationRoutes")
    val locationService = LocationService()
    
    route("/api/location") {
        
        // 獲取所有設備位置
        get("/devices") {
            try {
                val devices = locationService.getAllDeviceLocations()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = devices,
                        message = "設備位置列表獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取設備位置列表失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取設備位置失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取特定設備位置
        get("/device/{deviceId}") {
            try {
                val deviceId = call.parameters["deviceId"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Any>(
                            success = false,
                            data = null,
                            message = "設備ID不能為空",
                            timestamp = LocalDateTime.now()
                        )
                    )
                    return@get
                }
                
                val location = locationService.getDeviceLocation(deviceId)
                
                if (location != null) {
                    call.respond(
                        HttpStatusCode.OK,
                        ApiResponse(
                            success = true,
                            data = location,
                            message = "設備位置獲取成功",
                            timestamp = LocalDateTime.now()
                        )
                    )
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Any>(
                            success = false,
                            data = null,
                            message = "未找到設備位置數據",
                            timestamp = LocalDateTime.now()
                        )
                    )
                }
            } catch (e: Exception) {
                logger.error("獲取設備位置失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取設備位置失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 更新設備位置 (通常由MQTT自動調用)
        post("/update") {
            try {
                val locationData = call.receive<LocationData>()
                
                locationService.updateDeviceLocation(locationData)
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = locationData,
                        message = "位置更新成功",
                        timestamp = LocalDateTime.now()
                    )
                )
                
                logger.info("位置數據更新: 設備=${locationData.deviceId}, 位置=(${locationData.x}, ${locationData.y})")
                
            } catch (e: Exception) {
                logger.error("更新位置數據失敗", e)
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "位置更新失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取位置歷史記錄
        get("/history/{deviceId}") {
            try {
                val deviceId = call.parameters["deviceId"] ?: run {
                    call.respond(
                        HttpStatusCode.BadRequest,
                        ApiResponse<Any>(
                            success = false,
                            data = null,
                            message = "設備ID不能為空",
                            timestamp = LocalDateTime.now()
                        )
                    )
                    return@get
                }
                
                val from = call.request.queryParameters["from"]?.toLongOrNull()
                val to = call.request.queryParameters["to"]?.toLongOrNull()
                val limit = call.request.queryParameters["limit"]?.toIntOrNull() ?: 100
                
                val history = locationService.getLocationHistory(deviceId, from, to, limit)
                
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = history,
                        message = "位置歷史記錄獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取位置歷史記錄失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取位置歷史失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取Gateway列表
        get("/gateways") {
            try {
                val gateways = locationService.getAllGateways()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = gateways,
                        message = "Gateway列表獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取Gateway列表失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取Gateway列表失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取Anchor設備列表
        get("/anchors") {
            try {
                val anchors = locationService.getAllAnchors()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = anchors,
                        message = "Anchor設備列表獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取Anchor設備列表失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取Anchor設備列表失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
        
        // 獲取Tag設備列表
        get("/tags") {
            try {
                val tags = locationService.getAllTags()
                call.respond(
                    HttpStatusCode.OK,
                    ApiResponse(
                        success = true,
                        data = tags,
                        message = "Tag設備列表獲取成功",
                        timestamp = LocalDateTime.now()
                    )
                )
            } catch (e: Exception) {
                logger.error("獲取Tag設備列表失敗", e)
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Any>(
                        success = false,
                        data = null,
                        message = "獲取Tag設備列表失敗: ${e.message}",
                        timestamp = LocalDateTime.now()
                    )
                )
            }
        }
    }
} 