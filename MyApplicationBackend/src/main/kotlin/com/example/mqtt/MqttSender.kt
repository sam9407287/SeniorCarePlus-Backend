package com.example.mqtt

import com.example.models.LocationData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.slf4j.LoggerFactory
import java.util.*
import kotlin.concurrent.fixedRateTimer

/**
 * 模拟MQTT发送器，用于发送模拟数据到MQTT服务器
 */
class MqttSender {
    private val logger = LoggerFactory.getLogger(this::class.java)
    private lateinit var mqttClient: MqttClient
    private val json = Json { prettyPrint = true; encodeDefaults = true }
    
    // 模拟数据
    private val patients = mapOf(
        "P1" to Patient(
            id = "P1",
            nameZh = "张三",
            nameEn = "Zhang San",
            room = "101",
            deviceId = "device-001"
        ),
        "P2" to Patient(
            id = "P2",
            nameZh = "李四",
            nameEn = "Li Si",
            room = "102",
            deviceId = "device-002"
        ),
        "P3" to Patient(
            id = "P3",
            nameZh = "王五",
            nameEn = "Wang Wu",
            room = "103",
            deviceId = "device-003"
        )
    )
    
    private val diaperStatuses = listOf("DRY", "WET", "SOILED", "VERY_WET")
    private val eventTypes = listOf("CHECK_IN", "MEDICATION", "CHECK_OUT", "MEAL", "DIAPER_CHANGE", "VITAL_SIGNS")
    
    /**
     * 连接到MQTT代理
     */
    fun connect(brokerUrl: String = "tcp://localhost:1883") {
        try {
            val clientId = "mqtt-simulator-${System.currentTimeMillis()}"
            mqttClient = MqttClient(brokerUrl, clientId)
            
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
            }
            
            mqttClient.connect(options)
            logger.info("已连接到MQTT代理: $brokerUrl")
        } catch (e: Exception) {
            logger.error("连接MQTT代理失败: ${e.message}", e)
            throw e
        }
    }
    
    /**
     * 启动模拟数据发送
     */
    fun startSimulation() {
        if (!::mqttClient.isInitialized || !mqttClient.isConnected) {
            connect()
        }
        
        // 位置更新定时器 - 每1秒发送一次
        fixedRateTimer("location-updates", daemon = true, period = 1000) {
            try {
                sendLocationUpdates()
            } catch (e: Exception) {
                logger.error("发送位置更新失败: ${e.message}")
            }
        }
        
        // 生命体征定时器 - 每5秒发送一次
        fixedRateTimer("vital-signs", daemon = true, period = 5000) {
            try {
                sendVitalSigns()
            } catch (e: Exception) {
                logger.error("发送生命体征失败: ${e.message}")
            }
        }
        
        // 事件定时器 - 每10秒发送一次
        fixedRateTimer("events", daemon = true, period = 10000) {
            try {
                sendRandomEvent()
            } catch (e: Exception) {
                logger.error("发送事件失败: ${e.message}")
            }
        }
        
        logger.info("模拟数据发送已启动")
    }
    
    /**
     * 发送位置更新
     */
    private fun sendLocationUpdates() {
        patients.forEach { (_, patient) ->
            // 生成随机位置波动
            val locationData = LocationData(
                deviceId = patient.deviceId,
                x = 100.0 + Math.random() * 50 * (if (Math.random() > 0.5) 1 else -1),
                y = 200.0 + Math.random() * 50 * (if (Math.random() > 0.5) 1 else -1),
                z = 0.0,
                accuracy = 0.5 + Math.random() * 0.5,
                batteryLevel = (60 + (Math.random() * 40).toInt()),
                timestamp = System.currentTimeMillis() / 1000,
                area = "hospital-floor-1"
            )
            
            val topic = "uwb/location/${patient.deviceId}"
            val payload = json.encodeToString(locationData)
            
            mqttClient.publish(topic, payload.toByteArray(), 1, false)
            logger.debug("已发送位置更新: $topic")
        }
    }
    
    /**
     * 发送生命体征数据
     */
    private fun sendVitalSigns() {
        patients.values.random().let { patient ->
            // 生成随机生命体征
            val vitalSigns = VitalSigns(
                patientId = patient.id,
                temperature = 36.5 + (Math.random() * 2 - 1),
                heartRate = 70 + (Math.random() * 30).toInt(),
                diaperStatus = diaperStatuses.random(),
                bloodPressureSystolic = 120 + (Math.random() * 20 - 10).toInt(),
                bloodPressureDiastolic = 80 + (Math.random() * 10 - 5).toInt(),
                timestamp = System.currentTimeMillis()
            )
            
            val topic = "patient/vitals/${patient.id}"
            val payload = json.encodeToString(vitalSigns)
            
            mqttClient.publish(topic, payload.toByteArray(), 1, false)
            logger.debug("已发送生命体征: $topic")
        }
    }
    
    /**
     * 发送随机事件
     */
    private fun sendRandomEvent() {
        patients.values.random().let { patient ->
            // 生成随机事件
            val eventType = eventTypes.random()
            val description = when (eventType) {
                "MEDICATION" -> "服用药物：${listOf("退烧药", "止痛药", "抗生素").random()}"
                "MEAL" -> "用餐：${listOf("早餐", "午餐", "晚餐").random()}"
                "DIAPER_CHANGE" -> "更换尿布：${diaperStatuses.random()}"
                "CHECK_IN" -> "患者入院"
                "CHECK_OUT" -> "患者出院"
                "VITAL_SIGNS" -> "测量生命体征"
                else -> "一般检查"
            }
            
            val event = PatientEvent(
                patientId = patient.id,
                type = eventType,
                description = description,
                timestamp = System.currentTimeMillis(),
                locationData = LocationData(
                    deviceId = patient.deviceId,
                    x = 100.0 + Math.random() * 50,
                    y = 200.0 + Math.random() * 50,
                    timestamp = System.currentTimeMillis() / 1000
                )
            )
            
            val topic = "patient/events/${patient.id}"
            val payload = json.encodeToString(event)
            
            mqttClient.publish(topic, payload.toByteArray(), 1, false)
            logger.debug("已发送事件: $topic")
        }
    }
    
    /**
     * 关闭连接
     */
    fun disconnect() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
            logger.info("已断开MQTT连接")
        }
    }
    
    /**
     * 数据模型 - 仅用于模拟发送
     */
    @kotlinx.serialization.Serializable
    data class Patient(
        val id: String,
        val nameZh: String,
        val nameEn: String,
        val room: String,
        val deviceId: String
    )
    
    @kotlinx.serialization.Serializable
    data class VitalSigns(
        val patientId: String,
        val temperature: Double,
        val heartRate: Int,
        val diaperStatus: String,
        val bloodPressureSystolic: Int?,
        val bloodPressureDiastolic: Int?,
        val timestamp: Long
    )
    
    @kotlinx.serialization.Serializable
    data class PatientEvent(
        val patientId: String,
        val type: String,
        val description: String,
        val timestamp: Long,
        val locationData: LocationData? = null
    )
} 