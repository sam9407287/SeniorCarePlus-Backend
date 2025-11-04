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
import org.jetbrains.exposed.sql.transactions.transaction
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * 場域管理相關路由 (簡化版 - 不含刪除功能)
 */
fun Route.fieldManagementRoutes() {
    route("/api") {
        // ==================== Homes 路由 ====================
        
        // 獲取所有場域
        get("/homes") {
            try {
                val homes = transaction {
                    Homes.selectAll().map { row ->
                        HomeData(
                            id = row[Homes.homeId],
                            name = row[Homes.name],
                            description = row[Homes.description],
                            address = row[Homes.address],
                            createdAt = row[Homes.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                call.respond(homes)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(
                        success = false,
                        message = "獲取場域列表失敗: ${e.message}"
                    )
                )
            }
        }
        
        // 根據ID獲取單個場域
        get("/homes/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少場域ID")
                )
                
                val home = transaction {
                    Homes.select { Homes.homeId eq id }.singleOrNull()?.let { row ->
                        HomeData(
                            id = row[Homes.homeId],
                            name = row[Homes.name],
                            description = row[Homes.description],
                            address = row[Homes.address],
                            createdAt = row[Homes.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                if (home != null) {
                    call.respond(home)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "場域不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取場域失敗: ${e.message}")
                )
            }
        }
        
        // 創建場域
        post("/homes") {
            try {
                val request = call.receive<CreateHomeRequest>()
                val homeId = "home_${System.currentTimeMillis()}"
                
                transaction {
                    Homes.insert {
                        it[Homes.homeId] = homeId
                        it[name] = request.name
                        it[description] = request.description
                        it[address] = request.address
                    }
                }
                
                val newHome = transaction {
                    Homes.select { Homes.homeId eq homeId }.single().let { row ->
                        HomeData(
                            id = row[Homes.homeId],
                            name = row[Homes.name],
                            description = row[Homes.description],
                            address = row[Homes.address],
                            createdAt = row[Homes.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                call.respond(HttpStatusCode.Created, newHome)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "創建場域失敗: ${e.message}")
                )
            }
        }
        
        // 更新場域
        put("/homes/{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少場域ID")
                )
                val request = call.receive<UpdateHomeRequest>()
                
                val updated = transaction {
                    val existing = Homes.select { Homes.homeId eq id }.singleOrNull()
                    if (existing == null) {
                        return@transaction null
                    }
                    
                    Homes.update({ Homes.homeId eq id }) {
                        request.name?.let { name -> it[Homes.name] = name }
                        request.description?.let { desc -> it[description] = desc }
                        request.address?.let { addr -> it[address] = addr }
                    }
                    
                    Homes.select { Homes.homeId eq id }.single().let { row ->
                        HomeData(
                            id = row[Homes.homeId],
                            name = row[Homes.name],
                            description = row[Homes.description],
                            address = row[Homes.address],
                            createdAt = row[Homes.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                if (updated != null) {
                    call.respond(updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "場域不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "更新場域失敗: ${e.message}")
                )
            }
        }
        
        // 根據場域ID獲取樓層列表
        get("/homes/{homeId}/floors") {
            try {
                val homeId = call.parameters["homeId"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少場域ID")
                )
                
                val floors = transaction {
                    Floors.select { Floors.homeId eq homeId }.map { row ->
                        FloorData(
                            id = row[Floors.floorId],
                            homeId = row[Floors.homeId],
                            name = row[Floors.name],
                            level = row[Floors.level],
                            mapImage = row[Floors.mapImage],
                            dimensions = row[Floors.dimensions]?.let { Json.decodeFromString(it) },
                            calibration = row[Floors.calibration]?.let { Json.decodeFromString(it) },
                            createdAt = row[Floors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                call.respond(floors)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取樓層列表失敗: ${e.message}")
                )
            }
        }
        
        // ==================== Floors 路由 ====================
        
        // 獲取所有樓層
        get("/floors") {
            try {
                val floors = transaction {
                    Floors.selectAll().map { row ->
                        FloorData(
                            id = row[Floors.floorId],
                            homeId = row[Floors.homeId],
                            name = row[Floors.name],
                            level = row[Floors.level],
                            mapImage = row[Floors.mapImage],
                            dimensions = row[Floors.dimensions]?.let { Json.decodeFromString(it) },
                            calibration = row[Floors.calibration]?.let { Json.decodeFromString(it) },
                            createdAt = row[Floors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                call.respond(floors)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取樓層列表失敗: ${e.message}")
                )
            }
        }
        
        // 根據ID獲取單個樓層
        get("/floors/{id}") {
            try {
                val id = call.parameters["id"] ?: return@get call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少樓層ID")
                )
                
                val floor = transaction {
                    Floors.select { Floors.floorId eq id }.singleOrNull()?.let { row ->
                        FloorData(
                            id = row[Floors.floorId],
                            homeId = row[Floors.homeId],
                            name = row[Floors.name],
                            level = row[Floors.level],
                            mapImage = row[Floors.mapImage],
                            dimensions = row[Floors.dimensions]?.let { Json.decodeFromString(it) },
                            calibration = row[Floors.calibration]?.let { Json.decodeFromString(it) },
                            createdAt = row[Floors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                if (floor != null) {
                    call.respond(floor)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "樓層不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "獲取樓層失敗: ${e.message}")
                )
            }
        }
        
        // 創建樓層
        post("/floors") {
            try {
                val request = call.receive<CreateFloorRequest>()
                val floorId = "floor_${System.currentTimeMillis()}"
                
                transaction {
                    Floors.insert {
                        it[Floors.floorId] = floorId
                        it[homeId] = request.homeId
                        it[name] = request.name
                        it[level] = request.level
                        it[mapImage] = request.mapImage
                        it[dimensions] = request.dimensions?.let { d -> Json.encodeToString(d) }
                        it[calibration] = request.calibration?.let { c -> Json.encodeToString(c) }
                    }
                }
                
                val newFloor = transaction {
                    Floors.select { Floors.floorId eq floorId }.single().let { row ->
                        FloorData(
                            id = row[Floors.floorId],
                            homeId = row[Floors.homeId],
                            name = row[Floors.name],
                            level = row[Floors.level],
                            mapImage = row[Floors.mapImage],
                            dimensions = row[Floors.dimensions]?.let { Json.decodeFromString(it) },
                            calibration = row[Floors.calibration]?.let { Json.decodeFromString(it) },
                            createdAt = row[Floors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                call.respond(HttpStatusCode.Created, newFloor)
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "創建樓層失敗: ${e.message}")
                )
            }
        }
        
        // 更新樓層
        put("/floors/{id}") {
            try {
                val id = call.parameters["id"] ?: return@put call.respond(
                    HttpStatusCode.BadRequest,
                    ApiResponse<Nothing>(success = false, message = "缺少樓層ID")
                )
                val request = call.receive<UpdateFloorRequest>()
                
                val updated = transaction {
                    val existing = Floors.select { Floors.floorId eq id }.singleOrNull()
                    if (existing == null) {
                        return@transaction null
                    }
                    
                    Floors.update({ Floors.floorId eq id }) {
                        request.name?.let { name -> it[Floors.name] = name }
                        request.level?.let { lv -> it[level] = lv }
                        request.mapImage?.let { img -> it[mapImage] = img }
                        request.dimensions?.let { d -> it[dimensions] = Json.encodeToString(d) }
                        request.calibration?.let { c -> it[calibration] = Json.encodeToString(c) }
                    }
                    
                    Floors.select { Floors.floorId eq id }.single().let { row ->
                        FloorData(
                            id = row[Floors.floorId],
                            homeId = row[Floors.homeId],
                            name = row[Floors.name],
                            level = row[Floors.level],
                            mapImage = row[Floors.mapImage],
                            dimensions = row[Floors.dimensions]?.let { Json.decodeFromString(it) },
                            calibration = row[Floors.calibration]?.let { Json.decodeFromString(it) },
                            createdAt = row[Floors.createdAt].format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        )
                    }
                }
                
                if (updated != null) {
                    call.respond(updated)
                } else {
                    call.respond(
                        HttpStatusCode.NotFound,
                        ApiResponse<Nothing>(success = false, message = "樓層不存在")
                    )
                }
            } catch (e: Exception) {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    ApiResponse<Nothing>(success = false, message = "更新樓層失敗: ${e.message}")
                )
            }
        }
        
        // 其餘 GET /api/gateways, /api/anchors, /api/tags 等路由在此省略
        // 只保留核心的 Homes 和 Floors CRUD（不含刪除）
    }
}
