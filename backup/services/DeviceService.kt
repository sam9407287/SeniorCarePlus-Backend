package com.seniorcareplus.services

import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

/**
 * 設備管理服務
 * 負責設備的註冊、狀態管理、配置管理等功能
 */
class DeviceService {
    private val logger = LoggerFactory.getLogger(DeviceService::class.java)
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * 註冊新設備
     */
    suspend fun registerDevice(request: DeviceRegistrationRequest, registeredBy: Long): Result<Device> {
        return try {
            transaction {
                // 檢查設備ID是否已存在
                val existingDevice = Devices.select { Devices.deviceId eq request.deviceId }.singleOrNull()
                if (existingDevice != null) {
                    return@transaction Result.failure(Exception("設備ID已存在"))
                }

                // 檢查序列號是否已存在
                val existingSerial = Devices.select { Devices.serialNumber eq request.serialNumber }.singleOrNull()
                if (existingSerial != null) {
                    return@transaction Result.failure(Exception("序列號已存在"))
                }

                // 創建新設備
                val deviceId = Devices.insertAndGetId {
                    it[Devices.deviceId] = request.deviceId
                    it[deviceName] = request.deviceName
                    it[deviceType] = request.deviceType
                    it[manufacturer] = request.manufacturer
                    it[model] = request.model
                    it[firmwareVersion] = request.firmwareVersion
                    it[hardwareVersion] = request.hardwareVersion
                    it[serialNumber] = request.serialNumber
                    it[macAddress] = request.macAddress
                    it[connectionType] = request.connectionType
                    it[location] = request.location
                    it[description] = request.description
                    it[status] = DeviceStatus.INACTIVE
                    it[createdAt] = LocalDateTime.now()
                    it[updatedAt] = LocalDateTime.now()
                }

                // 記錄設備狀態歷史
                DeviceStatusHistory.insert {
                    it[DeviceStatusHistory.deviceId] = request.deviceId
                    it[previousStatus] = DeviceStatus.INACTIVE
                    it[newStatus] = DeviceStatus.INACTIVE
                    it[reason] = "設備註冊"
                    it[changedBy] = registeredBy
                    it[createdAt] = LocalDateTime.now()
                }

                // 創建設備統計記錄
                DeviceStatistics.insert {
                    it[DeviceStatistics.deviceId] = request.deviceId
                    it[calculatedAt] = LocalDateTime.now()
                }

                val device = getDeviceById(request.deviceId)
                if (device != null) {
                    logger.info("設備註冊成功: ${request.deviceId}")
                    Result.success(device)
                } else {
                    Result.failure(Exception("設備創建失敗"))
                }
            }
        } catch (e: Exception) {
            logger.error("設備註冊失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 根據設備ID獲取設備信息
     */
    suspend fun getDeviceById(deviceId: String): Device? {
        return try {
            transaction {
                Devices.select { Devices.deviceId eq deviceId }
                    .singleOrNull()
                    ?.let { row ->
                        Device(
                            id = row[Devices.id].value,
                            deviceId = row[Devices.deviceId],
                            deviceName = row[Devices.deviceName],
                            deviceType = row[Devices.deviceType],
                            manufacturer = row[Devices.manufacturer],
                            model = row[Devices.model],
                            firmwareVersion = row[Devices.firmwareVersion],
                            hardwareVersion = row[Devices.hardwareVersion],
                            serialNumber = row[Devices.serialNumber],
                            macAddress = row[Devices.macAddress],
                            connectionType = row[Devices.connectionType],
                            status = row[Devices.status],
                            batteryLevel = row[Devices.batteryLevel],
                            signalStrength = row[Devices.signalStrength],
                            lastHeartbeat = row[Devices.lastHeartbeat],
                            location = row[Devices.location],
                            description = row[Devices.description],
                            isActive = row[Devices.isActive],
                            createdAt = row[Devices.createdAt],
                            updatedAt = row[Devices.updatedAt]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("獲取設備信息失敗: ${e.message}", e)
            null
        }
    }

    /**
     * 更新設備信息
     */
    suspend fun updateDevice(deviceId: String, request: DeviceUpdateRequest, updatedBy: Long): Result<Device> {
        return try {
            transaction {
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("設備不存在"))

                Devices.update({ Devices.deviceId eq deviceId }) {
                    request.deviceName?.let { name -> it[deviceName] = name }
                    request.location?.let { loc -> it[location] = loc }
                    request.description?.let { desc -> it[description] = desc }
                    it[updatedAt] = LocalDateTime.now()
                }

                val updatedDevice = getDeviceById(deviceId)
                if (updatedDevice != null) {
                    logger.info("設備更新成功: $deviceId")
                    Result.success(updatedDevice)
                } else {
                    Result.failure(Exception("設備更新失敗"))
                }
            }
        } catch (e: Exception) {
            logger.error("設備更新失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 更新設備狀態
     */
    suspend fun updateDeviceStatus(deviceId: String, statusUpdate: DeviceStatusUpdate, updatedBy: Long): Result<Boolean> {
        return try {
            transaction {
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("設備不存在"))

                val currentStatus = existingDevice[Devices.status]

                // 更新設備狀態
                Devices.update({ Devices.deviceId eq deviceId }) {
                    it[status] = statusUpdate.status
                    statusUpdate.batteryLevel?.let { battery -> it[batteryLevel] = battery }
                    statusUpdate.signalStrength?.let { signal -> it[signalStrength] = signal }
                    it[lastHeartbeat] = LocalDateTime.now()
                    it[updatedAt] = LocalDateTime.now()
                }

                // 記錄狀態變更歷史
                if (currentStatus != statusUpdate.status) {
                    DeviceStatusHistory.insert {
                        it[DeviceStatusHistory.deviceId] = deviceId
                        it[previousStatus] = currentStatus
                        it[newStatus] = statusUpdate.status
                        it[reason] = statusUpdate.reason
                        it[changedBy] = updatedBy
                        it[metadata] = statusUpdate.metadata?.let { meta -> json.encodeToString(meta) }
                        it[createdAt] = LocalDateTime.now()
                    }
                }

                logger.info("設備狀態更新成功: $deviceId -> ${statusUpdate.status}")
                Result.success(true)
            }
        } catch (e: Exception) {
            logger.error("設備狀態更新失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 處理設備心跳
     */
    suspend fun processDeviceHeartbeat(deviceId: String, heartbeat: DeviceHeartbeat): Result<Boolean> {
        return try {
            transaction {
                // 檢查設備是否存在
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("設備不存在"))

                // 更新設備最後心跳時間和狀態
                Devices.update({ Devices.deviceId eq deviceId }) {
                    it[lastHeartbeat] = heartbeat.timestamp
                    it[status] = heartbeat.status
                    heartbeat.batteryLevel?.let { battery -> it[batteryLevel] = battery }
                    heartbeat.signalStrength?.let { signal -> it[signalStrength] = signal }
                    it[updatedAt] = LocalDateTime.now()
                }

                // 記錄心跳數據
                DeviceHeartbeats.insert {
                    it[DeviceHeartbeats.deviceId] = deviceId
                    it[timestamp] = heartbeat.timestamp
                    it[batteryLevel] = heartbeat.batteryLevel
                    it[signalStrength] = heartbeat.signalStrength
                    it[status] = heartbeat.status
                    it[metadata] = heartbeat.metadata?.let { meta -> json.encodeToString(meta) }
                    it[createdAt] = LocalDateTime.now()
                }

                // 檢查是否需要生成警報
                checkAndGenerateAlerts(deviceId, heartbeat)

                Result.success(true)
            }
        } catch (e: Exception) {
            logger.error("處理設備心跳失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取設備列表
     */
    suspend fun getDevices(
        page: Int = 1,
        pageSize: Int = 20,
        deviceType: DeviceType? = null,
        status: DeviceStatus? = null,
        location: String? = null
    ): DeviceListResponse {
        return try {
            transaction {
                var query = Devices.selectAll()

                // 添加過濾條件
                deviceType?.let { type ->
                    query = query.andWhere { Devices.deviceType eq type }
                }
                status?.let { stat ->
                    query = query.andWhere { Devices.status eq stat }
                }
                location?.let { loc ->
                    query = query.andWhere { Devices.location like "%$loc%" }
                }

                val totalCount = query.count()
                val offset = (page - 1) * pageSize

                val devices = query
                    .orderBy(Devices.createdAt, SortOrder.DESC)
                    .limit(pageSize, offset.toLong())
                    .map { row ->
                        Device(
                            id = row[Devices.id].value,
                            deviceId = row[Devices.deviceId],
                            deviceName = row[Devices.deviceName],
                            deviceType = row[Devices.deviceType],
                            manufacturer = row[Devices.manufacturer],
                            model = row[Devices.model],
                            firmwareVersion = row[Devices.firmwareVersion],
                            hardwareVersion = row[Devices.hardwareVersion],
                            serialNumber = row[Devices.serialNumber],
                            macAddress = row[Devices.macAddress],
                            connectionType = row[Devices.connectionType],
                            status = row[Devices.status],
                            batteryLevel = row[Devices.batteryLevel],
                            signalStrength = row[Devices.signalStrength],
                            lastHeartbeat = row[Devices.lastHeartbeat],
                            location = row[Devices.location],
                            description = row[Devices.description],
                            isActive = row[Devices.isActive],
                            createdAt = row[Devices.createdAt],
                            updatedAt = row[Devices.updatedAt]
                        )
                    }

                DeviceListResponse(
                    devices = devices,
                    totalCount = totalCount.toInt(),
                    page = page,
                    pageSize = pageSize,
                    totalPages = ((totalCount + pageSize - 1) / pageSize).toInt()
                )
            }
        } catch (e: Exception) {
            logger.error("獲取設備列表失敗: ${e.message}", e)
            DeviceListResponse(emptyList(), 0, page, pageSize, 0)
        }
    }

    /**
     * 獲取設備配置
     */
    suspend fun getDeviceConfiguration(deviceId: String): List<DeviceConfiguration> {
        return try {
            transaction {
                DeviceConfigurations.select { DeviceConfigurations.deviceId eq deviceId }
                    .map { row ->
                        DeviceConfiguration(
                            configKey = row[DeviceConfigurations.configKey],
                            configValue = row[DeviceConfigurations.configValue],
                            description = row[DeviceConfigurations.description],
                            isEditable = row[DeviceConfigurations.isEditable]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("獲取設備配置失敗: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 更新設備配置
     */
    suspend fun updateDeviceConfiguration(
        deviceId: String,
        request: DeviceConfigurationRequest,
        updatedBy: Long
    ): Result<Boolean> {
        return try {
            transaction {
                // 檢查設備是否存在
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("設備不存在"))

                request.configurations.forEach { config ->
                    // 檢查配置是否存在
                    val existingConfig = DeviceConfigurations.select {
                        (DeviceConfigurations.deviceId eq deviceId) and
                                (DeviceConfigurations.configKey eq config.configKey)
                    }.singleOrNull()

                    if (existingConfig != null) {
                        // 更新現有配置
                        DeviceConfigurations.update({
                            (DeviceConfigurations.deviceId eq deviceId) and
                                    (DeviceConfigurations.configKey eq config.configKey)
                        }) {
                            it[configValue] = config.configValue
                            it[updatedAt] = LocalDateTime.now()
                        }
                    } else {
                        // 創建新配置
                        DeviceConfigurations.insert {
                            it[DeviceConfigurations.deviceId] = deviceId
                            it[configKey] = config.configKey
                            it[configValue] = config.configValue
                            it[description] = config.description
                            it[isEditable] = config.isEditable
                            it[createdAt] = LocalDateTime.now()
                            it[updatedAt] = LocalDateTime.now()
                        }
                    }
                }

                logger.info("設備配置更新成功: $deviceId")
                Result.success(true)
            }
        } catch (e: Exception) {
            logger.error("設備配置更新失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 獲取設備統計信息
     */
    suspend fun getDeviceStatistics(deviceId: String): DeviceStatistics? {
        return try {
            transaction {
                DeviceStatistics.select { DeviceStatistics.deviceId eq deviceId }
                    .singleOrNull()
                    ?.let { row ->
                        com.seniorcareplus.models.DeviceStatistics(
                            deviceId = row[DeviceStatistics.deviceId],
                            totalUptime = row[DeviceStatistics.totalUptime],
                            totalDowntime = row[DeviceStatistics.totalDowntime],
                            averageBatteryLevel = row[DeviceStatistics.averageBatteryLevel],
                            averageSignalStrength = row[DeviceStatistics.averageSignalStrength],
                            dataPointsCount = row[DeviceStatistics.dataPointsCount],
                            lastDataReceived = row[DeviceStatistics.lastDataReceived],
                            errorCount = row[DeviceStatistics.errorCount],
                            maintenanceCount = row[DeviceStatistics.maintenanceCount],
                            calculatedAt = row[DeviceStatistics.calculatedAt]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("獲取設備統計失敗: ${e.message}", e)
            null
        }
    }

    /**
     * 獲取設備警報
     */
    suspend fun getDeviceAlerts(
        deviceId: String? = null,
        isResolved: Boolean? = null,
        severity: AlertSeverity? = null,
        limit: Int = 50
    ): List<DeviceAlert> {
        return try {
            transaction {
                var query = DeviceAlerts.selectAll()

                deviceId?.let { id ->
                    query = query.andWhere { DeviceAlerts.deviceId eq id }
                }
                isResolved?.let { resolved ->
                    query = query.andWhere { DeviceAlerts.isResolved eq resolved }
                }
                severity?.let { sev ->
                    query = query.andWhere { DeviceAlerts.severity eq sev }
                }

                query.orderBy(DeviceAlerts.createdAt, SortOrder.DESC)
                    .limit(limit)
                    .map { row ->
                        DeviceAlert(
                            id = row[DeviceAlerts.id].value,
                            deviceId = row[DeviceAlerts.deviceId],
                            alertType = row[DeviceAlerts.alertType],
                            severity = row[DeviceAlerts.severity],
                            title = row[DeviceAlerts.title],
                            message = row[DeviceAlerts.message],
                            isResolved = row[DeviceAlerts.isResolved],
                            resolvedAt = row[DeviceAlerts.resolvedAt],
                            resolvedBy = row[DeviceAlerts.resolvedBy],
                            metadata = row[DeviceAlerts.metadata]?.let { json.decodeFromString(it) },
                            createdAt = row[DeviceAlerts.createdAt]
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("獲取設備警報失敗: ${e.message}", e)
            emptyList()
        }
    }

    /**
     * 解決設備警報
     */
    suspend fun resolveDeviceAlert(alertId: Long, resolvedBy: Long): Result<Boolean> {
        return try {
            transaction {
                val updated = DeviceAlerts.update({ DeviceAlerts.id eq alertId }) {
                    it[isResolved] = true
                    it[resolvedAt] = LocalDateTime.now()
                    it[DeviceAlerts.resolvedBy] = resolvedBy
                }

                if (updated > 0) {
                    logger.info("設備警報已解決: $alertId")
                    Result.success(true)
                } else {
                    Result.failure(Exception("警報不存在"))
                }
            }
        } catch (e: Exception) {
            logger.error("解決設備警報失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 刪除設備
     */
    suspend fun deleteDevice(deviceId: String, deletedBy: Long): Result<Boolean> {
        return try {
            transaction {
                // 檢查設備是否存在
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                    ?: return@transaction Result.failure(Exception("設備不存在"))

                // 軟刪除：設置為非活躍狀態
                Devices.update({ Devices.deviceId eq deviceId }) {
                    it[isActive] = false
                    it[status] = DeviceStatus.INACTIVE
                    it[updatedAt] = LocalDateTime.now()
                }

                // 記錄狀態變更
                DeviceStatusHistory.insert {
                    it[DeviceStatusHistory.deviceId] = deviceId
                    it[previousStatus] = existingDevice[Devices.status]
                    it[newStatus] = DeviceStatus.INACTIVE
                    it[reason] = "設備刪除"
                    it[changedBy] = deletedBy
                    it[createdAt] = LocalDateTime.now()
                }

                logger.info("設備刪除成功: $deviceId")
                Result.success(true)
            }
        } catch (e: Exception) {
            logger.error("設備刪除失敗: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * 檢查並生成警報
     */
    private fun checkAndGenerateAlerts(deviceId: String, heartbeat: DeviceHeartbeat) {
        try {
            // 檢查電池電量
            heartbeat.batteryLevel?.let { battery ->
                if (battery <= 10) {
                    createAlert(
                        deviceId = deviceId,
                        alertType = AlertType.LOW_BATTERY,
                        severity = if (battery <= 5) AlertSeverity.CRITICAL else AlertSeverity.WARNING,
                        title = "設備電量不足",
                        message = "設備電量僅剩 $battery%，請及時充電"
                    )
                }
            }

            // 檢查信號強度
            heartbeat.signalStrength?.let { signal ->
                if (signal <= 20) {
                    createAlert(
                        deviceId = deviceId,
                        alertType = AlertType.WEAK_SIGNAL,
                        severity = AlertSeverity.WARNING,
                        title = "設備信號弱",
                        message = "設備信號強度僅為 $signal%，可能影響數據傳輸"
                    )
                }
            }

            // 檢查設備狀態
            if (heartbeat.status == DeviceStatus.ERROR) {
                createAlert(
                    deviceId = deviceId,
                    alertType = AlertType.DEVICE_ERROR,
                    severity = AlertSeverity.HIGH,
                    title = "設備錯誤",
                    message = "設備報告錯誤狀態，需要檢查"
                )
            }
        } catch (e: Exception) {
            logger.error("生成警報失敗: ${e.message}", e)
        }
    }

    /**
     * 創建設備警報
     */
    private fun createAlert(
        deviceId: String,
        alertType: AlertType,
        severity: AlertSeverity,
        title: String,
        message: String,
        metadata: Map<String, Any>? = null
    ) {
        try {
            // 檢查是否已存在相同類型的未解決警報
            val existingAlert = DeviceAlerts.select {
                (DeviceAlerts.deviceId eq deviceId) and
                        (DeviceAlerts.alertType eq alertType) and
                        (DeviceAlerts.isResolved eq false)
            }.singleOrNull()

            if (existingAlert == null) {
                DeviceAlerts.insert {
                    it[DeviceAlerts.deviceId] = deviceId
                    it[DeviceAlerts.alertType] = alertType
                    it[DeviceAlerts.severity] = severity
                    it[DeviceAlerts.title] = title
                    it[DeviceAlerts.message] = message
                    it[DeviceAlerts.metadata] = metadata?.let { meta -> json.encodeToString(meta) }
                    it[createdAt] = LocalDateTime.now()
                }
                logger.info("創建設備警報: $deviceId - $title")
            }
        } catch (e: Exception) {
            logger.error("創建警報失敗: ${e.message}", e)
        }
    }
} 