package com.seniorcareplus.services

import com.seniorcareplus.models.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.slf4j.LoggerFactory
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.UUID

/**
 * 位置服務類，負責處理位置數據的存儲和檢索
 */
class LocationService {
    private val logger = LoggerFactory.getLogger(LocationService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    
    // 內存存儲 (生產環境應使用PostgreSQL)
    private val currentLocations = ConcurrentHashMap<String, LocationData>()
    private val locationHistory = CopyOnWriteArrayList<LocationHistory>()
    private val gateways = ConcurrentHashMap<String, GatewayInfo>()
    private val anchors = ConcurrentHashMap<String, AnchorDevice>()
    private val tags = ConcurrentHashMap<String, TagDevice>()
    
    // WebSocket連接管理
    private val webSocketManager = WebSocketManager.getInstance()
    
    init {
        // 初始化一些測試數據
        initializeTestData()
    }
    
    /**
     * 更新設備位置
     */
    suspend fun updateDeviceLocation(locationData: LocationData) {
        try {
            // 更新當前位置
            currentLocations[locationData.deviceId] = locationData
            
            // 添加到歷史記錄
            val historyRecord = LocationHistory(
                id = UUID.randomUUID().toString(),
                deviceId = locationData.deviceId,
                patientId = locationData.patientId,
                x = locationData.x,
                y = locationData.y,
                z = locationData.z,
                floor = locationData.floor,
                batteryLevel = locationData.batteryLevel,
                signal_strength = locationData.signal_strength,
                timestamp = locationData.timestamp
            )
            locationHistory.add(historyRecord)
            
            // 清理舊的歷史記錄 (保留最近1000條)
            if (locationHistory.size > 1000) {
                locationHistory.removeFirst()
            }
            
            // 更新Tag設備信息
            val tag = tags[locationData.deviceId]
            if (tag != null) {
                tags[tag.tagId] = tag.copy(
                    currentLocation = locationData,
                    batteryLevel = locationData.batteryLevel,
                    lastSeen = System.currentTimeMillis(),
                    status = "active"
                )
            }
            
            // 實時推送位置更新到WebSocket客戶端
            val updateMessage = mapOf(
                "type" to "location_update",
                "data" to locationData,
                "timestamp" to System.currentTimeMillis()
            )
            
            webSocketManager.broadcastToLocationClients(json.encodeToString(updateMessage))
            
            logger.info("位置數據更新成功: 設備=${locationData.deviceId}, 位置=(${locationData.x}, ${locationData.y})")
            
        } catch (e: Exception) {
            logger.error("更新位置數據失敗: ${locationData.deviceId}", e)
            throw e
        }
    }
    
    /**
     * 獲取所有設備當前位置
     */
    fun getAllDeviceLocations(): List<LocationData> {
        return currentLocations.values.toList()
    }
    
    /**
     * 獲取特定設備當前位置
     */
    fun getDeviceLocation(deviceId: String): LocationData? {
        return currentLocations[deviceId]
    }
    
    /**
     * 獲取設備位置歷史
     */
    fun getLocationHistory(
        deviceId: String,
        fromTimestamp: Long? = null,
        toTimestamp: Long? = null,
        limit: Int = 100
    ): List<LocationHistory> {
        return locationHistory
            .filter { it.deviceId == deviceId }
            .filter { history ->
                (fromTimestamp == null || history.timestamp >= fromTimestamp) &&
                (toTimestamp == null || history.timestamp <= toTimestamp)
            }
            .sortedByDescending { it.timestamp }
            .take(limit)
    }
    
    /**
     * 獲取所有Gateway
     */
    fun getAllGateways(): List<GatewayInfo> {
        return gateways.values.toList()
    }
    
    /**
     * 獲取所有Anchor設備
     */
    fun getAllAnchors(): List<AnchorDevice> {
        return anchors.values.toList()
    }
    
    /**
     * 獲取所有Tag設備
     */
    fun getAllTags(): List<TagDevice> {
        return tags.values.toList()
    }
    
    /**
     * 更新Gateway狀態
     */
    suspend fun updateGatewayStatus(gatewayId: String, status: String, connectedDevices: Int = 0) {
        val gateway = gateways[gatewayId]
        if (gateway != null) {
            gateways[gatewayId] = gateway.copy(
                status = status,
                lastSeen = System.currentTimeMillis(),
                connectedDevices = connectedDevices
            )
            
            // 廣播Gateway狀態更新
            val updateMessage = mapOf(
                "type" to "gateway_status_update",
                "data" to gateways[gatewayId],
                "timestamp" to System.currentTimeMillis()
            )
            
            webSocketManager.broadcastToLocationClients(json.encodeToString(updateMessage))
        }
    }
    
    /**
     * 更新設備電池狀態
     */
    suspend fun updateDeviceBattery(deviceId: String, batteryLevel: Int) {
        // 更新當前位置的電池信息
        val currentLocation = currentLocations[deviceId]
        if (currentLocation != null) {
            currentLocations[deviceId] = currentLocation.copy(batteryLevel = batteryLevel)
        }
        
        // 更新Tag設備的電池信息
        val tag = tags[deviceId]
        if (tag != null) {
            tags[deviceId] = tag.copy(
                batteryLevel = batteryLevel,
                lastSeen = System.currentTimeMillis()
            )
        }
        
        // 廣播電池狀態更新
        val updateMessage = mapOf(
            "type" to "battery_update",
            "data" to mapOf(
                "deviceId" to deviceId,
                "batteryLevel" to batteryLevel
            ),
            "timestamp" to System.currentTimeMillis()
        )
        
        webSocketManager.broadcastToLocationClients(json.encodeToString(updateMessage))
    }
    
    /**
     * 初始化測試數據
     */
    private fun initializeTestData() {
        // 測試Gateway
        gateways["GW001"] = GatewayInfo(
            gatewayId = "GW001",
            name = "主閘道器",
            status = "online",
            ipAddress = "192.168.1.100",
            lastSeen = System.currentTimeMillis(),
            connectedDevices = 8
        )
        
        // 測試Anchor設備
        anchors["U001"] = AnchorDevice(
            anchorId = "U001",
            gatewayId = "GW001",
            x = 0.0,
            y = 0.0,
            z = 2.5,
            status = "active",
            batteryLevel = 95,
            lastSeen = System.currentTimeMillis()
        )
        
        anchors["U002"] = AnchorDevice(
            anchorId = "U002",
            gatewayId = "GW001",
            x = 10.0,
            y = 0.0,
            z = 2.5,
            status = "active",
            batteryLevel = 88,
            lastSeen = System.currentTimeMillis()
        )
        
        anchors["U003"] = AnchorDevice(
            anchorId = "U003",
            gatewayId = "GW001",
            x = 10.0,
            y = 8.0,
            z = 2.5,
            status = "active",
            batteryLevel = 92,
            lastSeen = System.currentTimeMillis()
        )
        
        anchors["U004"] = AnchorDevice(
            anchorId = "U004",
            gatewayId = "GW001",
            x = 0.0,
            y = 8.0,
            z = 2.5,
            status = "active",
            batteryLevel = 87,
            lastSeen = System.currentTimeMillis()
        )
        
        // 測試Tag設備
        tags["E001"] = TagDevice(
            tagId = "E001",
            gatewayId = "GW001",
            patientId = "P001",
            currentLocation = null,
            status = "active",
            batteryLevel = 75,
            lastSeen = System.currentTimeMillis(),
            config = TagConfig()
        )
        
        tags["E002"] = TagDevice(
            tagId = "E002",
            gatewayId = "GW001",
            patientId = "P002",
            currentLocation = null,
            status = "active",
            batteryLevel = 82,
            lastSeen = System.currentTimeMillis(),
            config = TagConfig()
        )
        
        logger.info("測試數據初始化完成")
    }
} 