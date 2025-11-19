package com.seniorcareplus.routes

import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 平坦化設備路由 - 支持前端新的完全平坦格式
 */
fun Route.flattenedDeviceRoutes() {
    route("/api") {
        // ==================== Gateways 路由 (平坦化格式) ====================
        
        // 獲取所有網關 - 返回平坦化格式
        get("/gateways") {
            try {
                val gateways = transaction {
                    Gateways.selectAll().map { row ->
                        convertGatewayRowToFlattenedData(row)
                    }
                }
                call.respond(gateways)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取網關列表失敗: ${e.message}")
                )
            }
        }
        
        // 根據樓層ID獲取網關 - 返回平坦化格式
        get("/floors/{floorId}/gateways") {
            try {
                val floorId = call.parameters["floorId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少樓層ID")
                )
                
                val gateways = transaction {
                    Gateways.select { Gateways.floorId eq floorId }.map { row ->
                        convertGatewayRowToFlattenedData(row)
                    }
                }
                call.respond(gateways)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取樓層網關列表失敗: ${e.message}")
                )
            }
        }
        
        // 創建新網關 - 接收平坦化格式
        post("/gateways") {
            try {
                val request = call.receive<FlattenedGatewayRequest>()
                val gatewayId = request.id ?: "gw_${System.currentTimeMillis()}"
                
                // 將平坦化請求轉換為存儲格式（JSON 字符串）
                val cloudDataJson = convertFlattenedGatewayToCloudDataJson(request)
                
                transaction {
                    Gateways.insert {
                        it[Gateways.gatewayId] = gatewayId
                        it[floorId] = request.floorId
                        it[name] = request.name
                        it[macAddress] = request.macAddress
                        it[ipAddress] = request.ipAddress ?: ""
                        it[firmwareVersion] = request.fw_version ?: ""
                        it[cloudData] = cloudDataJson
                        it[status] = request.status ?: "offline"
                    }
                }
                
                val newGateway = transaction {
                    Gateways.select { Gateways.gatewayId eq gatewayId }.single().let { row ->
                        convertGatewayRowToFlattenedData(row)
                    }
                }
                
                call.respond(HttpStatusCode.Created, newGateway)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "創建網關失敗: ${e.message}")
                )
            }
        }
        
        // 根據ID獲取單個網關 - 返回平坦化格式
        get("/gateways/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少網關ID")
                )
                
                val gateway = transaction {
                    Gateways.select { Gateways.gatewayId eq id }.singleOrNull()?.let { row ->
                        convertGatewayRowToFlattenedData(row)
                    }
                }
                
                if (gateway != null) {
                    call.respond(gateway)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "網關不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取網關失敗: ${e.message}")
                )
            }
        }
        
        // 更新網關 - 接收平坦化格式
        put("/gateways/{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少網關ID")
                )
                val request = call.receive<FlattenedGatewayRequest>()
                
                val updated = transaction {
                    val existing = Gateways.select { Gateways.gatewayId eq id }.singleOrNull()
                    if (existing == null) {
                        return@transaction null
                    }
                    
                    val cloudDataJson = convertFlattenedGatewayToCloudDataJson(request)
                    
                    Gateways.update({ Gateways.gatewayId eq id }) {
                        it[Gateways.name] = request.name
                        it[Gateways.floorId] = request.floorId
                        it[Gateways.macAddress] = request.macAddress
                        it[Gateways.ipAddress] = request.ipAddress ?: ""
                        it[Gateways.firmwareVersion] = request.fw_version ?: ""
                        it[Gateways.cloudData] = cloudDataJson
                        it[Gateways.status] = request.status ?: "offline"
                        if (request.received_at != null) {
                            it[Gateways.lastSeen] = try {
                                LocalDateTime.parse(request.received_at)
                            } catch (e: Exception) {
                                LocalDateTime.now()
                            }
                        }
                    }
                    
                    Gateways.select { Gateways.gatewayId eq id }.single().let { row ->
                        convertGatewayRowToFlattenedData(row)
                    }
                }
                
                if (updated != null) {
                    call.respond(updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "網關不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "更新網關失敗: ${e.message}")
                )
            }
        }
        
        // 刪除網關
        delete("/gateways/{id}") {
            try {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少網關ID")
                )
                
                val deleted = transaction {
                    Gateways.deleteWhere { Gateways.gatewayId eq id }
                }
                
                if (deleted > 0) {
                    call.respond(ApiResponse<Nothing>(success = true, message = "網關已刪除"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "網關不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "刪除網關失敗: ${e.message}")
                )
            }
        }
        
        // ==================== Anchors 路由 (平坦化格式) ====================
        
        // 獲取所有 Anchor
        get("/anchors") {
            try {
                val anchors = transaction {
                    Anchors.selectAll().map { row ->
                        convertAnchorRowToFlattenedData(row)
                    }
                }
                call.respond(anchors)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取 Anchor 列表失敗: ${e.message}")
                )
            }
        }
        
        // 根據 Gateway ID 獲取 Anchor
        get("/gateways/{gatewayId}/anchors") {
            try {
                val gatewayId = call.parameters["gatewayId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少 Gateway ID")
                )
                
                val anchors = transaction {
                    Anchors.select { Anchors.gatewayId eq gatewayId }.map { row ->
                        convertAnchorRowToFlattenedData(row)
                    }
                }
                call.respond(anchors)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取 Anchor 列表失敗: ${e.message}")
                )
            }
        }
        
        // 創建新 Anchor
        post("/anchors") {
            try {
                val request = call.receive<FlattenedAnchorRequest>()
                val anchorId = request.id ?: "anchor_${System.currentTimeMillis()}"
                
                val cloudDataJson = convertFlattenedAnchorToCloudDataJson(request)
                
                transaction {
                    Anchors.insert {
                        it[Anchors.anchorId] = anchorId
                        it[Anchors.gatewayId] = request.gatewayId ?: ""
                        it[Anchors.name] = request.name
                        it[Anchors.macAddress] = request.macAddress
                        it[Anchors.cloudData] = cloudDataJson
                        it[Anchors.status] = request.status ?: "offline"
                    }
                }
                
                val newAnchor = transaction {
                    Anchors.select { Anchors.anchorId eq anchorId }.single().let { row ->
                        convertAnchorRowToFlattenedData(row)
                    }
                }
                
                call.respond(HttpStatusCode.Created, newAnchor)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "創建 Anchor 失敗: ${e.message}")
                )
            }
        }
        
        // 根據 ID 獲取單個 Anchor
        get("/anchors/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少 Anchor ID")
                )
                
                val anchor = transaction {
                    Anchors.select { Anchors.anchorId eq id }.singleOrNull()?.let { row ->
                        convertAnchorRowToFlattenedData(row)
                    }
                }
                
                if (anchor != null) {
                    call.respond(anchor)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "Anchor 不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取 Anchor 失敗: ${e.message}")
                )
            }
        }
        
        // 更新 Anchor
        put("/anchors/{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少 Anchor ID")
                )
                val request = call.receive<FlattenedAnchorRequest>()
                
                val updated = transaction {
                    val existing = Anchors.select { Anchors.anchorId eq id }.singleOrNull()
                    if (existing == null) {
                        return@transaction null
                    }
                    
                    val cloudDataJson = convertFlattenedAnchorToCloudDataJson(request)
                    
                    Anchors.update({ Anchors.anchorId eq id }) {
                        it[Anchors.name] = request.name
                        it[Anchors.macAddress] = request.macAddress
                        if (request.gatewayId != null) {
                            it[Anchors.gatewayId] = request.gatewayId
                        }
                        it[Anchors.cloudData] = cloudDataJson
                        it[Anchors.status] = request.status ?: "offline"
                        if (request.received_at != null) {
                            it[Anchors.lastSeen] = try {
                                LocalDateTime.parse(request.received_at)
                            } catch (e: Exception) {
                                LocalDateTime.now()
                            }
                        }
                    }
                    
                    Anchors.select { Anchors.anchorId eq id }.single().let { row ->
                        convertAnchorRowToFlattenedData(row)
                    }
                }
                
                if (updated != null) {
                    call.respond(updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "Anchor 不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "更新 Anchor 失敗: ${e.message}")
                )
            }
        }
        
        // 刪除 Anchor
        delete("/anchors/{id}") {
            try {
                val id = call.parameters["id"] ?: return@delete call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少 Anchor ID")
                )
                
                val deleted = transaction {
                    Anchors.deleteWhere { Anchors.anchorId eq id }
                }
                
                if (deleted > 0) {
                    call.respond(ApiResponse<Nothing>(success = true, message = "Anchor 已刪除"))
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "Anchor 不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "刪除 Anchor 失敗: ${e.message}")
                )
            }
        }
    }
}

/**
 * 轉換工具函數
 */

private fun convertGatewayRowToFlattenedData(row: ResultRow): FlattenedGatewayData {
    val cloudDataJson = row[Gateways.cloudData] ?: "{}"
    val cloudData = try {
        Json.decodeFromString<Map<String, Any?>>(cloudDataJson)
    } catch (e: Exception) {
        emptyMap()
    }
    
    return FlattenedGatewayData(
        id = row[Gateways.gatewayId],
        floorId = row[Gateways.floorId],
        name = row[Gateways.name],
        macAddress = row[Gateways.macAddress],
        ipAddress = row[Gateways.ipAddress]?.takeIf { it.isNotEmpty() },
        status = row[Gateways.status],
        content = cloudData["content"]?.toString(),
        cloud_gateway_id = cloudData["cloud_gateway_id"]?.toString()?.toIntOrNull(),
        fw_version = cloudData["fw_version"]?.toString(),
        fw_serial = cloudData["fw_serial"]?.toString()?.toIntOrNull(),
        uwb_hw_com_ok = cloudData["uwb_hw_com_ok"]?.toString(),
        uwb_joined = cloudData["uwb_joined"]?.toString(),
        uwb_network_id = cloudData["uwb_network_id"]?.toString()?.toIntOrNull(),
        connected_ap = cloudData["connected_ap"]?.toString(),
        wifi_tx_power = cloudData["wifi_tx_power"]?.toString()?.toIntOrNull(),
        set_wifi_max_tx_power = cloudData["set_wifi_max_tx_power"]?.toString()?.toDoubleOrNull(),
        ble_scan_time = cloudData["ble_scan_time"]?.toString()?.toIntOrNull(),
        ble_scan_pause_time = cloudData["ble_scan_pause_time"]?.toString()?.toIntOrNull(),
        battery_voltage = cloudData["battery_voltage"]?.toString()?.toDoubleOrNull(),
        five_v_plugged = cloudData["five_v_plugged"]?.toString(),
        uwb_tx_power_changed = cloudData["uwb_tx_power_changed"]?.toString(),
        discard_iot_data_time = cloudData["discard_iot_data_time"]?.toString()?.toIntOrNull(),
        discarded_iot_data = cloudData["discarded_iot_data"]?.toString()?.toIntOrNull(),
        total_discarded_data = cloudData["total_discarded_data"]?.toString()?.toIntOrNull(),
        first_sync = cloudData["first_sync"]?.toString(),
        last_sync = cloudData["last_sync"]?.toString(),
        current = cloudData["current"]?.toString(),
        received_at = cloudData["received_at"]?.toString(),
        uwb_tx_power_boost_norm = cloudData["uwb_tx_power_boost_norm"]?.toString()?.toDoubleOrNull(),
        uwb_tx_power_boost_500 = cloudData["uwb_tx_power_boost_500"]?.toString()?.toDoubleOrNull(),
        uwb_tx_power_boost_250 = cloudData["uwb_tx_power_boost_250"]?.toString()?.toDoubleOrNull(),
        uwb_tx_power_boost_125 = cloudData["uwb_tx_power_boost_125"]?.toString()?.toDoubleOrNull(),
        pub_topic_anchor_config = cloudData["pub_topic_anchor_config"]?.toString(),
        pub_topic_tag_config = cloudData["pub_topic_tag_config"]?.toString(),
        pub_topic_location = cloudData["pub_topic_location"]?.toString(),
        pub_topic_message = cloudData["pub_topic_message"]?.toString(),
        pub_topic_ack_from_node = cloudData["pub_topic_ack_from_node"]?.toString(),
        pub_topic_health = cloudData["pub_topic_health"]?.toString(),
        sub_topic_downlink = cloudData["sub_topic_downlink"]?.toString(),
        processing_timestamp = cloudData["processing_timestamp"]?.toString(),
        device_type = cloudData["device_type"]?.toString(),
        battery_level = cloudData["battery_level"]?.toString(),
        createdAt = row[Gateways.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

private fun convertAnchorRowToFlattenedData(row: ResultRow): FlattenedAnchorData {
    val cloudDataJson = row[Anchors.cloudData] ?: "{}"
    val cloudData = try {
        Json.decodeFromString<Map<String, Any?>>(cloudDataJson)
    } catch (e: Exception) {
        emptyMap()
    }
    
    // 解析 position 物件
    val position = try {
        val posStr = cloudData["position"]?.toString() ?: "{}"
        Json.decodeFromString<PositionData>(posStr)
    } catch (e: Exception) {
        null
    }
    
    return FlattenedAnchorData(
        id = row[Anchors.anchorId],
        gatewayId = row[Anchors.gatewayId],
        name = row[Anchors.name],
        macAddress = row[Anchors.macAddress],
        position = position,
        status = row[Anchors.status],
        lastSeen = row[Anchors.lastSeen]?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
        cloudGatewayId = cloudData["cloud_gateway_id"]?.toString()?.toIntOrNull(),
        content = cloudData["content"]?.toString(),
        cloud_gateway_id = cloudData["cloud_gateway_id"]?.toString()?.toIntOrNull(),
        node = cloudData["node"]?.toString(),
        cloud_anchor_id = cloudData["cloud_anchor_id"]?.toString()?.toIntOrNull(),
        received_at = cloudData["received_at"]?.toString(),
        fw_update = cloudData["fw_update"]?.toString()?.toBoolean(),
        led_enabled = cloudData["led_enabled"]?.toString()?.toBoolean(),
        ble_enabled = cloudData["ble_enabled"]?.toString()?.toBoolean(),
        is_initiator = cloudData["is_initiator"]?.toString()?.toBoolean(),
        cloud_position_x = cloudData["cloud_position_x"]?.toString()?.toDoubleOrNull(),
        cloud_position_y = cloudData["cloud_position_y"]?.toString()?.toDoubleOrNull(),
        cloud_position_z = cloudData["cloud_position_z"]?.toString()?.toDoubleOrNull(),
        timestamp = cloudData["timestamp"]?.toString(),
        processing_timestamp = cloudData["processing_timestamp"]?.toString(),
        device_type = cloudData["device_type"]?.toString(),
        createdAt = row[Anchors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    )
}

private fun convertFlattenedGatewayToCloudDataJson(request: FlattenedGatewayRequest): String {
    val cloudData = mapOf(
        "content" to request.content,
        "cloud_gateway_id" to request.cloud_gateway_id,
        "fw_version" to request.fw_version,
        "fw_serial" to request.fw_serial,
        "uwb_hw_com_ok" to request.uwb_hw_com_ok,
        "uwb_joined" to request.uwb_joined,
        "uwb_network_id" to request.uwb_network_id,
        "connected_ap" to request.connected_ap,
        "wifi_tx_power" to request.wifi_tx_power,
        "set_wifi_max_tx_power" to request.set_wifi_max_tx_power,
        "ble_scan_time" to request.ble_scan_time,
        "ble_scan_pause_time" to request.ble_scan_pause_time,
        "battery_voltage" to request.battery_voltage,
        "five_v_plugged" to request.five_v_plugged,
        "uwb_tx_power_changed" to request.uwb_tx_power_changed,
        "discard_iot_data_time" to request.discard_iot_data_time,
        "discarded_iot_data" to request.discarded_iot_data,
        "total_discarded_data" to request.total_discarded_data,
        "first_sync" to request.first_sync,
        "last_sync" to request.last_sync,
        "current" to request.current,
        "received_at" to request.received_at,
        "uwb_tx_power_boost_norm" to request.uwb_tx_power_boost_norm,
        "uwb_tx_power_boost_500" to request.uwb_tx_power_boost_500,
        "uwb_tx_power_boost_250" to request.uwb_tx_power_boost_250,
        "uwb_tx_power_boost_125" to request.uwb_tx_power_boost_125,
        "pub_topic_anchor_config" to request.pub_topic_anchor_config,
        "pub_topic_tag_config" to request.pub_topic_tag_config,
        "pub_topic_location" to request.pub_topic_location,
        "pub_topic_message" to request.pub_topic_message,
        "pub_topic_ack_from_node" to request.pub_topic_ack_from_node,
        "pub_topic_health" to request.pub_topic_health,
        "sub_topic_downlink" to request.sub_topic_downlink,
        "processing_timestamp" to request.processing_timestamp,
        "device_type" to request.device_type,
        "battery_level" to request.battery_level
    ).filterValues { it != null }
    
    return Json.encodeToString<Map<String, Any?>>(cloudData)
}

private fun convertFlattenedAnchorToCloudDataJson(request: FlattenedAnchorRequest): String {
    val cloudData = mapOf(
        "position" to request.position?.let { Json.encodeToString<PositionData>(it) },
        "cloud_gateway_id" to request.cloud_gateway_id,
        "content" to request.content,
        "node" to request.node,
        "cloud_anchor_id" to request.cloud_anchor_id,
        "received_at" to request.received_at,
        "fw_update" to request.fw_update,
        "led_enabled" to request.led_enabled,
        "ble_enabled" to request.ble_enabled,
        "is_initiator" to request.is_initiator,
        "cloud_position_x" to request.cloud_position_x,
        "cloud_position_y" to request.cloud_position_y,
        "cloud_position_z" to request.cloud_position_z,
        "timestamp" to request.timestamp,
        "processing_timestamp" to request.processing_timestamp,
        "device_type" to request.device_type
    ).filterValues { it != null }
    
    return Json.encodeToString<Map<String, Any?>>(cloudData)
}

