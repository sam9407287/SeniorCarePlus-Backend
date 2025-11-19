package com.seniorcareplus.utils

import com.seniorcareplus.models.*
import kotlinx.serialization.json.*

/**
 * 數據轉換幫助工具
 * 將平坦化的前端格式轉換為資料庫存儲格式（JSON 字符串）
 */
object DataTransformationHelper {
    
    /**
     * 將 CreateAnchorRequest 轉換為存儲用的 JSON 字符串
     * 返回：{ cloudData: {...所有cloudData加前綴字段...} } 的 JSON 字符串
     */
    fun convertCreateAnchorRequestToCloudDataJson(request: CreateAnchorRequest): String? {
        // 如果沒有任何 cloudData 加前綴字段，返回 null
        val hasCloudData = listOfNotNull(
            request.cloudDataContent,
            request.cloudDataGatewayId,
            request.cloudDataNode,
            request.cloudDataName,
            request.cloudDataId,
            request.cloudDataFwUpdate,
            request.cloudDataLed,
            request.cloudDataBle,
            request.cloudDataInitiator,
            request.cloudDataReceivedAt,
            request.cloudDataPosition
        ).isNotEmpty()
        
        if (!hasCloudData) return null
        
        // 構建 cloudData 對象
        val cloudDataMap = mutableMapOf<String, Any?>()
        
        // 添加標量值字段
        request.cloudDataContent?.let { cloudDataMap["content"] = it }
        request.cloudDataGatewayId?.let { cloudDataMap["gateway_id"] = it }
        request.cloudDataNode?.let { cloudDataMap["node"] = it }
        request.cloudDataName?.let { cloudDataMap["name"] = it }
        request.cloudDataId?.let { cloudDataMap["id"] = it }
        request.cloudDataFwUpdate?.let { cloudDataMap["fw_update"] = it }
        request.cloudDataLed?.let { cloudDataMap["led"] = it }
        request.cloudDataBle?.let { cloudDataMap["ble"] = it }
        request.cloudDataInitiator?.let { cloudDataMap["initiator"] = it }
        request.cloudDataReceivedAt?.let { cloudDataMap["receivedAt"] = it }
        
        // 添加位置字段（物件）
        request.cloudDataPosition?.let { 
            cloudDataMap["position"] = mapOf(
                "x" to it.x,
                "y" to it.y,
                "z" to it.z
            )
        }
        
        // 轉換為 JSON 字符串
        return Json.encodeToString(cloudDataMap)
    }
    
    /**
     * 將 UpdateAnchorRequest 轉換為存儲用的 JSON 字符串
     */
    fun convertUpdateAnchorRequestToCloudDataJson(request: UpdateAnchorRequest): String? {
        val hasCloudData = listOfNotNull(
            request.cloudDataContent,
            request.cloudDataGatewayId,
            request.cloudDataNode,
            request.cloudDataName,
            request.cloudDataId,
            request.cloudDataFwUpdate,
            request.cloudDataLed,
            request.cloudDataBle,
            request.cloudDataInitiator,
            request.cloudDataReceivedAt,
            request.cloudDataPosition
        ).isNotEmpty()
        
        if (!hasCloudData) return null
        
        val cloudDataMap = mutableMapOf<String, Any?>()
        
        request.cloudDataContent?.let { cloudDataMap["content"] = it }
        request.cloudDataGatewayId?.let { cloudDataMap["gateway_id"] = it }
        request.cloudDataNode?.let { cloudDataMap["node"] = it }
        request.cloudDataName?.let { cloudDataMap["name"] = it }
        request.cloudDataId?.let { cloudDataMap["id"] = it }
        request.cloudDataFwUpdate?.let { cloudDataMap["fw_update"] = it }
        request.cloudDataLed?.let { cloudDataMap["led"] = it }
        request.cloudDataBle?.let { cloudDataMap["ble"] = it }
        request.cloudDataInitiator?.let { cloudDataMap["initiator"] = it }
        request.cloudDataReceivedAt?.let { cloudDataMap["receivedAt"] = it }
        
        request.cloudDataPosition?.let { 
            cloudDataMap["position"] = mapOf(
                "x" to it.x,
                "y" to it.y,
                "z" to it.z
            )
        }
        
        return Json.encodeToString(cloudDataMap)
    }
    
    /**
     * 將 CreateGatewayRequest 轉換為存儲用的 JSON 字符串
     */
    fun convertCreateGatewayRequestToCloudDataJson(request: CreateGatewayRequest): String? {
        val hasCloudData = listOfNotNull(
            request.cloudDataContent,
            request.cloudDataGatewayId,
            request.cloudDataFwVer,
            request.cloudDataFwSerial,
            request.cloudDataUwbHwComOk,
            request.cloudDataUwbJoined,
            request.cloudDataUwbNetworkId,
            request.cloudDataConnectedAp,
            request.cloudDataWifiTxPower,
            request.cloudDataSetWifiMaxTxPower,
            request.cloudDataBleScanTime,
            request.cloudDataBleScanPauseTime,
            request.cloudDataBatteryVoltage,
            request.cloudDataFiveVPlugged,
            request.cloudDataUwbTxPowerChanged,
            request.cloudDataDiscardIotDataTime,
            request.cloudDataDiscardedIotData,
            request.cloudDataTotalDiscardedData,
            request.cloudDataFirstSync,
            request.cloudDataLastSync,
            request.cloudDataCurrent,
            request.cloudDataReceivedAt,
            request.cloudDataUwbTxPower,
            request.cloudDataPubTopic,
            request.cloudDataSubTopic
        ).isNotEmpty()
        
        if (!hasCloudData) return null
        
        val cloudDataMap = mutableMapOf<String, Any?>()
        
        request.cloudDataContent?.let { cloudDataMap["content"] = it }
        request.cloudDataGatewayId?.let { cloudDataMap["gateway_id"] = it }
        request.cloudDataFwVer?.let { cloudDataMap["fw_ver"] = it }
        request.cloudDataFwSerial?.let { cloudDataMap["fw_serial"] = it }
        request.cloudDataUwbHwComOk?.let { cloudDataMap["uwb_hw_com_ok"] = it }
        request.cloudDataUwbJoined?.let { cloudDataMap["uwb_joined"] = it }
        request.cloudDataUwbNetworkId?.let { cloudDataMap["uwb_network_id"] = it }
        request.cloudDataConnectedAp?.let { cloudDataMap["connected_ap"] = it }
        request.cloudDataWifiTxPower?.let { cloudDataMap["wifi_tx_power"] = it }
        request.cloudDataSetWifiMaxTxPower?.let { cloudDataMap["set_wifi_max_tx_power"] = it }
        request.cloudDataBleScanTime?.let { cloudDataMap["ble_scan_time"] = it }
        request.cloudDataBleScanPauseTime?.let { cloudDataMap["ble_scan_pause_time"] = it }
        request.cloudDataBatteryVoltage?.let { cloudDataMap["battery_voltage"] = it }
        request.cloudDataFiveVPlugged?.let { cloudDataMap["five_v_plugged"] = it }
        request.cloudDataUwbTxPowerChanged?.let { cloudDataMap["uwb_tx_power_changed"] = it }
        request.cloudDataDiscardIotDataTime?.let { cloudDataMap["discard_iot_data_time"] = it }
        request.cloudDataDiscardedIotData?.let { cloudDataMap["discarded_iot_data"] = it }
        request.cloudDataTotalDiscardedData?.let { cloudDataMap["total_discarded_data"] = it }
        request.cloudDataFirstSync?.let { cloudDataMap["first_sync"] = it }
        request.cloudDataLastSync?.let { cloudDataMap["last_sync"] = it }
        request.cloudDataCurrent?.let { cloudDataMap["current"] = it }
        request.cloudDataReceivedAt?.let { cloudDataMap["receivedAt"] = it }
        request.cloudDataUwbTxPower?.let { cloudDataMap["uwb_tx_power"] = it }
        request.cloudDataPubTopic?.let { cloudDataMap["pub_topic"] = it }
        request.cloudDataSubTopic?.let { cloudDataMap["sub_topic"] = it }
        
        return Json.encodeToString(cloudDataMap)
    }
    
    /**
     * 將存儲的 JSON（cloudData）轉換為 GatewayData（返回給前端）
     */
    fun convertStoredJsonToGatewayData(
        id: String,
        floorId: String,
        name: String,
        macAddress: String,
        ipAddress: String? = null,
        firmwareVersion: String? = null,
        cloudDataJson: String?,
        status: String = "offline",
        lastSeen: String? = null,
        createdAt: String
    ): GatewayData {
        val gateway = GatewayData(
            id = id,
            floorId = floorId,
            name = name,
            macAddress = macAddress,
            ipAddress = ipAddress,
            firmwareVersion = firmwareVersion,
            status = status,
            lastSeen = lastSeen,
            createdAt = createdAt
        )
        
        // 解析 cloudData JSON 並轉換為加前綴格式
        if (cloudDataJson != null) {
            return try {
                val cloudData = Json.decodeFromString<Map<String, Any?>>(cloudDataJson)
                convertCloudDataToGatewayDataWithPrefix(gateway, cloudData)
            } catch (e: Exception) {
                gateway
            }
        }
        
        return gateway
    }
    
    /**
     * 將存儲的 JSON（cloudData）轉換為 AnchorData（返回給前端）
     */
    fun convertStoredJsonToAnchorData(
        id: String,
        name: String,
        macAddress: String,
        position: PositionData,
        cloudDataJson: String?,
        gatewayId: String? = null,
        homeId: String? = null,
        floorId: String? = null,
        status: String = "offline",
        lastSeen: String? = null,
        isBound: Boolean = false,
        createdAt: String
    ): AnchorData {
        val anchor = AnchorData(
            id = id,
            gatewayId = gatewayId,
            homeId = homeId,
            floorId = floorId,
            name = name,
            macAddress = macAddress,
            position = position,
            status = status,
            lastSeen = lastSeen,
            isBound = isBound,
            createdAt = createdAt
        )
        
        // 解析 cloudData JSON 並轉換為加前綴格式
        if (cloudDataJson != null) {
            return try {
                val cloudData = Json.decodeFromString<Map<String, Any?>>(cloudDataJson)
                convertCloudDataToAnchorDataWithPrefix(anchor, cloudData)
            } catch (e: Exception) {
                anchor
            }
        }
        
        return anchor
    }
    
    /**
     * 內部幫助函數：將存儲的 cloudData 轉換為帶前綴的 GatewayData
     */
    private fun convertCloudDataToGatewayDataWithPrefix(
        gateway: GatewayData,
        cloudData: Map<String, Any?>
    ): GatewayData {
        return gateway.copy(
            cloudDataContent = (cloudData["content"] as? String),
            cloudDataGatewayId = (cloudData["gateway_id"] as? Number)?.toInt(),
            cloudDataFwVer = (cloudData["fw_ver"] as? String),
            cloudDataFwSerial = (cloudData["fw_serial"] as? Number)?.toInt(),
            cloudDataUwbHwComOk = (cloudData["uwb_hw_com_ok"] as? String),
            cloudDataUwbJoined = (cloudData["uwb_joined"] as? String),
            cloudDataUwbNetworkId = (cloudData["uwb_network_id"] as? Number)?.toInt(),
            cloudDataConnectedAp = (cloudData["connected_ap"] as? String),
            cloudDataWifiTxPower = (cloudData["wifi_tx_power"] as? Number)?.toInt(),
            cloudDataSetWifiMaxTxPower = (cloudData["set_wifi_max_tx_power"] as? Number)?.toDouble(),
            cloudDataBleScanTime = (cloudData["ble_scan_time"] as? Number)?.toInt(),
            cloudDataBleScanPauseTime = (cloudData["ble_scan_pause_time"] as? Number)?.toInt(),
            cloudDataBatteryVoltage = (cloudData["battery_voltage"] as? Number)?.toDouble(),
            cloudDataFiveVPlugged = (cloudData["five_v_plugged"] as? String),
            cloudDataUwbTxPowerChanged = (cloudData["uwb_tx_power_changed"] as? String),
            cloudDataDiscardIotDataTime = (cloudData["discard_iot_data_time"] as? Number)?.toInt(),
            cloudDataDiscardedIotData = (cloudData["discarded_iot_data"] as? Number)?.toInt(),
            cloudDataTotalDiscardedData = (cloudData["total_discarded_data"] as? Number)?.toInt(),
            cloudDataFirstSync = (cloudData["first_sync"] as? String),
            cloudDataLastSync = (cloudData["last_sync"] as? String),
            cloudDataCurrent = (cloudData["current"] as? String),
            cloudDataReceivedAt = (cloudData["receivedAt"] as? String),
            cloudDataUwbTxPower = (cloudData["uwb_tx_power"] as? Map<String, Double>),
            cloudDataPubTopic = (cloudData["pub_topic"] as? Map<String, String>),
            cloudDataSubTopic = (cloudData["sub_topic"] as? Map<String, String>)
        )
    }
    
    /**
     * 內部幫助函數：將存儲的 cloudData 轉換為帶前綴的 AnchorData
     */
    private fun convertCloudDataToAnchorDataWithPrefix(
        anchor: AnchorData,
        cloudData: Map<String, Any?>
    ): AnchorData {
        return anchor.copy(
            cloudData = AnchorCloudData(
                id = (cloudData["id"] as? Number)?.toInt(),
                gatewayId = (cloudData["gateway_id"] as? Number)?.toInt(),
                node = (cloudData["node"] as? String),
                name = (cloudData["name"] as? String),
                content = (cloudData["content"] as? String),
                fwUpdate = (cloudData["fw_update"] as? Number)?.toInt(),
                led = (cloudData["led"] as? Number)?.toInt(),
                ble = (cloudData["ble"] as? Number)?.toInt(),
                initiator = (cloudData["initiator"] as? Number)?.toInt(),
                position = (cloudData["position"] as? Map<String, Number>)?.let {
                    PositionData(
                        x = (it["x"] as? Number)?.toDouble() ?: 0.0,
                        y = (it["y"] as? Number)?.toDouble() ?: 0.0,
                        z = (it["z"] as? Number)?.toDouble() ?: 0.0
                    )
                },
                receivedAt = (cloudData["receivedAt"] as? String)
            )
        )
    }
}

