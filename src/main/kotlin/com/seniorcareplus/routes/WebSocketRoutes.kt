package com.seniorcareplus.routes

import com.seniorcareplus.services.DataStorageService
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
    val json = Json { ignoreUnknownKeys = true }
    
    // 存儲活躍的WebSocket連接
    val connections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    
    webSocket("/ws/health") {
        val sessionId = "session_${System.currentTimeMillis()}"
        connections[sessionId] = this
        logger.info("WebSocket連接建立: $sessionId")
        
        try {
            // 發送歡迎消息
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "welcome",
                "message" to "連接到SeniorCarePlus實時數據服務",
                "sessionId" to sessionId
            ))))
            
            // 定期發送數據更新
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
                        logger.error("發送WebSocket更新失敗", e)
                        break
                    }
                }
            }
            
            // 處理接收到的消息
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("收到WebSocket消息: $text")
                        
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
                                        // 發送特定患者的數據
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
                            logger.error("處理WebSocket消息失敗: $text", e)
                        }
                    }
                    is Frame.Close -> {
                        logger.info("WebSocket連接關閉: $sessionId")
                        break
                    }
                    else -> {
                        logger.debug("收到其他類型的WebSocket幀")
                    }
                }
            }
            
            updateJob.cancel()
            
        } catch (e: Exception) {
            logger.error("WebSocket處理出錯", e)
        } finally {
            connections.remove(sessionId)
            logger.info("WebSocket連接清理完成: $sessionId")
        }
    }
    
    // 警報專用WebSocket
    webSocket("/ws/alerts") {
        val sessionId = "alert_session_${System.currentTimeMillis()}"
        logger.info("警報WebSocket連接建立: $sessionId")
        
        try {
            send(Frame.Text(json.encodeToString(mapOf(
                "type" to "alert_service_connected",
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
            logger.info("警報WebSocket連接清理完成: $sessionId")
        }
    }
    
    // 廣播消息到所有連接的客戶端
    suspend fun broadcastToAll(message: String) {
        connections.values.forEach { session ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                logger.error("廣播消息失敗", e)
            }
        }
    }
}