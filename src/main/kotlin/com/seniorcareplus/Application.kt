package com.seniorcareplus

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.cors.routing.*
import kotlinx.serialization.json.Json

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    configureModules()
}

fun Application.configureModules() {
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
        anyHost()
        allowHeader(HttpHeaders.ContentType)
        allowHeader(HttpHeaders.Authorization)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Options)
    }
    
    // 基本路由
    routing {
        get("/") {
            call.respondText(
                """
                {
                  "service": "SeniorCarePlus Backend",
                  "version": "0.0.1",
                  "status": "running",
                  "endpoints": {
                    "health": "/health",
                    "api_docs": "/api",
                    "location": "/api/location/*"
                  }
                }
                """.trimIndent(),
                ContentType.Application.Json
            )
        }
        
        get("/health") {
            call.respondText(
                """
                {
                  "status": "healthy",
                  "service": "SeniorCarePlus Backend",
                  "timestamp": ${System.currentTimeMillis()}
                }
                """.trimIndent(),
                ContentType.Application.Json
            )
        }
        
        // 基本位置API
        route("/api/location") {
            get("/devices") {
                call.respondText(
                    """
                    {
                      "success": true,
                      "message": "設備列表",
                      "data": []
                    }
                    """.trimIndent(),
                    ContentType.Application.Json
                )
            }
            
            get("/gateways") {
                call.respondText(
                    """
                    {
                      "success": true,
                      "message": "Gateway列表", 
                      "data": []
                    }
                    """.trimIndent(),
                    ContentType.Application.Json
                )
            }
            
            get("/anchors") {
                call.respondText(
                    """
                    {
                      "success": true,
                      "message": "Anchor設備列表",
                      "data": []
                    }
                    """.trimIndent(),
                    ContentType.Application.Json
                )
            }
            
            get("/tags") {
                call.respondText(
                    """
                    {
                      "success": true,
                      "message": "Tag設備列表",
                      "data": []
                    }
                    """.trimIndent(),
                    ContentType.Application.Json
                )
            }
        }
    }
    
    println("🚀 SeniorCarePlus Backend 簡化版已啟動！")
    println("📍 服務地址: http://localhost:8080")
    println("🔗 可用端點:")
    println("  - 服務狀態: http://localhost:8080/")
    println("  - 健康檢查: http://localhost:8080/health")
    println("  - 設備列表: http://localhost:8080/api/location/devices")
    println("  - Gateway列表: http://localhost:8080/api/location/gateways")
    println("  - Anchor列表: http://localhost:8080/api/location/anchors")  
    println("  - Tag列表: http://localhost:8080/api/location/tags")
    println("=" + "=".repeat(49))
}