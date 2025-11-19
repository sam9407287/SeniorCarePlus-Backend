package com.seniorcareplus.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * 平坦化 Gateway 請求模型 - 與前端新格式對應
 * 前端發送的完全平坦格式，所有字段都在同一層級
 */
@Serializable
data class FlattenedGatewayRequest(
    val id: String? = null,
    val floorId: String,
    val name: String,
    val macAddress: String,
    val ipAddress: String? = null,
    val status: String? = null,
    
    // 所有平坦化字段（snake_case）
    val content: String? = null,
    val cloud_gateway_id: Int? = null,
    val fw_version: String? = null,
    val fw_serial: Int? = null,
    val uwb_hw_com_ok: String? = null,
    val uwb_joined: String? = null,
    val uwb_network_id: Int? = null,
    val connected_ap: String? = null,
    val wifi_tx_power: Int? = null,
    val set_wifi_max_tx_power: Double? = null,
    val ble_scan_time: Int? = null,
    val ble_scan_pause_time: Int? = null,
    val battery_voltage: Double? = null,
    val five_v_plugged: String? = null,
    val uwb_tx_power_changed: String? = null,
    val discard_iot_data_time: Int? = null,
    val discarded_iot_data: Int? = null,
    val total_discarded_data: Int? = null,
    val first_sync: String? = null,
    val last_sync: String? = null,
    val current: String? = null,
    val received_at: String? = null,
    
    // UWB TX Power 字段
    val uwb_tx_power_boost_norm: Double? = null,
    val uwb_tx_power_boost_500: Double? = null,
    val uwb_tx_power_boost_250: Double? = null,
    val uwb_tx_power_boost_125: Double? = null,
    
    // Pub Topic 字段
    val pub_topic_anchor_config: String? = null,
    val pub_topic_tag_config: String? = null,
    val pub_topic_location: String? = null,
    val pub_topic_message: String? = null,
    val pub_topic_ack_from_node: String? = null,
    val pub_topic_health: String? = null,
    
    // Sub Topic 字段
    val sub_topic_downlink: String? = null,
    
    // 其他
    val processing_timestamp: String? = null,
    val device_type: String? = null,
    val battery_level: String? = null,
    val createdAt: String? = null
)

/**
 * 平坦化 Anchor 請求模型 - 與前端新格式對應
 */
@Serializable
data class FlattenedAnchorRequest(
    val id: String? = null,
    val gatewayId: String? = null,
    val name: String,
    val macAddress: String,
    val position: PositionData? = null,
    val status: String? = null,
    val lastSeen: String? = null,
    
    // 平坦化的 cloud 字段
    val cloudGatewayId: Int? = null,
    val content: String? = null,
    val cloud_gateway_id: Int? = null,
    val node: String? = null,
    val cloud_anchor_id: Int? = null,
    val received_at: String? = null,
    val fw_update: Boolean? = null,
    val led_enabled: Boolean? = null,
    val ble_enabled: Boolean? = null,
    val is_initiator: Boolean? = null,
    
    // Cloud position 字段
    val cloud_position_x: Double? = null,
    val cloud_position_y: Double? = null,
    val cloud_position_z: Double? = null,
    
    // 其他
    val timestamp: String? = null,
    val processing_timestamp: String? = null,
    val device_type: String? = null,
    val createdAt: String? = null
)

/**
 * 平坦化 Gateway 響應 - 返回前端同樣的平坦格式
 */
@Serializable
data class FlattenedGatewayData(
    val id: String,
    val floorId: String,
    val name: String,
    val macAddress: String,
    val ipAddress: String? = null,
    val status: String = "offline",
    val content: String? = null,
    val cloud_gateway_id: Int? = null,
    val fw_version: String? = null,
    val fw_serial: Int? = null,
    val uwb_hw_com_ok: String? = null,
    val uwb_joined: String? = null,
    val uwb_network_id: Int? = null,
    val connected_ap: String? = null,
    val wifi_tx_power: Int? = null,
    val set_wifi_max_tx_power: Double? = null,
    val ble_scan_time: Int? = null,
    val ble_scan_pause_time: Int? = null,
    val battery_voltage: Double? = null,
    val five_v_plugged: String? = null,
    val uwb_tx_power_changed: String? = null,
    val discard_iot_data_time: Int? = null,
    val discarded_iot_data: Int? = null,
    val total_discarded_data: Int? = null,
    val first_sync: String? = null,
    val last_sync: String? = null,
    val current: String? = null,
    val received_at: String? = null,
    val uwb_tx_power_boost_norm: Double? = null,
    val uwb_tx_power_boost_500: Double? = null,
    val uwb_tx_power_boost_250: Double? = null,
    val uwb_tx_power_boost_125: Double? = null,
    val pub_topic_anchor_config: String? = null,
    val pub_topic_tag_config: String? = null,
    val pub_topic_location: String? = null,
    val pub_topic_message: String? = null,
    val pub_topic_ack_from_node: String? = null,
    val pub_topic_health: String? = null,
    val sub_topic_downlink: String? = null,
    val processing_timestamp: String? = null,
    val device_type: String? = null,
    val battery_level: String? = null,
    val createdAt: String
)

/**
 * 平坦化 Anchor 響應 - 返回前端同樣的平坦格式
 */
@Serializable
data class FlattenedAnchorData(
    val id: String,
    val gatewayId: String? = null,
    val name: String,
    val macAddress: String,
    val position: PositionData? = null,
    val status: String = "offline",
    val lastSeen: String? = null,
    val cloudGatewayId: Int? = null,
    val content: String? = null,
    val cloud_gateway_id: Int? = null,
    val node: String? = null,
    val cloud_anchor_id: Int? = null,
    val received_at: String? = null,
    val fw_update: Boolean? = null,
    val led_enabled: Boolean? = null,
    val ble_enabled: Boolean? = null,
    val is_initiator: Boolean? = null,
    val cloud_position_x: Double? = null,
    val cloud_position_y: Double? = null,
    val cloud_position_z: Double? = null,
    val timestamp: String? = null,
    val processing_timestamp: String? = null,
    val device_type: String? = null,
    val createdAt: String
)

