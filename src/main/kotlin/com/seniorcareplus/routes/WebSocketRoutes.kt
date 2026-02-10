package com.seniorcareplus.routes

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * WebSocket相關路由 - 含連接管理
 */
fun Route.webSocketRoutes() {
    val logger = LoggerFactory.getLogger("WebSocketRoutes")
    
    // 管理活動的 WebSocket 連接
    val healthConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    val alertConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    val connectionCounter = AtomicInteger(0)
    
    webSocket("/ws/health") {
        val connectionId = "health_${connectionCounter.incrementAndGet()}"
        logger.info("健康數據WebSocket客戶端已連接 - ID: $connectionId")
        
        // 添加到連接池
        healthConnections[connectionId] = this
        
        try {
            // 發送歡迎消息
            send(Frame.Text("""
                {
                    "type": "welcome",
                    "message": "已連接到SeniorCarePlus健康數據流",
                    "connectionId": "$connectionId",
                    "timestamp": ${System.currentTimeMillis()}
                }
            """.trimIndent()))
            
            // 保持連接活躍
            for (frame in incoming) {
                // 處理客戶端消息（如果需要）
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("收到WebSocket消息 [$connectionId]: $text")
                    }
                    is Frame.Close -> {
                        logger.info("客戶端請求關閉連接: $connectionId")
                        break
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket錯誤 [$connectionId]: ${e.message}")
        } finally {
            // 從連接池移除
            healthConnections.remove(connectionId)
            logger.info("健康數據WebSocket客戶端已斷開 - ID: $connectionId (剩餘連接: ${healthConnections.size})")
        }
    }
    
    webSocket("/ws/alerts") {
        val connectionId = "alert_${connectionCounter.incrementAndGet()}"
        logger.info("警報WebSocket客戶端已連接 - ID: $connectionId")
        
        // 添加到連接池
        alertConnections[connectionId] = this
        
        try {
            // 發送歡迎消息
            send(Frame.Text("""
                {
                    "type": "welcome",
                    "message": "已連接到SeniorCarePlus警報流",
                    "connectionId": "$connectionId",
                    "timestamp": ${System.currentTimeMillis()}
                }
            """.trimIndent()))
            
            // 保持連接活躍
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val text = frame.readText()
                        logger.debug("收到WebSocket消息 [$connectionId]: $text")
                    }
                    is Frame.Close -> {
                        logger.info("客戶端請求關閉連接: $connectionId")
                        break
                    }
                    else -> {}
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket錯誤 [$connectionId]: ${e.message}")
        } finally {
            // 從連接池移除
            alertConnections.remove(connectionId)
            logger.info("警報WebSocket客戶端已斷開 - ID: $connectionId (剩餘連接: ${alertConnections.size})")
        }
    }
    
    // 添加狀態檢查端點
    get("/ws/status") {
        val status = mapOf(
            "healthConnections" to healthConnections.size,
            "alertConnections" to alertConnections.size,
            "totalConnections" to (healthConnections.size + alertConnections.size),
            "timestamp" to System.currentTimeMillis()
        )
        call.respond(status)
    }
} 