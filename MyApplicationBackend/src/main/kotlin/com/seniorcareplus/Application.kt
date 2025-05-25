package com.seniorcareplus

import com.seniorcareplus.database.DatabaseConfig
import com.seniorcareplus.routes.healthRoutes
import com.seniorcareplus.routes.webSocketRoutes
import com.seniorcareplus.services.MqttService
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.plugins.defaultheaders.*
import io.ktor.server.plugins.statuspages.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.time.Duration

fun main() {
    val logger = LoggerFactory.getLogger("Application")
    
    // 初始化數據庫
    logger.info("正在初始化數據庫...")
    DatabaseConfig.init()
    
    // 啟動服務器
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    val logger = LoggerFactory.getLogger("ApplicationModule")
    
    // 配置JSON序列化
    install(ContentNegotiation) {
        json(Json {
            prettyPrint = true
            isLenient = true
            ignoreUnknownKeys = true
        })
    }
    
    // 配置CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        allowHeader("X-Requested-With")
        allowCredentials = true
        anyHost() // 在生產環境中應該限制特定域名
    }
    
    // 配置默認頭部
    install(DefaultHeaders) {
        header("X-Engine", "Ktor")
        header("X-Service", "SeniorCarePlus")
    }
    
    // 配置WebSocket
    install(WebSockets) {
        pingPeriod = Duration.ofSeconds(15)
        timeout = Duration.ofSeconds(15)
        maxFrameSize = Long.MAX_VALUE
        masking = false
    }
    
    // 配置狀態頁面（錯誤處理）
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            logger.error("未處理的異常", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                mapOf(
                    "success" to false,
                    "message" to "服務器內部錯誤",
                    "error" to cause.message,
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
        
        status(HttpStatusCode.NotFound) { call, status ->
            call.respond(
                status,
                mapOf(
                    "success" to false,
                    "message" to "請求的資源未找到",
                    "timestamp" to System.currentTimeMillis()
                )
            )
        }
    }
    
    // 配置路由
    routing {
        // 根路徑
        get("/") {
            call.respond(mapOf(
                "service" to "SeniorCarePlus Backend",
                "version" to "1.0.0",
                "status" to "running",
                "timestamp" to System.currentTimeMillis()
            ))
        }
        
        // 健康檢查
        get("/health") {
            call.respond(mapOf(
                "status" to "healthy",
                "service" to "SeniorCarePlus Backend",
                "timestamp" to System.currentTimeMillis()
            ))
        }
        
        // API路由
        healthRoutes()
        
        // WebSocket路由
        webSocketRoutes()
    }
    
    // 啟動MQTT服務
    launch {
        try {
            logger.info("正在啟動MQTT服務...")
            val mqttService = MqttService()
            mqttService.connect()
            logger.info("MQTT服務啟動成功")
        } catch (e: Exception) {
            logger.error("MQTT服務啟動失敗", e)
        }
    }
    
    // 應用程序關閉時的清理工作
    environment.monitor.subscribe(ApplicationStopping) {
        logger.info("應用程序正在關閉...")
        // 在這裡可以添加清理邏輯，如關閉MQTT連接等
    }
    
    logger.info("SeniorCarePlus Backend 服務已啟動，監聽端口: 8080")
}