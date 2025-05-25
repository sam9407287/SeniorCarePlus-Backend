package com.example.plugins

import com.example.routes.configureLocationRoutes
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {
    // 配置CORS
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Get)
        allowMethod(HttpMethod.Post)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.ContentType)
        anyHost() // 生产环境应该限制为特定域名
    }
    
    routing {
        // 健康检查端点
        get("/health") {
            call.respond(HttpStatusCode.OK, mapOf("status" to "ok"))
        }
        
        // API版本信息
        get("/version") {
            call.respond(mapOf(
                "version" to "1.0.0",
                "name" to "MyApplication UWB Positioning Backend"
            ))
        }
        
        // 配置位置相关路由
        configureLocationRoutes()
    }
} 