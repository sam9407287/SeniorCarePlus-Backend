package com.seniorcareplus.services

import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime

class DataStorageService {
    private val logger = LoggerFactory.getLogger(DataStorageService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    
    // 保存心率數據
    suspend fun saveHeartRateData(data: HeartRateData) {
        try {
            transaction {
                // 確保患者存在
                val patientId = ensurePatientExists(data.patientId, data.deviceId)
                
                // 保存健康記錄
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[dataType] = "heart_rate"
                    it[value] = json.encodeToString(data)
                    it[deviceId] = data.deviceId
                    it[quality] = data.quality
                    it[timestamp] = data.timestamp
                }
                
                // 更新設備最後見到時間
                updateDeviceLastSeen(data.deviceId)
            }
            logger.debug("心率數據已保存: 患者=${data.patientId}")
        } catch (e: Exception) {
            logger.error("保存心率數據失敗", e)
        }
    }
    
    // 保存體溫數據
    suspend fun saveTemperatureData(data: TemperatureData) {
        try {
            transaction {
                val patientId = ensurePatientExists(data.patientId, data.deviceId)
                
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[dataType] = "temperature"
                    it[value] = json.encodeToString(data)
                    it[deviceId] = data.deviceId
                    it[unit] = data.unit
                    it[timestamp] = data.timestamp
                }
                
                updateDeviceLastSeen(data.deviceId)
            }
            logger.debug("體溫數據已保存: 患者=${data.patientId}")
        } catch (e: Exception) {
            logger.error("保存體溫數據失敗", e)
        }
    }
    
    // 保存尿布數據
    suspend fun saveDiaperData(data: DiaperData) {
        try {
            transaction {
                val patientId = ensurePatientExists(data.patientId, data.deviceId)
                
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[dataType] = "diaper"
                    it[value] = json.encodeToString(data)
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                }
                
                updateDeviceLastSeen(data.deviceId)
            }
            logger.debug("尿布數據已保存: 患者=${data.patientId}")
        } catch (e: Exception) {
            logger.error("保存尿布數據失敗", e)
        }
    }
    
    // 保存位置數據
    suspend fun saveLocationData(data: LocationData) {
        try {
            transaction {
                val patientId = ensurePatientExists(data.patientId, data.deviceId)
                
                // 保存到位置記錄表
                LocationRecords.insert {
                    it[LocationRecords.patientId] = patientId
                    it[x] = data.x
                    it[y] = data.y
                    it[z] = data.z ?: 0.0
                    it[accuracy] = data.accuracy
                    it[area] = data.area
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                }
                
                // 同時保存到健康記錄表
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[dataType] = "location"
                    it[value] = json.encodeToString(data)
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                }
                
                updateDeviceLastSeen(data.deviceId)
            }
            logger.debug("位置數據已保存: 患者=${data.patientId}")
        } catch (e: Exception) {
            logger.error("保存位置數據失敗", e)
        }
    }
    
    // 確保患者存在，如果不存在則創建
    private fun ensurePatientExists(patientIdStr: String, deviceId: String): Int {
        // 首先嘗試通過設備ID查找患者
        val existingPatient = Patients.select { Patients.deviceId eq deviceId }.singleOrNull()
        
        return if (existingPatient != null) {
            existingPatient[Patients.id].value
        } else {
            // 創建新患者
            Patients.insertAndGetId {
                it[name] = "患者 $patientIdStr"
                it[room] = "未分配"
                it[Patients.deviceId] = deviceId
            }.value
        }
    }
    
    // 更新設備最後見到時間
    private fun updateDeviceLastSeen(deviceId: String) {
        val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
        
        if (existingDevice != null) {
            Devices.update({ Devices.deviceId eq deviceId }) {
                it[lastSeen] = LocalDateTime.now()
                it[updatedAt] = LocalDateTime.now()
            }
        } else {
            // 創建新設備記錄
            Devices.insert {
                it[Devices.deviceId] = deviceId
                it[deviceType] = "sensor"
                it[lastSeen] = LocalDateTime.now()
            }
        }
    }
    
    // 更新設備狀態
    suspend fun updateDeviceStatus(deviceStatus: Map<String, Any>) {
        try {
            val deviceId = deviceStatus["deviceId"] as? String ?: return
            val status = deviceStatus["status"] as? String
            val batteryLevel = (deviceStatus["batteryLevel"] as? Number)?.toInt()
            
            transaction {
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                
                if (existingDevice != null) {
                    Devices.update({ Devices.deviceId eq deviceId }) {
                        status?.let { s -> it[Devices.status] = s }
                        batteryLevel?.let { b -> it[Devices.batteryLevel] = b }
                        it[lastSeen] = LocalDateTime.now()
                        it[updatedAt] = LocalDateTime.now()
                    }
                } else {
                    Devices.insert {
                        it[Devices.deviceId] = deviceId
                        it[deviceType] = "sensor"
                        status?.let { s -> it[Devices.status] = s }
                        batteryLevel?.let { b -> it[Devices.batteryLevel] = b }
                        it[lastSeen] = LocalDateTime.now()
                    }
                }
            }
            logger.debug("設備狀態已更新: $deviceId")
        } catch (e: Exception) {
            logger.error("更新設備狀態失敗", e)
        }
    }
    
    // 創建警報
    suspend fun createAlert(
        patientId: String,
        alertType: String,
        title: String,
        message: String,
        severity: String,
        deviceId: String
    ) {
        try {
            transaction {
                val patientDbId = ensurePatientExists(patientId, deviceId)
                
                Alerts.insert {
                    it[Alerts.patientId] = patientDbId
                    it[Alerts.alertType] = alertType
                    it[Alerts.title] = title
                    it[Alerts.message] = message
                    it[Alerts.severity] = severity
                    it[Alerts.deviceId] = deviceId
                    it[triggeredAt] = LocalDateTime.now()
                }
            }
            logger.info("警報已創建: $title - 患者=$patientId")
        } catch (e: Exception) {
            logger.error("創建警報失敗", e)
        }
    }
    
    // 獲取患者的最新健康數據
    suspend fun getLatestHealthData(patientId: String): HealthDataPacket? {
        return try {
            transaction {
                val patient = Patients.select { Patients.deviceId eq patientId }.singleOrNull()
                    ?: return@transaction null
                
                val patientDbId = patient[Patients.id].value
                
                // 獲取最新的各類數據
                val latestHeartRate = getLatestDataByType(patientDbId, "heart_rate")
                val latestTemperature = getLatestDataByType(patientDbId, "temperature")
                val latestDiaper = getLatestDataByType(patientDbId, "diaper")
                val latestLocation = getLatestDataByType(patientDbId, "location")
                
                HealthDataPacket(
                    patientId = patientId,
                    heartRate = latestHeartRate?.let { json.decodeFromString<HeartRateData>(it) },
                    temperature = latestTemperature?.let { json.decodeFromString<TemperatureData>(it) },
                    diaper = latestDiaper?.let { json.decodeFromString<DiaperData>(it) },
                    location = latestLocation?.let { json.decodeFromString<LocationData>(it) },
                    timestamp = LocalDateTime.now()
                )
            }
        } catch (e: Exception) {
            logger.error("獲取患者健康數據失敗: $patientId", e)
            null
        }
    }
    
    private fun getLatestDataByType(patientDbId: Int, dataType: String): String? {
        return HealthRecords
            .select { (HealthRecords.patientId eq patientDbId) and (HealthRecords.dataType eq dataType) }
            .orderBy(HealthRecords.timestamp, SortOrder.DESC)
            .limit(1)
            .singleOrNull()
            ?.get(HealthRecords.value)
    }
    
    // 獲取患者列表
    suspend fun getAllPatients(): List<Patient> {
        return try {
            transaction {
                Patients.selectAll().map { row ->
                    Patient(
                        id = row[Patients.deviceId],
                        name = row[Patients.name],
                        room = row[Patients.room],
                        deviceId = row[Patients.deviceId],
                        age = row[Patients.age],
                        gender = row[Patients.gender],
                        emergencyContact = row[Patients.emergencyContact]
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("獲取患者列表失敗", e)
            emptyList()
        }
    }
    
    // 獲取活躍警報
    suspend fun getActiveAlerts(): List<Map<String, Any>> {
        return try {
            transaction {
                (Alerts innerJoin Patients)
                    .select { Alerts.status eq "active" }
                    .orderBy(Alerts.triggeredAt, SortOrder.DESC)
                    .map { row ->
                        mapOf(
                            "id" to row[Alerts.id].value,
                            "patientName" to row[Patients.name],
                            "patientId" to row[Patients.deviceId],
                            "alertType" to row[Alerts.alertType],
                            "title" to row[Alerts.title],
                            "message" to row[Alerts.message],
                            "severity" to row[Alerts.severity],
                            "triggeredAt" to row[Alerts.triggeredAt].toString()
                        )
                    }
            }
        } catch (e: Exception) {
            logger.error("獲取活躍警報失敗", e)
            emptyList()
        }
    }
    
    // 清理舊數據（保留最近3天）
    suspend fun cleanupOldData() {
        try {
            val cutoffTime = LocalDateTime.now().minusDays(3)
            
            transaction {
                val deletedHealthRecords = HealthRecords.deleteWhere { 
                    HealthRecords.timestamp less cutoffTime 
                }
                
                val deletedLocationRecords = LocationRecords.deleteWhere { 
                    LocationRecords.timestamp less cutoffTime 
                }
                
                val deletedAlerts = Alerts.deleteWhere { 
                    (Alerts.triggeredAt less cutoffTime) and (Alerts.status neq "active")
                }
                
                logger.info("數據清理完成 - 健康記錄: $deletedHealthRecords, 位置記錄: $deletedLocationRecords, 警報: $deletedAlerts")
            }
        } catch (e: Exception) {
            logger.error("清理舊數據失敗", e)
        }
    }
}