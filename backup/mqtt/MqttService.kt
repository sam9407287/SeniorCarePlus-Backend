package com.seniorcareplus.mqtt

import com.seniorcareplus.models.*
import com.seniorcareplus.services.HealthDataService
import com.seniorcareplus.services.LocationService
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap

/**
 * MQTT服務類，負責接收和處理來自設備的健康數據和位置數據
 */
class MqttService(
    private val healthDataService: HealthDataService,
    private val locationService: LocationService
) {
    private val logger = LoggerFactory.getLogger(MqttService::class.java)
    private var mqttClient: MqttClient? = null
    private val json = Json { ignoreUnknownKeys = true }
    
    // 連接狀態監控
    private val deviceStatus = ConcurrentHashMap<String, Boolean>()
    
    companion object {
        private const val BROKER_URL = "tcp://localhost:1883"
        private const val CLIENT_ID = "SeniorCarePlusBackend"
        
        // MQTT主題
        private const val HEART_RATE_TOPIC = "seniorcareplus/heartrate/+"
        private const val TEMPERATURE_TOPIC = "seniorcareplus/temperature/+"
        private const val DIAPER_TOPIC = "seniorcareplus/diaper/+"
        private const val LOCATION_TOPIC = "seniorcareplus/location/+"
        private const val DEVICE_STATUS_TOPIC = "seniorcareplus/device/status/+"
        private const val GATEWAY_TOPIC = "seniorcareplus/gateway/+"
        private const val BATTERY_TOPIC = "seniorcareplus/battery/+"
        
        // 新增的健康監測主題
        private const val BLOOD_PRESSURE_TOPIC = "seniorcareplus/bloodpressure/+"
        private const val SLEEP_DATA_TOPIC = "seniorcareplus/sleep/+"
        private const val STEPS_TOPIC = "seniorcareplus/steps/+"
    }
    
    /**
     * 初始化MQTT連接
     */
    suspend fun connect(): Boolean = withContext(Dispatchers.IO) {
        try {
            logger.info("正在連接到MQTT代理: $BROKER_URL")
            
            mqttClient = MqttClient(BROKER_URL, CLIENT_ID).apply {
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 30
                    keepAliveInterval = 60
                    isAutomaticReconnect = true
                }
                
                setCallback(object : MqttCallback {
                    override fun connectionLost(cause: Throwable?) {
                        logger.warn("MQTT連接丟失", cause)
                        // 自動重連邏輯
                        GlobalScope.launch {
                            delay(5000)
                            reconnect()
                        }
                    }
                    
                    override fun messageArrived(topic: String, message: MqttMessage) {
                        handleMessage(topic, message)
                    }
                    
                    override fun deliveryComplete(token: IMqttDeliveryToken?) {
                        // 消息發送完成
                    }
                })
                
                connect(options)
                logger.info("MQTT連接成功")
                
                // 訂閱所有相關主題
                subscribeToTopics()
            }
            
            true
        } catch (e: Exception) {
            logger.error("MQTT連接失敗", e)
            false
        }
    }
    
    /**
     * 訂閱MQTT主題
     */
    private fun subscribeToTopics() {
        try {
            val topics = arrayOf(
                HEART_RATE_TOPIC,
                TEMPERATURE_TOPIC,
                DIAPER_TOPIC,
                LOCATION_TOPIC,
                DEVICE_STATUS_TOPIC,
                GATEWAY_TOPIC,
                BATTERY_TOPIC,
                BLOOD_PRESSURE_TOPIC,
                SLEEP_DATA_TOPIC,
                STEPS_TOPIC
            )
            
            topics.forEach { topic ->
                mqttClient?.subscribe(topic, 1)
                logger.info("已訂閱主題: $topic")
            }
        } catch (e: Exception) {
            logger.error("訂閱主題失敗", e)
        }
    }
    
    /**
     * 處理接收到的MQTT消息
     */
    private fun handleMessage(topic: String, message: MqttMessage) {
        GlobalScope.launch {
            try {
                val payload = String(message.payload)
                logger.debug("收到消息 - 主題: $topic, 內容: $payload")
                
                when {
                    // 健康數據處理
                    topic.startsWith("seniorcareplus/heartrate/") -> {
                        val patientId = extractPatientId(topic)
                        val heartRateData = json.decodeFromString<HeartRateData>(payload)
                        healthDataService.saveHeartRateData(patientId, heartRateData)
                        logger.info("保存心率數據: 患者ID=$patientId, 心率=${heartRateData.heartRate}")
                    }
                    
                    topic.startsWith("seniorcareplus/temperature/") -> {
                        val patientId = extractPatientId(topic)
                        val temperatureData = json.decodeFromString<TemperatureData>(payload)
                        healthDataService.saveTemperatureData(patientId, temperatureData)
                        logger.info("保存體溫數據: 患者ID=$patientId, 體溫=${temperatureData.temperature}")
                    }
                    
                    topic.startsWith("seniorcareplus/diaper/") -> {
                        val patientId = extractPatientId(topic)
                        val diaperData = json.decodeFromString<DiaperData>(payload)
                        healthDataService.saveDiaperData(patientId, diaperData)
                        logger.info("保存尿布數據: 患者ID=$patientId, 狀態=${diaperData.status}")
                    }
                    
                    topic.startsWith("seniorcareplus/bloodpressure/") -> {
                        val patientId = extractPatientId(topic)
                        val bloodPressureData = json.decodeFromString<Map<String, Any>>(payload)
                        // 處理血壓數據
                        logger.info("保存血壓數據: 患者ID=$patientId, 數據=$bloodPressureData")
                    }
                    
                    topic.startsWith("seniorcareplus/sleep/") -> {
                        val patientId = extractPatientId(topic)
                        val sleepData = json.decodeFromString<Map<String, Any>>(payload)
                        // 處理睡眠數據
                        logger.info("保存睡眠數據: 患者ID=$patientId, 數據=$sleepData")
                    }
                    
                    topic.startsWith("seniorcareplus/steps/") -> {
                        val patientId = extractPatientId(topic)
                        val stepsData = json.decodeFromString<Map<String, Any>>(payload)
                        // 處理步數數據
                        logger.info("保存步數數據: 患者ID=$patientId, 數據=$stepsData")
                    }
                    
                    // 位置數據處理 - 核心功能
                    topic.startsWith("seniorcareplus/location/") -> {
                        val deviceId = extractDeviceId(topic)
                        val locationData = json.decodeFromString<LocationData>(payload)
                        
                        // 通過LocationService處理位置數據（會自動廣播到WebSocket客戶端）
                        locationService.updateDeviceLocation(locationData)
                        logger.info("處理位置數據: 設備ID=$deviceId, 位置=(${locationData.x}, ${locationData.y})")
                    }
                    
                    // Gateway狀態處理
                    topic.startsWith("seniorcareplus/gateway/") -> {
                        val gatewayId = extractDeviceId(topic)
                        val gatewayData = json.decodeFromString<Map<String, Any>>(payload)
                        
                        val status = gatewayData["status"] as? String ?: "unknown"
                        val connectedDevices = (gatewayData["connected_devices"] as? Number)?.toInt() ?: 0
                        
                        locationService.updateGatewayStatus(gatewayId, status, connectedDevices)
                        logger.info("更新Gateway狀態: ID=$gatewayId, 狀態=$status, 連接設備數=$connectedDevices")
                    }
                    
                    // 電池狀態處理
                    topic.startsWith("seniorcareplus/battery/") -> {
                        val deviceId = extractDeviceId(topic)
                        val batteryData = json.decodeFromString<Map<String, Any>>(payload)
                        
                        val batteryLevel = (batteryData["level"] as? Number)?.toInt() ?: 0
                        locationService.updateDeviceBattery(deviceId, batteryLevel)
                        logger.info("更新設備電池: ID=$deviceId, 電量=$batteryLevel%")
                    }
                    
                    // 設備狀態處理
                    topic.startsWith("seniorcareplus/device/status/") -> {
                        val deviceId = extractDeviceId(topic)
                        handleDeviceStatus(deviceId, payload)
                    }
                }
            } catch (e: Exception) {
                logger.error("處理MQTT消息失敗: $topic", e)
            }
        }
    }
    
    /**
     * 從主題中提取患者ID
     */
    private fun extractPatientId(topic: String): String {
        return topic.substringAfterLast("/")
    }
    
    /**
     * 從主題中提取設備ID
     */
    private fun extractDeviceId(topic: String): String {
        return topic.substringAfterLast("/")
    }
    
    /**
     * 處理設備狀態更新
     */
    private suspend fun handleDeviceStatus(deviceId: String, payload: String) {
        try {
            val statusData = json.decodeFromString<Map<String, Any>>(payload)
            val isOnline = statusData["online"] as? Boolean ?: false
            
            deviceStatus[deviceId] = isOnline
            healthDataService.updateDeviceStatus(deviceId, isOnline)
            
            logger.info("設備狀態更新: 設備ID=$deviceId, 在線狀態=$isOnline")
        } catch (e: Exception) {
            logger.error("處理設備狀態失敗: $deviceId", e)
        }
    }
    
    /**
     * 發送指令到設備
     */
    suspend fun sendDeviceCommand(deviceId: String, command: Map<String, Any>) {
        try {
            val topic = "seniorcareplus/device/config/$deviceId"
            val message = json.encodeToString(command)
            
            mqttClient?.publish(topic, MqttMessage(message.toByteArray()))
            logger.info("發送設備指令: 設備ID=$deviceId, 指令=$command")
        } catch (e: Exception) {
            logger.error("發送設備指令失敗: $deviceId", e)
        }
    }
    
    /**
     * 重新連接MQTT
     */
    private suspend fun reconnect() {
        try {
            if (mqttClient?.isConnected == false) {
                logger.info("嘗試重新連接MQTT...")
                connect()
            }
        } catch (e: Exception) {
            logger.error("MQTT重連失敗", e)
        }
    }
    
    /**
     * 斷開MQTT連接
     */
    fun disconnect() {
        try {
            mqttClient?.disconnect()
            logger.info("MQTT連接已斷開")
        } catch (e: Exception) {
            logger.error("斷開MQTT連接失敗", e)
        }
    }
    
    /**
     * 獲取設備在線狀態
     */
    fun getDeviceStatus(): Map<String, Boolean> = deviceStatus.toMap()
}