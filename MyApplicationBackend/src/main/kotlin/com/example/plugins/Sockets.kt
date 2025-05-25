package com.example.plugins

import io.ktor.server.application.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import java.time.Duration
import kotlinx.coroutines.channels.consumeEach
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger("com.example.plugins.Sockets")

fun Application.configureSockets() {
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(30)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    routing {
        // 实时位置更新WebSocket端点
        webSocket("/location/updates") {
            try {
                // 客户端连接时的处理
                val remoteHost = call.request.local.remoteHost
                logger.info("WebSocket客户端已连接: $remoteHost")
                
                // 监听客户端消息
                incoming.consumeEach { frame ->
                    if (frame is Frame.Text) {
                        val text = frame.readText()
                        logger.info("收到客户端消息: $text")
                        
                        // 处理用户请求，例如订阅特定区域或设备的位置更新
                        // 这里将根据实际需求实现
                    }
                }
            } catch (e: Exception) {
                logger.error("WebSocket错误: ${e.localizedMessage}")
            } finally {
                val remoteHost = call.request.local.remoteHost
                logger.info("WebSocket客户端断开连接: $remoteHost")
            }
        }
    }
} 