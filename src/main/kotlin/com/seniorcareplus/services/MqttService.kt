package com.seniorcareplus.services

import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import org.eclipse.paho.client.mqttv3.*
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.Instant
import java.time.ZoneId
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom
import java.security.cert.X509Certificate

/**
 * MQTT服務 - 接收MQTT數據並存儲到數據庫
 */
class MqttService {
    private val logger = LoggerFactory.getLogger(MqttService::class.java)
    private var mqttClient: MqttAsyncClient? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    companion object {
        // 遠端MQTT服務器配置 (與app端相同)
        private const val MQTT_REMOTE_SERVER_URI = "wss://067ec32ef1344d3bb20c4e53abdde99a.s1.eu.hivemq.cloud:8884/mqtt"
        private const val MQTT_REMOTE_USER = "testweb1"
        private const val MQTT_REMOTE_PASSWORD = "Aa000000"
        
        // 訂閱主題列表 (與app端相同)
        private val SUBSCRIBE_TOPICS = arrayOf(
            "UWB/GW16B8_Loca",      // 位置數據
            "health/heart_rate/+",   // 心率數據 
            "health/temperature/+",  // 體溫數據
            "health/diaper/+",       // 尿布數據
            "health/alert/+"         // 警報數據
        )
        
        // 本地MQTT配置 (備用)
        private const val MQTT_LOCAL_SERVER_URI = "tcp://localhost:1883"
        
        private const val CLIENT_ID_PREFIX = "SeniorCarePlusBackend_"
        private const val QOS_1 = 1
    }
    
    /**
     * 連接MQTT服務器並開始接收數據
     */
    suspend fun connect(): Boolean {
        return try {
            logger.info("正在初始化MQTT客戶端...")
            
            // 生成唯一客戶端ID
            val clientId = CLIENT_ID_PREFIX + System.currentTimeMillis()
            
            // 創建MQTT異步客戶端
            mqttClient = MqttAsyncClient(
                MQTT_REMOTE_SERVER_URI,
                clientId,
                org.eclipse.paho.client.mqttv3.persist.MemoryPersistence()
            )
            
            // 配置SSL連接
            val connOpts = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
                isAutomaticReconnect = true
                maxInflight = 10
                userName = MQTT_REMOTE_USER
                password = MQTT_REMOTE_PASSWORD.toCharArray()
                
                // SSL配置
                socketFactory = createTrustAllSSLContext().socketFactory
            }
            
            // 設置回調
            mqttClient?.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    logger.warn("MQTT連接丟失: ${cause?.message}")
                    GlobalScope.launch {
                        reconnect()
                    }
                }
                
                override fun messageArrived(topic: String?, message: MqttMessage?) {
                    try {
                        if (topic != null && message != null) {
                            val messageContent = String(message.payload)
                            logger.info("收到MQTT消息 - 主題: $topic, 內容: $messageContent")
                            
                            // 處理並存儲消息
                            GlobalScope.launch {
                                processMessage(topic, messageContent)
                            }
                        }
                    } catch (e: Exception) {
                        logger.error("處理MQTT消息時發生錯誤: ${e.message}")
                    }
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // 不需要處理發送完成事件
                }
            })
            
            // 連接到MQTT代理
            logger.info("正在連接到MQTT服務器: $MQTT_REMOTE_SERVER_URI")
            mqttClient?.connect(connOpts)?.waitForCompletion(10000)
            
            if (mqttClient?.isConnected == true) {
                logger.info("MQTT連接成功")
                
                // 訂閱主題
                subscribeToTopics()
                
                return true
            } else {
                logger.error("MQTT連接失敗")
                return false
            }
            
        } catch (e: Exception) {
            logger.error("MQTT連接過程中發生錯誤: ${e.message}")
            false
        }
    }
    
    /**
     * 訂閱所有相關主題
     */
    private suspend fun subscribeToTopics() {
        try {
            SUBSCRIBE_TOPICS.forEach { topic ->
                mqttClient?.subscribe(topic, QOS_1)
                logger.info("訂閱主題: $topic")
            }
        } catch (e: Exception) {
            logger.error("訂閱主題失敗: ${e.message}")
        }
    }
    
    /**
     * 處理接收到的MQTT消息
     */
    private suspend fun processMessage(topic: String, messageContent: String) {
        try {
            when {
                topic.contains("Loca") -> {
                    // 處理位置數據
                    processLocationData(messageContent)
                }
                topic.contains("heart_rate") -> {
                    // 處理心率數據  
                    processHeartRateData(messageContent, extractPatientIdFromTopic(topic))
                }
                topic.contains("temperature") -> {
                    // 處理體溫數據
                    processTemperatureData(messageContent, extractPatientIdFromTopic(topic))
                }
                topic.contains("diaper") -> {
                    // 處理尿布數據
                    processDiaperData(messageContent, extractPatientIdFromTopic(topic))
                }
                topic.contains("alert") -> {
                    // 處理警報數據
                    processAlertData(messageContent, extractPatientIdFromTopic(topic))
                }
                else -> {
                    logger.debug("未知主題類型: $topic")
                }
            }
        } catch (e: Exception) {
            logger.error("處理消息失敗 - 主題: $topic, 錯誤: ${e.message}")
        }
    }
    
    /**
     * 處理位置數據
     */
    private suspend fun processLocationData(messageContent: String) {
        try {
            // 解析UWB位置數據 (假設格式與app端相同)
            val locationData = json.decodeFromString<LocationData>(messageContent)
            
            // 存儲到數據庫
            transaction {
                // 檢查患者是否存在
                val patient = Patients.select { Patients.deviceId eq (locationData.deviceId ?: "unknown") }
                    .singleOrNull()
                
                if (patient != null) {
                    val patientId = patient[Patients.id].value
                    
                    // 插入位置記錄
                    LocationRecords.insert {
                        it[LocationRecords.patientId] = patientId
                        it[x] = locationData.x
                        it[y] = locationData.y
                        it[z] = locationData.z
                        it[accuracy] = locationData.accuracy
                        it[area] = locationData.area
                        it[deviceId] = locationData.deviceId ?: "unknown"
                        it[timestamp] = Instant.ofEpochSecond(locationData.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    
                    logger.info("位置數據已存儲 - 患者ID: $patientId, 位置: (${locationData.x}, ${locationData.y})")
                } else {
                    logger.warn("找不到對應的患者 - 設備ID: ${locationData.deviceId}")
                }
            }
            
        } catch (e: Exception) {
            logger.error("處理位置數據失敗: ${e.message}")
        }
    }
    
    /**
     * 處理心率數據
     */
    private suspend fun processHeartRateData(messageContent: String, patientId: String) {
        try {
            val heartRateData = json.decodeFromString<HeartRateData>(messageContent)
            
            transaction {
                // 檢查患者是否存在
                val patient = Patients.select { Patients.deviceId eq patientId }
                    .singleOrNull()
                
                if (patient != null) {
                    val id = patient[Patients.id].value
                    
                    // 插入健康記錄
                    HealthRecords.insert {
                        it[HealthRecords.patientId] = id
                        it[dataType] = "heart_rate"
                        it[value] = json.encodeToString(heartRateData)
                        it[deviceId] = heartRateData.deviceId ?: patientId
                        it[quality] = heartRateData.quality
                        it[unit] = "bpm"
                        it[timestamp] = Instant.ofEpochSecond(heartRateData.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    
                    logger.info("心率數據已存儲 - 患者: $patientId, 心率: ${heartRateData.heartRate} bpm")
                }
            }
            
        } catch (e: Exception) {
            logger.error("處理心率數據失敗: ${e.message}")
        }
    }
    
    /**
     * 處理體溫數據
     */
    private suspend fun processTemperatureData(messageContent: String, patientId: String) {
        try {
            val temperatureData = json.decodeFromString<TemperatureData>(messageContent)
            
            transaction {
                val patient = Patients.select { Patients.deviceId eq patientId }
                    .singleOrNull()
                
                if (patient != null) {
                    val id = patient[Patients.id].value
                    
                    HealthRecords.insert {
                        it[HealthRecords.patientId] = id
                        it[dataType] = "temperature"
                        it[value] = json.encodeToString(temperatureData)
                        it[deviceId] = temperatureData.deviceId ?: patientId
                        it[unit] = temperatureData.unit
                        it[timestamp] = Instant.ofEpochSecond(temperatureData.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    
                    logger.info("體溫數據已存儲 - 患者: $patientId, 體溫: ${temperatureData.temperature}°${temperatureData.unit}")
                }
            }
            
        } catch (e: Exception) {
            logger.error("處理體溫數據失敗: ${e.message}")
        }
    }
    
    /**
     * 處理尿布數據
     */
    private suspend fun processDiaperData(messageContent: String, patientId: String) {
        try {
            val diaperData = json.decodeFromString<DiaperData>(messageContent)
            
            transaction {
                val patient = Patients.select { Patients.deviceId eq patientId }
                    .singleOrNull()
                
                if (patient != null) {
                    val id = patient[Patients.id].value
                    
                    HealthRecords.insert {
                        it[HealthRecords.patientId] = id
                        it[dataType] = "diaper"
                        it[value] = json.encodeToString(diaperData)
                        it[deviceId] = diaperData.deviceId ?: patientId
                        it[timestamp] = Instant.ofEpochSecond(diaperData.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    
                    logger.info("尿布數據已存儲 - 患者: $patientId, 狀態: ${diaperData.status}")
                }
            }
            
        } catch (e: Exception) {
            logger.error("處理尿布數據失敗: ${e.message}")
        }
    }
    
    /**
     * 處理警報數據
     */
    private suspend fun processAlertData(messageContent: String, patientId: String) {
        try {
            val alertData = json.decodeFromString<AlertData>(messageContent)
            
            transaction {
                val patient = Patients.select { Patients.deviceId eq patientId }
                    .singleOrNull()
                
                if (patient != null) {
                    val id = patient[Patients.id].value
                    
                    Alerts.insert {
                        it[Alerts.patientId] = id
                        it[alertType] = alertData.alertType
                        it[title] = alertData.title
                        it[message] = alertData.message
                        it[severity] = alertData.severity
                        it[deviceId] = alertData.deviceId
                        it[triggeredAt] = Instant.ofEpochSecond(alertData.timestamp)
                            .atZone(ZoneId.systemDefault()).toLocalDateTime()
                    }
                    
                    logger.info("警報已存儲 - 患者: $patientId, 類型: ${alertData.alertType}")
                }
            }
            
        } catch (e: Exception) {
            logger.error("處理警報數據失敗: ${e.message}")
        }
    }
    
    /**
     * 從主題中提取患者ID
     */
    private fun extractPatientIdFromTopic(topic: String): String {
        return topic.split("/").lastOrNull() ?: "unknown"
    }
    
    /**
     * 重新連接
     */
    private suspend fun reconnect() {
        delay(5000)
        logger.info("嘗試重新連接MQTT...")
        connect()
    }
    
    /**
     * 創建信任所有證書的SSL上下文
     */
    private fun createTrustAllSSLContext(): SSLContext {
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })
        
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())
        return sslContext
    }
    
    /**
     * 斷開連接
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            logger.info("MQTT連接已斷開")
        } catch (e: Exception) {
            logger.error("斷開MQTT連接時發生錯誤: ${e.message}")
        }
    }
} 