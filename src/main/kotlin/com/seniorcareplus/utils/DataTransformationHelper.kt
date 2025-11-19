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
     */
    fun convertCreateAnchorRequestToCloudDataJson(request: CreateAnchorRequest): String? {
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
        
        val cloudDataMap = buildJsonObject {
            request.cloudDataContent?.let { put("content", it) }
            request.cloudDataGatewayId?.let { put("gateway_id", it) }
            request.cloudDataNode?.let { put("node", it) }
            request.cloudDataName?.let { put("name", it) }
            request.cloudDataId?.let { put("id", it) }
            request.cloudDataFwUpdate?.let { put("fw_update", it) }
            request.cloudDataLed?.let { put("led", it) }
            request.cloudDataBle?.let { put("ble", it) }
            request.cloudDataInitiator?.let { put("initiator", it) }
            request.cloudDataReceivedAt?.let { put("receivedAt", it) }
            
            request.cloudDataPosition?.let { 
                putJsonObject("position") {
                    put("x", it.x)
                    put("y", it.y)
                    put("z", it.z)
                }
            }
        }
        
        return cloudDataMap.toString()
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
        
        val cloudDataMap = buildJsonObject {
            request.cloudDataContent?.let { put("content", it) }
            request.cloudDataGatewayId?.let { put("gateway_id", it) }
            request.cloudDataNode?.let { put("node", it) }
            request.cloudDataName?.let { put("name", it) }
            request.cloudDataId?.let { put("id", it) }
            request.cloudDataFwUpdate?.let { put("fw_update", it) }
            request.cloudDataLed?.let { put("led", it) }
            request.cloudDataBle?.let { put("ble", it) }
            request.cloudDataInitiator?.let { put("initiator", it) }
            request.cloudDataReceivedAt?.let { put("receivedAt", it) }
            
            request.cloudDataPosition?.let { 
                putJsonObject("position") {
                    put("x", it.x)
                    put("y", it.y)
                    put("z", it.z)
                }
            }
        }
        
        return cloudDataMap.toString()
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
        
        val cloudDataMap = buildJsonObject {
            request.cloudDataContent?.let { put("content", it) }
            request.cloudDataGatewayId?.let { put("gateway_id", it) }
            request.cloudDataFwVer?.let { put("fw_ver", it) }
            request.cloudDataFwSerial?.let { put("fw_serial", it) }
            request.cloudDataUwbHwComOk?.let { put("uwb_hw_com_ok", it) }
            request.cloudDataUwbJoined?.let { put("uwb_joined", it) }
            request.cloudDataUwbNetworkId?.let { put("uwb_network_id", it) }
            request.cloudDataConnectedAp?.let { put("connected_ap", it) }
            request.cloudDataWifiTxPower?.let { put("wifi_tx_power", it) }
            request.cloudDataSetWifiMaxTxPower?.let { put("set_wifi_max_tx_power", it) }
            request.cloudDataBleScanTime?.let { put("ble_scan_time", it) }
            request.cloudDataBleScanPauseTime?.let { put("ble_scan_pause_time", it) }
            request.cloudDataBatteryVoltage?.let { put("battery_voltage", it) }
            request.cloudDataFiveVPlugged?.let { put("five_v_plugged", it) }
            request.cloudDataUwbTxPowerChanged?.let { put("uwb_tx_power_changed", it) }
            request.cloudDataDiscardIotDataTime?.let { put("discard_iot_data_time", it) }
            request.cloudDataDiscardedIotData?.let { put("discarded_iot_data", it) }
            request.cloudDataTotalDiscardedData?.let { put("total_discarded_data", it) }
            request.cloudDataFirstSync?.let { put("first_sync", it) }
            request.cloudDataLastSync?.let { put("last_sync", it) }
            request.cloudDataCurrent?.let { put("current", it) }
            request.cloudDataReceivedAt?.let { put("receivedAt", it) }
            
            request.cloudDataUwbTxPower?.let { 
                putJsonObject("uwb_tx_power") {
                    it.forEach { (k, v) -> put(k, v) }
                }
            }
            request.cloudDataPubTopic?.let { 
                putJsonObject("pub_topic") {
                    it.forEach { (k, v) -> put(k, v) }
                }
            }
            request.cloudDataSubTopic?.let { 
                putJsonObject("sub_topic") {
                    it.forEach { (k, v) -> put(k, v) }
                }
            }
        }
        
        return cloudDataMap.toString()
    }
    
    /**
     * 將 UpdateGatewayRequest 轉換為存儲用的 JSON 字符串
     */
    fun convertUpdateGatewayRequestToCloudDataJson(request: UpdateGatewayRequest): String? {
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
        
        val cloudDataMap = buildJsonObject {
            request.cloudDataContent?.let { put("content", it) }
            request.cloudDataGatewayId?.let { put("gateway_id", it) }
            request.cloudDataFwVer?.let { put("fw_ver", it) }
            request.cloudDataFwSerial?.let { put("fw_serial", it) }
            request.cloudDataUwbHwComOk?.let { put("uwb_hw_com_ok", it) }
            request.cloudDataUwbJoined?.let { put("uwb_joined", it) }
            request.cloudDataUwbNetworkId?.let { put("uwb_network_id", it) }
            request.cloudDataConnectedAp?.let { put("connected_ap", it) }
            request.cloudDataWifiTxPower?.let { put("wifi_tx_power", it) }
            request.cloudDataSetWifiMaxTxPower?.let { put("set_wifi_max_tx_power", it) }
            request.cloudDataBleScanTime?.let { put("ble_scan_time", it) }
            request.cloudDataBleScanPauseTime?.let { put("ble_scan_pause_time", it) }
            request.cloudDataBatteryVoltage?.let { put("battery_voltage", it) }
            request.cloudDataFiveVPlugged?.let { put("five_v_plugged", it) }
            request.cloudDataUwbTxPowerChanged?.let { put("uwb_tx_power_changed", it) }
            request.cloudDataDiscardIotDataTime?.let { put("discard_iot_data_time", it) }
            request.cloudDataDiscardedIotData?.let { put("discarded_iot_data", it) }
            request.cloudDataTotalDiscardedData?.let { put("total_discarded_data", it) }
            request.cloudDataFirstSync?.let { put("first_sync", it) }
            request.cloudDataLastSync?.let { put("last_sync", it) }
            request.cloudDataCurrent?.let { put("current", it) }
            request.cloudDataReceivedAt?.let { put("receivedAt", it) }
            
            request.cloudDataUwbTxPower?.let { 
                putJsonObject("uwb_tx_power") {
                    it.forEach { (k, v) -> put(k, v) }
                }
            }
            request.cloudDataPubTopic?.let { 
                putJsonObject("pub_topic") {
                    it.forEach { (k, v) -> put(k, v) }
                }
            }
            request.cloudDataSubTopic?.let { 
                putJsonObject("sub_topic") {
                    it.forEach { (k, v) -> put(k, v) }
                }
            }
        }
        
        return cloudDataMap.toString()
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
        
        if (cloudDataJson != null) {
            return try {
                val cloudData = Json.decodeFromString<JsonObject>(cloudDataJson)
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
        
        if (cloudDataJson != null) {
            return try {
                val cloudData = Json.decodeFromString<JsonObject>(cloudDataJson)
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
        cloudData: JsonObject
    ): GatewayData {
        return gateway.copy(
            cloudDataContent = cloudData["content"]?.jsonPrimitive?.contentOrNull,
            cloudDataGatewayId = cloudData["gateway_id"]?.jsonPrimitive?.intOrNull,
            cloudDataFwVer = cloudData["fw_ver"]?.jsonPrimitive?.contentOrNull,
            cloudDataFwSerial = cloudData["fw_serial"]?.jsonPrimitive?.intOrNull,
            cloudDataUwbHwComOk = cloudData["uwb_hw_com_ok"]?.jsonPrimitive?.contentOrNull,
            cloudDataUwbJoined = cloudData["uwb_joined"]?.jsonPrimitive?.contentOrNull,
            cloudDataUwbNetworkId = cloudData["uwb_network_id"]?.jsonPrimitive?.intOrNull,
            cloudDataConnectedAp = cloudData["connected_ap"]?.jsonPrimitive?.contentOrNull,
            cloudDataWifiTxPower = cloudData["wifi_tx_power"]?.jsonPrimitive?.intOrNull,
            cloudDataSetWifiMaxTxPower = cloudData["set_wifi_max_tx_power"]?.jsonPrimitive?.doubleOrNull,
            cloudDataBleScanTime = cloudData["ble_scan_time"]?.jsonPrimitive?.intOrNull,
            cloudDataBleScanPauseTime = cloudData["ble_scan_pause_time"]?.jsonPrimitive?.intOrNull,
            cloudDataBatteryVoltage = cloudData["battery_voltage"]?.jsonPrimitive?.doubleOrNull,
            cloudDataFiveVPlugged = cloudData["five_v_plugged"]?.jsonPrimitive?.contentOrNull,
            cloudDataUwbTxPowerChanged = cloudData["uwb_tx_power_changed"]?.jsonPrimitive?.contentOrNull,
            cloudDataDiscardIotDataTime = cloudData["discard_iot_data_time"]?.jsonPrimitive?.intOrNull,
            cloudDataDiscardedIotData = cloudData["discarded_iot_data"]?.jsonPrimitive?.intOrNull,
            cloudDataTotalDiscardedData = cloudData["total_discarded_data"]?.jsonPrimitive?.intOrNull,
            cloudDataFirstSync = cloudData["first_sync"]?.jsonPrimitive?.contentOrNull,
            cloudDataLastSync = cloudData["last_sync"]?.jsonPrimitive?.contentOrNull,
            cloudDataCurrent = cloudData["current"]?.jsonPrimitive?.contentOrNull,
            cloudDataReceivedAt = cloudData["receivedAt"]?.jsonPrimitive?.contentOrNull,
            cloudDataUwbTxPower = cloudData["uwb_tx_power"]?.jsonObject?.let { obj ->
                obj.mapValues { (_, v) -> v.jsonPrimitive.double }
            },
            cloudDataPubTopic = cloudData["pub_topic"]?.jsonObject?.let { obj ->
                obj.mapValues { (_, v) -> v.jsonPrimitive.content }
            },
            cloudDataSubTopic = cloudData["sub_topic"]?.jsonObject?.let { obj ->
                obj.mapValues { (_, v) -> v.jsonPrimitive.content }
            }
        )
    }
    
    /**
     * 內部幫助函數：將存儲的 cloudData 轉換為帶前綴的 AnchorData
     */
    private fun convertCloudDataToAnchorDataWithPrefix(
        anchor: AnchorData,
        cloudData: JsonObject
    ): AnchorData {
        return anchor.copy(
            cloudData = AnchorCloudData(
                id = cloudData["id"]?.jsonPrimitive?.intOrNull,
                gatewayId = cloudData["gateway_id"]?.jsonPrimitive?.intOrNull,
                node = cloudData["node"]?.jsonPrimitive?.contentOrNull,
                name = cloudData["name"]?.jsonPrimitive?.contentOrNull,
                content = cloudData["content"]?.jsonPrimitive?.contentOrNull,
                fwUpdate = cloudData["fw_update"]?.jsonPrimitive?.intOrNull,
                led = cloudData["led"]?.jsonPrimitive?.intOrNull,
                ble = cloudData["ble"]?.jsonPrimitive?.intOrNull,
                initiator = cloudData["initiator"]?.jsonPrimitive?.intOrNull,
                position = cloudData["position"]?.jsonObject?.let {
                    PositionData(
                        x = it["x"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        y = it["y"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                        z = it["z"]?.jsonPrimitive?.doubleOrNull ?: 0.0
                    )
                },
                receivedAt = cloudData["receivedAt"]?.jsonPrimitive?.contentOrNull
            )
        )
    }
}
