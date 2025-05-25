package com.seniorcareplus.services

import com.seniorcareplus.models.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString
import org.eclipse.paho.client.mqttv3.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

class MqttService {
    private val logger = LoggerFactory.getLogger(MqttService::class.java)
    private var client: MqttClient? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    // 數據存儲服務
    private val dataStorageService = DataStorageService()
    
    // 連接的設備列表
    private val connectedDevices = ConcurrentHashMap<String, Long>()
    
    companion object {
        private const val BROKER_URL = "tcp://localhost:1883"
        private const val CLIENT_ID = "SeniorCarePlusBackend"
        private const val QOS = 1
        
        // MQTT主題
        private const val HEART_RATE_TOPIC = "seniorcareplus/heartrate"
        private const val TEMPERATURE_TOPIC = "seniorcareplus/temperature"
        private const val DIAPER_TOPIC = "seniorcareplus/diaper"
        private const val LOCATION_TOPIC = "seniorcareplus/location"
        private const val DEVICE_STATUS_TOPIC = "seniorcareplus/device/status"
    }
    
    suspend fun connect(): Boolean {
        return try {
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                keepAliveInterval = 60
                connectionTimeout = 30
                isAutomaticReconnect = true
            }
            
            client = MqttClient(BROKER_URL, CLIENT_ID).apply {
                setCallback(createMqttCallback())
                connect(options)
            }
            
            // 訂閱所有相關主題
            subscribeToTopics()
            
            logger.info("MQTT客戶端連接成功: $BROKER_URL")
            true
        } catch (e: Exception) {
            logger.error("MQTT連接失敗", e)
            false
        }
    }
    
    private fun subscribeToTopics() {
        client?.let { mqttClient ->
            val topics = arrayOf(
                HEART_RATE_TOPIC,
                TEMPERATURE_TOPIC,
                DIAPER_TOPIC,
                LOCATION_TOPIC,
                DEVICE_STATUS_TOPIC
            )
            
            val qosArray = IntArray(topics.size) { QOS }
            mqttClient.subscribe(topics, qosArray)
            logger.info("已訂閱MQTT主題: ${topics.joinToString(", ")}")
        }
    }
    
    private fun createMqttCallback() = object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            logger.warn("MQTT連接丟失", cause)
            // 嘗試重新連接
            GlobalScope.launch {
                delay(5000)
                connect()
            }
        }
        
        override fun messageArrived(topic: String, message: MqttMessage) {
            try {
                val payload = String(message.payload)
                logger.debug("收到MQTT消息 - 主題: $topic, 內容: $payload")
                
                // 處理不同類型的消息
                when (topic) {
                    HEART_RATE_TOPIC -> handleHeartRateData(payload)
                    TEMPERATURE_TOPIC -> handleTemperatureData(payload)
                    DIAPER_TOPIC -> handleDiaperData(payload)
                    LOCATION_TOPIC -> handleLocationData(payload)
                    DEVICE_STATUS_TOPIC -> handleDeviceStatus(payload)
                }
            } catch (e: Exception) {
                logger.error("處理MQTT消息時出錯 - 主題: $topic", e)
            }
        }
        
        override fun deliveryComplete(token: IMqttDeliveryToken?) {
            logger.debug("MQTT消息發送完成")
        }
    }
    
    private fun handleHeartRateData(payload: String) {
        try {
            val heartRateData = json.decodeFromString<HeartRateData>(payload)
            logger.info("處理心率數據: 患者=${heartRateData.patientId}, 心率=${heartRateData.heartRate}")
            
            // 存儲到數據庫
            GlobalScope.launch {
                dataStorageService.saveHeartRateData(heartRateData)
            }
            
            // 檢查異常心率
            checkHeartRateAlert(heartRateData)
            
        } catch (e: Exception) {
            logger.error("解析心率數據失敗: $payload", e)
        }
    }
    
    private fun handleTemperatureData(payload: String) {
        try {
            val temperatureData = json.decodeFromString<TemperatureData>(payload)
            logger.info("處理體溫數據: 患者=${temperatureData.patientId}, 體溫=${temperatureData.temperature}")
            
            // 存儲到數據庫
            GlobalScope.launch {
                dataStorageService.saveTemperatureData(temperatureData)
            }
            
            // 檢查異常體溫
            checkTemperatureAlert(temperatureData)
            
        } catch (e: Exception) {
            logger.error("解析體溫數據失敗: $payload", e)
        }
    }
    
    private fun handleDiaperData(payload: String) {
        try {
            val diaperData = json.decodeFromString<DiaperData>(payload)
            logger.info("處理尿布數據: 患者=${diaperData.patientId}, 狀態=${diaperData.status}")
            
            // 存儲到數據庫
            GlobalScope.launch {
                dataStorageService.saveDiaperData(diaperData)
            }
            
            // 檢查尿布狀態警報
            checkDiaperAlert(diaperData)
            
        } catch (e: Exception) {
            logger.error("解析尿布數據失敗: $payload", e)
        }
    }
    
    private fun handleLocationData(payload: String) {
        try {
            val locationData = json.decodeFromString<LocationData>(payload)
            logger.info("處理位置數據: 患者=${locationData.patientId}, 位置=(${locationData.x}, ${locationData.y})")
            
            // 存儲到數據庫
            GlobalScope.launch {
                dataStorageService.saveLocationData(locationData)
            }
            
        } catch (e: Exception) {
            logger.error("解析位置數據失敗: $payload", e)
        }
    }
    
    private fun handleDeviceStatus(payload: String) {
        try {
            // 假設設備狀態消息格式為: {"deviceId": "device123", "status": "online", "batteryLevel": 85}
            val deviceStatus = json.decodeFromString<Map<String, Any>>(payload)
            val deviceId = deviceStatus["deviceId"] as? String ?: return
            
            connectedDevices[deviceId] = System.currentTimeMillis()
            logger.info("設備狀態更新: $deviceId")
            
            // 更新設備狀態到數據庫
            GlobalScope.launch {
                dataStorageService.updateDeviceStatus(deviceStatus)
            }
            
        } catch (e: Exception) {
            logger.error("解析設備狀態失敗: $payload", e)
        }
    }
    
    private fun checkHeartRateAlert(data: HeartRateData) {
        val heartRate = data.heartRate
        when {
            heartRate > 100 -> {
                logger.warn("心率過高警報: 患者=${data.patientId}, 心率=$heartRate")
                // 創建高心率警報
                GlobalScope.launch {
                    dataStorageService.createAlert(
                        patientId = data.patientId,
                        alertType = "emergency",
                        title = "心率異常",
                        message = "患者心率過高: ${heartRate}bpm",
                        severity = "high",
                        deviceId = data.deviceId
                    )
                }
            }
            heartRate < 60 -> {
                logger.warn("心率過低警報: 患者=${data.patientId}, 心率=$heartRate")
                // 創建低心率警報
                GlobalScope.launch {
                    dataStorageService.createAlert(
                        patientId = data.patientId,
                        alertType = "warning",
                        title = "心率異常",
                        message = "患者心率過低: ${heartRate}bpm",
                        severity = "medium",
                        deviceId = data.deviceId
                    )
                }
            }
        }
    }
    
    private fun checkTemperatureAlert(data: TemperatureData) {
        val temperature = data.temperature
        when {
            temperature > 38.0 -> {
                logger.warn("體溫過高警報: 患者=${data.patientId}, 體溫=$temperature")
                GlobalScope.launch {
                    dataStorageService.createAlert(
                        patientId = data.patientId,
                        alertType = "emergency",
                        title = "體溫異常",
                        message = "患者體溫過高: ${temperature}°C",
                        severity = "high",
                        deviceId = data.deviceId
                    )
                }
            }
            temperature < 36.0 -> {
                logger.warn("體溫過低警報: 患者=${data.patientId}, 體溫=$temperature")
                GlobalScope.launch {
                    dataStorageService.createAlert(
                        patientId = data.patientId,
                        alertType = "warning",
                        title = "體溫異常",
                        message = "患者體溫過低: ${temperature}°C",
                        severity = "medium",
                        deviceId = data.deviceId
                    )
                }
            }
        }
    }
    
    private fun checkDiaperAlert(data: DiaperData) {
        if (data.status == "wet" || data.status == "soiled") {
            logger.info("尿布需要更換: 患者=${data.patientId}, 狀態=${data.status}")
            GlobalScope.launch {
                dataStorageService.createAlert(
                    patientId = data.patientId,
                    alertType = "info",
                    title = "尿布提醒",
                    message = "患者尿布需要更換",
                    severity = "low",
                    deviceId = data.deviceId
                )
            }
        }
    }
    
    fun disconnect() {
        try {
            client?.disconnect()
            client?.close()
            logger.info("MQTT客戶端已斷開連接")
        } catch (e: Exception) {
            logger.error("斷開MQTT連接時出錯", e)
        }
    }
    
    fun isConnected(): Boolean {
        return client?.isConnected ?: false
    }
    
    // 獲取連接的設備列表
    fun getConnectedDevices(): Map<String, Long> {
        return connectedDevices.toMap()
    }
}