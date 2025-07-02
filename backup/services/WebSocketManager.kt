package com.seniorcareplus.services

import io.ktor.websocket.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * WebSocket管理器 - 單例模式
 * 負責管理所有WebSocket連接和消息廣播
 */
class WebSocketManager private constructor() {
    private val logger = LoggerFactory.getLogger(WebSocketManager::class.java)
    
    // 不同類型的WebSocket連接池
    private val healthConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val locationConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    private val alertConnections = ConcurrentHashMap<String, DefaultWebSocketSession>()
    
    companion object {
        @Volatile
        private var INSTANCE: WebSocketManager? = null
        
        fun getInstance(): WebSocketManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: WebSocketManager().also { INSTANCE = it }
            }
        }
    }
    
    /**
     * 添加健康數據WebSocket連接
     */
    fun addHealthConnection(sessionId: String, session: DefaultWebSocketSession) {
        healthConnections[sessionId] = session
        logger.info("新增健康數據WebSocket連接: $sessionId, 總連接數: ${healthConnections.size}")
    }
    
    /**
     * 移除健康數據WebSocket連接
     */
    fun removeHealthConnection(sessionId: String) {
        healthConnections.remove(sessionId)
        logger.info("移除健康數據WebSocket連接: $sessionId, 剩餘連接數: ${healthConnections.size}")
    }
    
    /**
     * 添加位置數據WebSocket連接
     */
    fun addLocationConnection(sessionId: String, session: DefaultWebSocketSession) {
        locationConnections[sessionId] = session
        logger.info("新增位置數據WebSocket連接: $sessionId, 總連接數: ${locationConnections.size}")
    }
    
    /**
     * 移除位置數據WebSocket連接
     */
    fun removeLocationConnection(sessionId: String) {
        locationConnections.remove(sessionId)
        logger.info("移除位置數據WebSocket連接: $sessionId, 剩餘連接數: ${locationConnections.size}")
    }
    
    /**
     * 添加警報WebSocket連接
     */
    fun addAlertConnection(sessionId: String, session: DefaultWebSocketSession) {
        alertConnections[sessionId] = session
        logger.info("新增警報WebSocket連接: $sessionId, 總連接數: ${alertConnections.size}")
    }
    
    /**
     * 移除警報WebSocket連接
     */
    fun removeAlertConnection(sessionId: String) {
        alertConnections.remove(sessionId)
        logger.info("移除警報WebSocket連接: $sessionId, 剩餘連接數: ${alertConnections.size}")
    }
    
    /**
     * 廣播健康數據到所有健康數據客戶端
     */
    suspend fun broadcastToHealthClients(message: String) {
        val deadConnections = mutableListOf<String>()
        
        healthConnections.forEach { (sessionId, session) ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                logger.warn("發送健康數據到客戶端失敗: $sessionId", e)
                deadConnections.add(sessionId)
            }
        }
        
        // 清理失效連接
        deadConnections.forEach { sessionId ->
            removeHealthConnection(sessionId)
        }
    }
    
    /**
     * 廣播位置數據到所有位置客戶端
     */
    suspend fun broadcastToLocationClients(message: String) {
        val deadConnections = mutableListOf<String>()
        
        locationConnections.forEach { (sessionId, session) ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                logger.warn("發送位置數據到客戶端失敗: $sessionId", e)
                deadConnections.add(sessionId)
            }
        }
        
        // 清理失效連接
        deadConnections.forEach { sessionId ->
            removeLocationConnection(sessionId)
        }
    }
    
    /**
     * 廣播警報到所有警報客戶端
     */
    suspend fun broadcastToAlertClients(message: String) {
        val deadConnections = mutableListOf<String>()
        
        alertConnections.forEach { (sessionId, session) ->
            try {
                session.send(Frame.Text(message))
            } catch (e: Exception) {
                logger.warn("發送警報到客戶端失敗: $sessionId", e)
                deadConnections.add(sessionId)
            }
        }
        
        // 清理失效連接
        deadConnections.forEach { sessionId ->
            removeAlertConnection(sessionId)
        }
    }
    
    /**
     * 廣播到所有類型的客戶端
     */
    suspend fun broadcastToAllClients(message: String) {
        GlobalScope.launch {
            broadcastToHealthClients(message)
            broadcastToLocationClients(message)
            broadcastToAlertClients(message)
        }
    }
    
    /**
     * 獲取連接統計信息
     */
    fun getConnectionStats(): Map<String, Int> {
        return mapOf(
            "health" to healthConnections.size,
            "location" to locationConnections.size,
            "alerts" to alertConnections.size,
            "total" to (healthConnections.size + locationConnections.size + alertConnections.size)
        )
    }
    
    /**
     * 清理所有連接
     */
    fun clearAllConnections() {
        healthConnections.clear()
        locationConnections.clear()
        alertConnections.clear()
        logger.info("已清理所有WebSocket連接")
    }
} 