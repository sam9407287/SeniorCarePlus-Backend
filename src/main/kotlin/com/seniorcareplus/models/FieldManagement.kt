package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import java.time.LocalDateTime

/**
 * 場域數據模型 (Home)
 */
@Serializable
data class HomeData(
    val id: String,
    val name: String,
    val description: String? = null,
    val address: String? = null,
    val createdAt: String
)

/**
 * 創建場域請求
 */
@Serializable
data class CreateHomeRequest(
    val name: String,
    val description: String? = null,
    val address: String? = null
)

/**
 * 更新場域請求
 */
@Serializable
data class UpdateHomeRequest(
    val name: String? = null,
    val description: String? = null,
    val address: String? = null
)

/**
 * 樓層數據模型 (Floor)
 */
@Serializable
data class FloorData(
    val id: String,
    val homeId: String,
    val name: String,
    val level: Int,
    val mapImage: String? = null,
    val dimensions: DimensionsData? = null,
    val calibration: CalibrationData? = null,
    val createdAt: String
)

/**
 * 尺寸數據
 */
@Serializable
data class DimensionsData(
    val width: Double,
    val height: Double,
    val realWidth: Double,
    val realHeight: Double
)

/**
 * 校準數據
 */
@Serializable
data class CalibrationData(
    val originPixel: PixelPoint,
    val originCoordinates: CoordinatePoint? = null,
    val pixelToMeterRatio: Double,
    val scalePoints: ScalePoints? = null,
    val realDistance: Double? = null,
    val isCalibrated: Boolean
)

@Serializable
data class PixelPoint(
    val x: Double,
    val y: Double
)

@Serializable
data class CoordinatePoint(
    val x: Double,
    val y: Double
)

@Serializable
data class ScalePoints(
    val point1: PixelPoint?,
    val point2: PixelPoint?
)

/**
 * 創建樓層請求
 */
@Serializable
data class CreateFloorRequest(
    val homeId: String,
    val name: String,
    val level: Int,
    val mapImage: String? = null,
    val dimensions: DimensionsData? = null,
    val calibration: CalibrationData? = null
)

/**
 * 更新樓層請求
 */
@Serializable
data class UpdateFloorRequest(
    val name: String? = null,
    val level: Int? = null,
    val mapImage: String? = null,
    val dimensions: DimensionsData? = null,
    val calibration: CalibrationData? = null
)

/**
 * 網關數據模型 (Gateway) - v2 平坦化格式
 * 支援 cloudData 加前綴的格式（如 cloudDataGatewayId, cloudDataUwbTxPower 等）
 */
@Serializable
data class GatewayData(
    val id: String,
    val floorId: String,
    val name: String,
    val macAddress: String,
    val ipAddress: String? = null,
    val firmwareVersion: String? = null,
    val status: String = "offline",
    val lastSeen: String? = null,
    val createdAt: String,
    
    // cloudData 加前綴格式（標量值）
    val cloudDataContent: String? = null,
    val cloudDataGatewayId: Int? = null,
    val cloudDataFwVer: String? = null,
    val cloudDataFwSerial: Int? = null,
    val cloudDataUwbHwComOk: String? = null,
    val cloudDataUwbJoined: String? = null,
    val cloudDataUwbNetworkId: Int? = null,
    val cloudDataConnectedAp: String? = null,
    val cloudDataWifiTxPower: Int? = null,
    val cloudDataSetWifiMaxTxPower: Double? = null,
    val cloudDataBleScanTime: Int? = null,
    val cloudDataBleScanPauseTime: Int? = null,
    val cloudDataBatteryVoltage: Double? = null,
    val cloudDataFiveVPlugged: String? = null,
    val cloudDataUwbTxPowerChanged: String? = null,
    val cloudDataDiscardIotDataTime: Int? = null,
    val cloudDataDiscardedIotData: Int? = null,
    val cloudDataTotalDiscardedData: Int? = null,
    val cloudDataFirstSync: String? = null,
    val cloudDataLastSync: String? = null,
    val cloudDataCurrent: String? = null,
    val cloudDataReceivedAt: String? = null,
    
    // cloudData 加前綴格式（物件結構保留）
    val cloudDataUwbTxPower: Map<String, Double>? = null,
    val cloudDataPubTopic: Map<String, String>? = null,
    val cloudDataSubTopic: Map<String, String>? = null
)

/**
 * 創建網關請求 - v2 平坦化格式
 */
@Serializable
data class CreateGatewayRequest(
    val floorId: String,
    val name: String,
    val macAddress: String,
    val ipAddress: String? = null,
    val firmwareVersion: String? = null,
    val status: String? = null,
    
    // cloudData 加前綴格式（標量值）
    @SerialName("cloudDataContent")
    val cloudDataContent: String? = null,
    @SerialName("cloudDataGatewayId")
    val cloudDataGatewayId: Int? = null,
    @SerialName("cloudDataFwVer")
    val cloudDataFwVer: String? = null,
    @SerialName("cloudDataFwSerial")
    val cloudDataFwSerial: Int? = null,
    @SerialName("cloudDataUwbHwComOk")
    val cloudDataUwbHwComOk: String? = null,
    @SerialName("cloudDataUwbJoined")
    val cloudDataUwbJoined: String? = null,
    @SerialName("cloudDataUwbNetworkId")
    val cloudDataUwbNetworkId: Int? = null,
    @SerialName("cloudDataConnectedAp")
    val cloudDataConnectedAp: String? = null,
    @SerialName("cloudDataWifiTxPower")
    val cloudDataWifiTxPower: Int? = null,
    @SerialName("cloudDataSetWifiMaxTxPower")
    val cloudDataSetWifiMaxTxPower: Double? = null,
    @SerialName("cloudDataBleScanTime")
    val cloudDataBleScanTime: Int? = null,
    @SerialName("cloudDataBleScanPauseTime")
    val cloudDataBleScanPauseTime: Int? = null,
    @SerialName("cloudDataBatteryVoltage")
    val cloudDataBatteryVoltage: Double? = null,
    @SerialName("cloudDataFiveVPlugged")
    val cloudDataFiveVPlugged: String? = null,
    @SerialName("cloudDataUwbTxPowerChanged")
    val cloudDataUwbTxPowerChanged: String? = null,
    @SerialName("cloudDataDiscardIotDataTime")
    val cloudDataDiscardIotDataTime: Int? = null,
    @SerialName("cloudDataDiscardedIotData")
    val cloudDataDiscardedIotData: Int? = null,
    @SerialName("cloudDataTotalDiscardedData")
    val cloudDataTotalDiscardedData: Int? = null,
    @SerialName("cloudDataFirstSync")
    val cloudDataFirstSync: String? = null,
    @SerialName("cloudDataLastSync")
    val cloudDataLastSync: String? = null,
    @SerialName("cloudDataCurrent")
    val cloudDataCurrent: String? = null,
    @SerialName("cloudDataReceivedAt")
    val cloudDataReceivedAt: String? = null,
    
    // cloudData 加前綴格式（物件結構保留）
    @SerialName("cloudDataUwbTxPower")
    val cloudDataUwbTxPower: Map<String, Double>? = null,
    @SerialName("cloudDataPubTopic")
    val cloudDataPubTopic: Map<String, String>? = null,
    @SerialName("cloudDataSubTopic")
    val cloudDataSubTopic: Map<String, String>? = null
)

/**
 * 更新網關請求 - v2 平坦化格式
 */
@Serializable
data class UpdateGatewayRequest(
    val floorId: String? = null,
    val name: String? = null,
    val macAddress: String? = null,
    val ipAddress: String? = null,
    val firmwareVersion: String? = null,
    val status: String? = null,
    val lastSeen: String? = null,
    
    // cloudData 加前綴格式（標量值）
    @SerialName("cloudDataContent")
    val cloudDataContent: String? = null,
    @SerialName("cloudDataGatewayId")
    val cloudDataGatewayId: Int? = null,
    @SerialName("cloudDataFwVer")
    val cloudDataFwVer: String? = null,
    @SerialName("cloudDataFwSerial")
    val cloudDataFwSerial: Int? = null,
    @SerialName("cloudDataUwbHwComOk")
    val cloudDataUwbHwComOk: String? = null,
    @SerialName("cloudDataUwbJoined")
    val cloudDataUwbJoined: String? = null,
    @SerialName("cloudDataUwbNetworkId")
    val cloudDataUwbNetworkId: Int? = null,
    @SerialName("cloudDataConnectedAp")
    val cloudDataConnectedAp: String? = null,
    @SerialName("cloudDataWifiTxPower")
    val cloudDataWifiTxPower: Int? = null,
    @SerialName("cloudDataSetWifiMaxTxPower")
    val cloudDataSetWifiMaxTxPower: Double? = null,
    @SerialName("cloudDataBleScanTime")
    val cloudDataBleScanTime: Int? = null,
    @SerialName("cloudDataBleScanPauseTime")
    val cloudDataBleScanPauseTime: Int? = null,
    @SerialName("cloudDataBatteryVoltage")
    val cloudDataBatteryVoltage: Double? = null,
    @SerialName("cloudDataFiveVPlugged")
    val cloudDataFiveVPlugged: String? = null,
    @SerialName("cloudDataUwbTxPowerChanged")
    val cloudDataUwbTxPowerChanged: String? = null,
    @SerialName("cloudDataDiscardIotDataTime")
    val cloudDataDiscardIotDataTime: Int? = null,
    @SerialName("cloudDataDiscardedIotData")
    val cloudDataDiscardedIotData: Int? = null,
    @SerialName("cloudDataTotalDiscardedData")
    val cloudDataTotalDiscardedData: Int? = null,
    @SerialName("cloudDataFirstSync")
    val cloudDataFirstSync: String? = null,
    @SerialName("cloudDataLastSync")
    val cloudDataLastSync: String? = null,
    @SerialName("cloudDataCurrent")
    val cloudDataCurrent: String? = null,
    @SerialName("cloudDataReceivedAt")
    val cloudDataReceivedAt: String? = null,
    
    // cloudData 加前綴格式（物件結構保留）
    @SerialName("cloudDataUwbTxPower")
    val cloudDataUwbTxPower: Map<String, Double>? = null,
    @SerialName("cloudDataPubTopic")
    val cloudDataPubTopic: Map<String, String>? = null,
    @SerialName("cloudDataSubTopic")
    val cloudDataSubTopic: Map<String, String>? = null
)

/**
 * 錨點數據模型 (Anchor)
 * 當 Anchor 未綁定時，gatewayId/homeId/floorId 為 null
 */
@Serializable
data class AnchorData(
    val id: String,
    val gatewayId: String? = null,      // 綁定的 Gateway ID（未綁定時為 null）
    val homeId: String? = null,          // 反查：Gateway 所在的養老院
    val floorId: String? = null,         // 反查：Gateway 所在的樓層
    val name: String,
    val macAddress: String,
    val position: PositionData,
    val cloudData: AnchorCloudData? = null,
    val status: String = "offline",
    val lastSeen: String? = null,        // ✨ 新增：最後上線時間
    val isBound: Boolean = false,        // 是否已綁定到 Gateway
    val createdAt: String
)

/**
 * 位置數據
 */
@Serializable
data class PositionData(
    val x: Double,
    val y: Double,
    val z: Double
)

/**
 * 錨點數據 v2 - 平坦化格式
 * 支持 cloudData 加前綴的格式（如 cloudDataGatewayId, cloudDataPosition 等）
 */
@Serializable
data class AnchorCloudData(
    // 基本標識
    val id: Int? = null,
    val gatewayId: Int? = null,  // camelCase (from cloudDataGatewayId)
    val node: String? = null,
    val name: String? = null,
    val content: String? = null,
    
    // 配置字段
    val fwUpdate: Int? = null,
    val led: Int? = null,
    val ble: Int? = null,
    val initiator: Int? = null,
    
    // 位置信息（物件結構保留）
    val position: PositionData? = null,
    
    // 時間戳
    val receivedAt: String? = null
)

/**
 * 錨點配置信息
 */
@Serializable
data class AnchorConfig(
    val fw_update: Boolean? = null,
    val led: Boolean? = null,
    val ble: Boolean? = null,
    val initiator: Boolean? = null
)

/**
 * 創建錨點請求 - v2 平坦化格式
 * 接收前端發送的 cloudData 加前綴格式
 */
@Serializable
data class CreateAnchorRequest(
    val id: String? = null,
    val name: String,
    val macAddress: String,
    val position: PositionData? = null,
    val gatewayId: String? = null,
    val status: String? = null,
    val lastSeen: String? = null,
    
    // cloudData 加前綴字段（標量值）
    @SerialName("cloudDataContent")
    val cloudDataContent: String? = null,
    @SerialName("cloudDataGatewayId")
    val cloudDataGatewayId: Int? = null,
    @SerialName("cloudDataNode")
    val cloudDataNode: String? = null,
    @SerialName("cloudDataName")
    val cloudDataName: String? = null,
    @SerialName("cloudDataId")
    val cloudDataId: Int? = null,
    @SerialName("cloudDataFwUpdate")
    val cloudDataFwUpdate: Int? = null,
    @SerialName("cloudDataLed")
    val cloudDataLed: Int? = null,
    @SerialName("cloudDataBle")
    val cloudDataBle: Int? = null,
    @SerialName("cloudDataInitiator")
    val cloudDataInitiator: Int? = null,
    @SerialName("cloudDataReceivedAt")
    val cloudDataReceivedAt: String? = null,
    
    // cloudData 加前綴字段（物件結構保留）
    @SerialName("cloudDataPosition")
    val cloudDataPosition: PositionData? = null
)

/**
 * 更新錨點請求 - v2 平坦化格式
 */
@Serializable
data class UpdateAnchorRequest(
    val name: String? = null,
    val macAddress: String? = null,
    val position: PositionData? = null,
    val status: String? = null,
    val lastSeen: String? = null,
    
    // cloudData 加前綴字段（標量值）
    @SerialName("cloudDataContent")
    val cloudDataContent: String? = null,
    @SerialName("cloudDataGatewayId")
    val cloudDataGatewayId: Int? = null,
    @SerialName("cloudDataNode")
    val cloudDataNode: String? = null,
    @SerialName("cloudDataName")
    val cloudDataName: String? = null,
    @SerialName("cloudDataId")
    val cloudDataId: Int? = null,
    @SerialName("cloudDataFwUpdate")
    val cloudDataFwUpdate: Int? = null,
    @SerialName("cloudDataLed")
    val cloudDataLed: Int? = null,
    @SerialName("cloudDataBle")
    val cloudDataBle: Int? = null,
    @SerialName("cloudDataInitiator")
    val cloudDataInitiator: Int? = null,
    @SerialName("cloudDataReceivedAt")
    val cloudDataReceivedAt: String? = null,
    
    // cloudData 加前綴字段（物件結構保留）
    @SerialName("cloudDataPosition")
    val cloudDataPosition: PositionData? = null
)

/**
 * 標籤數據模型 (Tag)
 */
@Serializable
data class TagData(
    val id: String,
    val gatewayId: String,
    val name: String,
    val hardwareId: String,
    val cloudData: TagCloudData? = null,
    val boundTo: String? = null,
    val status: String = "offline",
    val createdAt: String
)

/**
 * 標籤雲端數據
 */
@Serializable
data class TagCloudData(
    val id: Int,
    val name: String,
    val config: TagConfig? = null
)

@Serializable
data class TagConfig(
    val fw_update: Boolean = false,
    val led: Boolean = true,
    val ble: Boolean = true
)

/**
 * 創建標籤請求
 */
@Serializable
data class CreateTagRequest(
    val gatewayId: String,
    val name: String,
    val hardwareId: String,
    val cloudData: TagCloudData? = null,
    val boundTo: String? = null
)

/**
 * 更新標籤請求
 */
@Serializable
data class UpdateTagRequest(
    val name: String? = null,
    val hardwareId: String? = null,
    val cloudData: TagCloudData? = null,
    val boundTo: String? = null,
    val status: String? = null
)

/**
 * 通用響應模型
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val timestamp: Long = System.currentTimeMillis()
)

