package com.example.routes

import com.example.models.LocationData
import com.example.services.MqttService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.websocket.*
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

private val logger = LoggerFactory.getLogger("com.example.routes.LocationRoutes")

// 内存中的数据存储
private val deviceLocations = ConcurrentHashMap<String, LocationData>()
private val patientRecords = ConcurrentHashMap<String, PatientRecord>()
private val nextPatientId = AtomicInteger(1)

@Serializable
data class PatientRecord(
    val id: String,
    val name: String,
    val room: String,
    var currentLocation: LocationData? = null,
    val events: MutableList<PatientEvent> = mutableListOf()
)

@Serializable
data class PatientEvent(
    val timestamp: Long = System.currentTimeMillis(),
    val type: String,
    val description: String,
    val locationData: LocationData? = null
)

/**
 * 配置位置相关路由
 */
fun Routing.configureLocationRoutes() {
    // MQTT服务实例
    val mqttService = MqttService()
    
    // 活跃的WebSocket会话，sessionId -> WebSocketSession
    val activeSessions = ConcurrentHashMap<String, WebSocketSession>()
    
    // 初始化一些测试数据
    initializeTestData()
    
    // 路由组：位置API
    route("/api/locations") {
        // 获取所有设备的最新位置
        get {
            call.respond(deviceLocations)
        }
        
        // 获取特定设备的最新位置
        get("/{deviceId}") {
            val deviceId = call.parameters["deviceId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "设备ID不能为空")
            )
            
            val location = deviceLocations[deviceId]
            if (location != null) {
                call.respond(location)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "找不到设备位置数据"))
            }
        }
        
        // 更新设备位置
        post("/{deviceId}") {
            val deviceId = call.parameters["deviceId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "设备ID不能为空")
            )
            
            try {
                val locationData = call.receive<LocationData>()
                deviceLocations[deviceId] = locationData
                
                // 更新关联患者的位置
                patientRecords.values.forEach { patient ->
                    if (patient.currentLocation?.deviceId == deviceId) {
                        patient.currentLocation = locationData
                    }
                }
                
                // 通知所有WebSocket客户端
                notifyWebSocketClients(activeSessions)
                
                call.respond(HttpStatusCode.OK, mapOf("status" to "success"))
            } catch (e: Exception) {
                logger.error("更新位置失败: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "更新位置失败: ${e.message}"))
            }
        }
    }
    
    // 患者管理API
    route("/api/patients") {
        // 获取所有患者
        get {
            call.respond(patientRecords.values.toList())
        }
        
        // 获取特定患者
        get("/{patientId}") {
            val patientId = call.parameters["patientId"] ?: return@get call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "患者ID不能为空")
            )
            
            val patient = patientRecords[patientId]
            if (patient != null) {
                call.respond(patient)
            } else {
                call.respond(HttpStatusCode.NotFound, mapOf("error" to "找不到患者记录"))
            }
        }
        
        // 创建新患者
        post {
            try {
                val patientData = call.receive<PatientRecord>()
                val patientId = "P${nextPatientId.getAndIncrement()}"
                val newPatient = patientData.copy(id = patientId)
                patientRecords[patientId] = newPatient
                
                call.respond(HttpStatusCode.Created, newPatient)
            } catch (e: Exception) {
                logger.error("创建患者失败: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "创建患者失败: ${e.message}"))
            }
        }
        
        // 为患者添加事件
        post("/{patientId}/events") {
            val patientId = call.parameters["patientId"] ?: return@post call.respond(
                HttpStatusCode.BadRequest, mapOf("error" to "患者ID不能为空")
            )
            
            try {
                val event = call.receive<PatientEvent>()
                val patient = patientRecords[patientId]
                
                if (patient != null) {
                    patient.events.add(event)
                    
                    // 通知所有WebSocket客户端
                    notifyWebSocketClients(activeSessions)
                    
                    call.respond(HttpStatusCode.Created, event)
                } else {
                    call.respond(HttpStatusCode.NotFound, mapOf("error" to "找不到患者记录"))
                }
            } catch (e: Exception) {
                logger.error("添加患者事件失败: ${e.message}")
                call.respond(HttpStatusCode.InternalServerError, mapOf("error" to "添加患者事件失败: ${e.message}"))
            }
        }
    }
    
    // WebSocket端点：位置和患者实时更新
    webSocket("/ws/updates") {
        val sessionId = call.request.headers["Origin"] ?: System.currentTimeMillis().toString()
        activeSessions[sessionId] = this
        
        try {
            // 发送初始数据
            val initialData = mapOf(
                "deviceLocations" to deviceLocations,
                "patients" to patientRecords.values.toList()
            )
            outgoing.send(Frame.Text(Json.encodeToString(initialData)))
            
            // 保持会话活跃并处理客户端消息
            for (frame in incoming) {
                if (frame is Frame.Text) {
                    val text = frame.readText()
                    logger.info("收到客户端消息: $text")
                    
                    // 可以扩展实现对特定命令的处理
                }
            }
        } catch (e: Exception) {
            logger.error("WebSocket错误: ${e.message}")
        } finally {
            activeSessions.remove(sessionId)
        }
    }
    
    // WebSocket端点：模拟位置更新
    webSocket("/ws/location-updates") {
        val sessionId = call.request.headers["Origin"] ?: System.currentTimeMillis().toString()
        
        try {
            // 定期发送更新
            while (isActive) {
                // 如果要添加随机移动，可以在这里更新deviceLocations中的位置
                simulateMovement()
                
                // 发送当前状态
                val currentData = mapOf(
                    "deviceLocations" to deviceLocations,
                    "patients" to patientRecords.values.toList()
                )
                outgoing.send(Frame.Text(Json.encodeToString(currentData)))
                delay(1000) // 每秒更新一次
            }
        } catch (e: Exception) {
            logger.error("位置更新WebSocket错误: ${e.message}")
        }
    }
}

/**
 * 初始化测试数据
 */
private fun initializeTestData() {
    // 添加一些设备位置
    deviceLocations["device-001"] = LocationData(
        deviceId = "device-001",
        x = 100.0,
        y = 200.0,
        accuracy = 0.5,
        batteryLevel = 85
    )
    
    deviceLocations["device-002"] = LocationData(
        deviceId = "device-002",
        x = 300.0,
        y = 400.0,
        accuracy = 0.7,
        batteryLevel = 92
    )
    
    // 添加一些患者记录
    val patient1 = PatientRecord(
        id = "P1",
        name = "张三",
        room = "101",
        currentLocation = deviceLocations["device-001"],
        events = mutableListOf(
            PatientEvent(
                timestamp = System.currentTimeMillis() - 3600000, // 1小时前
                type = "CHECK_IN",
                description = "患者入院",
                locationData = deviceLocations["device-001"]
            )
        )
    )
    
    val patient2 = PatientRecord(
        id = "P2",
        name = "李四",
        room = "102",
        currentLocation = deviceLocations["device-002"],
        events = mutableListOf(
            PatientEvent(
                timestamp = System.currentTimeMillis() - 7200000, // 2小时前
                type = "CHECK_IN",
                description = "患者入院",
                locationData = deviceLocations["device-002"]
            ),
            PatientEvent(
                timestamp = System.currentTimeMillis() - 3600000, // 1小时前
                type = "MEDICATION",
                description = "服用药物",
                locationData = deviceLocations["device-002"]
            )
        )
    )
    
    patientRecords["P1"] = patient1
    patientRecords["P2"] = patient2
    
    // 设置下一个ID
    nextPatientId.set(3)
}

/**
 * 模拟设备移动
 */
private fun simulateMovement() {
    deviceLocations.forEach { (deviceId, location) ->
        // 随机小幅度移动
        val newX = location.x + (Math.random() * 10 - 5)
        val newY = location.y + (Math.random() * 10 - 5)
        
        deviceLocations[deviceId] = location.copy(
            x = newX,
            y = newY,
            timestamp = System.currentTimeMillis() / 1000
        )
        
        // 更新关联患者的位置
        patientRecords.values.forEach { patient ->
            if (patient.currentLocation?.deviceId == deviceId) {
                patient.currentLocation = deviceLocations[deviceId]
            }
        }
    }
}

/**
 * 通知所有WebSocket客户端
 */
private suspend fun notifyWebSocketClients(sessions: ConcurrentHashMap<String, WebSocketSession>) {
    val currentData = mapOf(
        "deviceLocations" to deviceLocations,
        "patients" to patientRecords.values.toList()
    )
    val jsonData = Json.encodeToString(currentData)
    
    sessions.values.forEach { session ->
        try {
            session.outgoing.send(Frame.Text(jsonData))
        } catch (e: ClosedSendChannelException) {
            // 忽略已关闭的通道
        } catch (e: Exception) {
            logger.error("通知WebSocket客户端失败: ${e.message}")
        }
    }
} 