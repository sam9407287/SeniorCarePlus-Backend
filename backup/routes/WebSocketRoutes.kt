package com.seniorcareplus.routes

import com.seniorcareplus.services.DataStorageService
import com.seniorcareplus.services.WebSocketManager
import com.seniorcareplus.services.LocationService
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

fun Route.webSocketRoutes() {
    val logger = LoggerFactory.getLogger("WebSocketRoutes")
    val dataStorageService = DataStorageService()
    val locationService = LocationService()
    val webSocketManager = WebSocketManager.getInstance()
    val json = Json { ignoreUnknownKeys = true }
    
    // 健康數據WebSocket
    webSocket("/ws/health") {
        val sessionId = "health_${System.currentTimeMillis()}"
        webSocketManager.addHealthConnection(sessionId, this)
        logger.info("健康數據WebSocket連接建立: $sessionId")
        
        try {
            // 發送歡迎消息
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "welcome",
                "service" to "health",
                "message" to "連接到SeniorCarePlus健康數據服務",
                "sessionId" to sessionId
            ))))
            
            // 定期發送健康數據更新
            val updateJob = launch {
                while (isActive) {
                    try {
                        // 獲取所有患者的最新數據
                        val patients = dataStorageService.getAllPatients()
                        val healthUpdates = patients.mapNotNull { patient ->
                            dataStorageService.getLatestHealthData(patient.id)
                        }
                        
                        // 獲取活躍警報
                        val alerts = dataStorageService.getActiveAlerts()
                        
                        // 發送更新
                        val update = mapOf(
                            "type" to "health_update",
                            "data" to mapOf(
                                "patients" to healthUpdates,
                                "alerts" to alerts
                            ),
                            "timestamp" to System.currentTimeMillis()
                        )
                        
                        send(Frame.Text(json.encodeToString(update)))
                        
                        delay(5000) // 每5秒發送一次更新
                    } catch (e: Exception) {
                        logger.error("發送健康數據WebSocket更新失敗", e)
                        break
                    }
                }
            }
            
            // 處理接收到的消息
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("收到健康數據WebSocket消息: $text")
                        
                        try {
                            val message = json.decodeFromString<Map<String, String>>(text)
                            when (message["type"]) {
                                "ping" -> {
                                    send(Frame.Text(json.encodeToString(mapOf(
                                        "type" to "pong",
                                        "timestamp" to System.currentTimeMillis()
                                    ))))
                                }
                                "subscribe_patient" -> {
                                    val patientId = message["patientId"]
                                    if (patientId != null) {
                                        val healthData = dataStorageService.getLatestHealthData(patientId)
                                        send(Frame.Text(json.encodeToString(mapOf(
                                            "type" to "patient_data",
                                            "patientId" to patientId,
                                            "data" to healthData
                                        ))))
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("處理健康數據WebSocket消息失敗: $text", e)
                        }
                    }
                    is Frame.Close -> {
                        logger.info("健康數據WebSocket連接關閉: $sessionId")
                        break
                    }
                    else -> {}
                }
            }
            
            updateJob.cancel()
            
        } catch (e: Exception) {
            logger.error("健康數據WebSocket處理出錯", e)
        } finally {
            webSocketManager.removeHealthConnection(sessionId)
            logger.info("健康數據WebSocket連接清理完成: $sessionId")
        }
    }

    // 位置數據WebSocket
    webSocket("/ws/location") {
        val sessionId = "location_${System.currentTimeMillis()}"
        webSocketManager.addLocationConnection(sessionId, this)
        logger.info("位置數據WebSocket連接建立: $sessionId")
        
        try {
            // 發送歡迎消息
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "welcome",
                "service" to "location",
                "message" to "連接到SeniorCarePlus位置追蹤服務",
                "sessionId" to sessionId
            ))))
            
            // 發送初始位置數據
            val initialData = mapOf(
                "type" to "initial_data",
                "data" to mapOf(
                    "devices" to locationService.getAllDeviceLocations(),
                    "gateways" to locationService.getAllGateways(),
                    "anchors" to locationService.getAllAnchors(),
                    "tags" to locationService.getAllTags()
                ),
                "timestamp" to System.currentTimeMillis()
            )
            send(Frame.Text(json.encodeToString(initialData)))
            
            // 定期發送位置狀態更新 (輕量級檢查)
            val statusJob = launch {
                while (isActive) {
                    try {
                        val statusUpdate = mapOf(
                            "type" to "status_update",
                            "data" to mapOf(
                                "activeDevices" to locationService.getAllDeviceLocations().size,
                                "onlineGateways" to locationService.getAllGateways().count { it.status == "online" },
                                "activeTags" to locationService.getAllTags().count { it.status == "active" }
                            ),
                            "timestamp" to System.currentTimeMillis()
                        )
                        
                        send(Frame.Text(json.encodeToString(statusUpdate)))
                        
                        delay(10000) // 每10秒發送一次狀態更新
                    } catch (e: Exception) {
                        logger.error("發送位置狀態更新失敗", e)
                        break
                    }
                }
            }
            
            // 處理接收到的消息
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("收到位置WebSocket消息: $text")
                        
                        try {
                            val message = json.decodeFromString<Map<String, String>>(text)
                            when (message["type"]) {
                                "ping" -> {
                                    send(Frame.Text(json.encodeToString(mapOf(
                                        "type" to "pong",
                                        "timestamp" to System.currentTimeMillis()
                                    ))))
                                }
                                "get_device_location" -> {
                                    val deviceId = message["deviceId"]
                                    if (deviceId != null) {
                                        val location = locationService.getDeviceLocation(deviceId)
                                        send(Frame.Text(json.encodeToString(mapOf(
                                            "type" to "device_location",
                                            "deviceId" to deviceId,
                                            "data" to location
                                        ))))
                                    }
                                }
                                "subscribe_device" -> {
                                    val deviceId = message["deviceId"]
                                    // 可以實現特定設備的訂閱邏輯
                                    send(Frame.Text(json.encodeToString(mapOf(
                                        "type" to "subscription_confirmed",
                                        "deviceId" to deviceId
                                    ))))
                                }
                            }
                        } catch (e: Exception) {
                            logger.error("處理位置WebSocket消息失敗: $text", e)
                        }
                    }
                    is Frame.Close -> {
                        logger.info("位置WebSocket連接關閉: $sessionId")
                        break
                    }
                    else -> {}
                }
            }
            
            statusJob.cancel()
            
        } catch (e: Exception) {
            logger.error("位置WebSocket處理出錯", e)
        } finally {
            webSocketManager.removeLocationConnection(sessionId)
            logger.info("位置WebSocket連接清理完成: $sessionId")
        }
    }
    
    // 警報專用WebSocket (更新為使用WebSocketManager)
    webSocket("/ws/alerts") {
        val sessionId = "alert_${System.currentTimeMillis()}"
        webSocketManager.addAlertConnection(sessionId, this)
        logger.info("警報WebSocket連接建立: $sessionId")
        
        try {
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "welcome",
                "service" to "alerts",
                "message" to "連接到警報服務",
                "sessionId" to sessionId
            ))))
            
            // 定期檢查新警報
            val alertJob = launch {
                while (isActive) {
                    try {
                        val alerts = dataStorageService.getActiveAlerts()
                        
                        send(Frame.Text(json.encodeToString(mapOf(
                            "type" to "alerts_update",
                            "alerts" to alerts,
                            "count" to alerts.size,
                            "timestamp" to System.currentTimeMillis()
                        ))))
                        
                        delay(3000) // 每3秒檢查一次警報
                    } catch (e: Exception) {
                        logger.error("發送警報更新失敗", e)
                        break
                    }
                }
            }
            
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("收到警報WebSocket消息: $text")
                        // 可以在這裡處理警報確認等操作
                    }
                    is Frame.Close -> {
                        logger.info("警報WebSocket連接關閉: $sessionId")
                        break
                    }
                    else -> {}
                }
            }
            
            alertJob.cancel()
            
        } catch (e: Exception) {
            logger.error("警報WebSocket處理出錯", e)
        } finally {
            webSocketManager.removeAlertConnection(sessionId)
            logger.info("警報WebSocket連接清理完成: $sessionId")
        }
    }
}