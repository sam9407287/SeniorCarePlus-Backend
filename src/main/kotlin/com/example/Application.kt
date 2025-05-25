package com.example

import com.example.database.DatabaseConfig
import com.example.mqtt.MqttSender
import com.example.plugins.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main() {
    // 初始化数据库
    try {
        DatabaseConfig.init()
    } catch (e: Exception) {
        println("警告: 数据库初始化失败，但程序将继续运行: ${e.message}")
    }
    
    // 启动模拟MQTT发送器
    val mqttSender = MqttSender()
    try {
        mqttSender.connect()
        mqttSender.startSimulation()
    } catch (e: Exception) {
        println("警告: MQTT发送器启动失败，但程序将继续运行: ${e.message}")
    }
    
    // 启动Ktor服务器
    embeddedServer(Netty, port = 8080, host = "0.0.0.0", module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    // 配置Ktor插件
    configureSerialization()
    configureSockets()
    configureRouting()
    
    // 日志输出
    log.info("UWB定位后端服务启动成功，正在监听 0.0.0.0:8080")
} 