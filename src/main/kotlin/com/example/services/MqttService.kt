package com.example.services

import com.example.models.LocationData
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.eclipse.paho.client.mqttv3.*
import org.slf4j.LoggerFactory
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * MQTT服务类，用于连接MQTT代理并接收UWB定位数据
 */
class MqttService {
    private val logger = LoggerFactory.getLogger(MqttService::class.java)
    private lateinit var mqttClient: MqttClient
    
    // 存储最新的位置数据，deviceId -> LocationData
    private val latestLocations = ConcurrentHashMap<String, LocationData>()
    
    // 位置更新监听器列表
    private val locationListeners = Collections.synchronizedList(mutableListOf<(LocationData) -> Unit>())
    
    /**
     * 连接到MQTT代理服务器
     */
    fun connect(
        brokerUrl: String = "tcp://localhost:1883",
        clientId: String = "ktor-backend-${System.currentTimeMillis()}"
    ) {
        try {
            logger.info("正在连接到MQTT代理: $brokerUrl")
            mqttClient = MqttClient(brokerUrl, clientId)
            
            // 设置连接选项
            val options = MqttConnectOptions().apply {
                isCleanSession = true
                connectionTimeout = 30
                keepAliveInterval = 60
            }
            
            // 设置回调
            mqttClient.setCallback(object : MqttCallback {
                override fun connectionLost(cause: Throwable?) {
                    logger.error("MQTT连接丢失: ${cause?.message}")
                    // 尝试重新连接
                    Thread.sleep(5000)
                    try {
                        mqttClient.connect(options)
                        subscribeToTopics()
                    } catch (e: Exception) {
                        logger.error("重连失败: ${e.message}")
                    }
                }
                
                override fun messageArrived(topic: String, message: MqttMessage) {
                    handleIncomingMessage(topic, message)
                }
                
                override fun deliveryComplete(token: IMqttDeliveryToken?) {
                    // 消息发送完成的回调，暂时不需要处理
                }
            })
            
            // 连接到代理
            mqttClient.connect(options)
            logger.info("已成功连接到MQTT代理")
            
            // 订阅主题
            subscribeToTopics()
        } catch (e: Exception) {
            logger.error("MQTT连接失败: ${e.message}")
            throw e
        }
    }
    
    /**
     * 订阅相关主题
     */
    private fun subscribeToTopics() {
        // 订阅UWB定位数据主题
        mqttClient.subscribe("uwb/location/#") { _, message ->
            val payload = String(message.payload)
            logger.debug("收到UWB定位数据: $payload")
            
            try {
                val locationData = Json.decodeFromString<LocationData>(payload)
                
                // 更新最新位置
                latestLocations[locationData.deviceId] = locationData
                
                // 通知所有监听器
                notifyListeners(locationData)
            } catch (e: Exception) {
                logger.error("解析位置数据失败: ${e.message}")
            }
        }
        
        logger.info("已订阅UWB定位数据主题")
    }
    
    /**
     * 处理接收到的MQTT消息
     */
    private fun handleIncomingMessage(topic: String, message: MqttMessage) {
        val payload = String(message.payload)
        logger.debug("收到主题 [$topic] 的消息: $payload")
        
        when {
            topic.startsWith("uwb/location/") -> {
                try {
                    val locationData = Json.decodeFromString<LocationData>(payload)
                    
                    // 更新最新位置
                    latestLocations[locationData.deviceId] = locationData
                    
                    // 通知所有监听器
                    notifyListeners(locationData)
                } catch (e: Exception) {
                    logger.error("解析位置数据失败: ${e.message}")
                }
            }
            // 可以添加其他主题的处理逻辑
        }
    }
    
    /**
     * 通知所有位置更新监听器
     */
    private fun notifyListeners(locationData: LocationData) {
        locationListeners.forEach { listener ->
            try {
                listener(locationData)
            } catch (e: Exception) {
                logger.error("通知监听器失败: ${e.message}")
            }
        }
    }
    
    /**
     * 添加位置更新监听器
     */
    fun addLocationListener(listener: (LocationData) -> Unit) {
        locationListeners.add(listener)
    }
    
    /**
     * 移除位置更新监听器
     */
    fun removeLocationListener(listener: (LocationData) -> Unit) {
        locationListeners.remove(listener)
    }
    
    /**
     * 获取设备的最新位置
     */
    fun getLatestLocation(deviceId: String): LocationData? {
        return latestLocations[deviceId]
    }
    
    /**
     * 获取所有设备的最新位置
     */
    fun getAllLatestLocations(): Map<String, LocationData> {
        return latestLocations.toMap()
    }
    
    /**
     * 断开MQTT连接
     */
    fun disconnect() {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            mqttClient.disconnect()
            logger.info("已断开MQTT连接")
        }
    }
} 