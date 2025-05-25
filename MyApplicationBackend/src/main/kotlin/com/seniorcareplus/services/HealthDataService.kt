package com.seniorcareplus.services

import com.seniorcareplus.database.*
import com.seniorcareplus.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.util.*

/**
 * 健康數據服務類，負責處理所有健康相關數據的存儲和檢索
 */
class HealthDataService {
    private val logger = LoggerFactory.getLogger(HealthDataService::class.java)
    private val json = Json { ignoreUnknownKeys = true }
    
    /**
     * 保存心率數據
     */
    suspend fun saveHeartRateData(patientId: String, data: HeartRateData) = withContext(Dispatchers.IO) {
        try {
            transaction {
                // 確保患者存在
                ensurePatientExists(patientId, data.deviceId)
                
                // 保存心率記錄
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[recordType] = "heart_rate"
                    it[value] = data.heartRate.toString()
                    it[quality] = data.quality
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                    it[additionalData] = json.encodeToString(data)
                }
                
                // 檢查異常心率並創建警報
                checkHeartRateAlert(patientId, data.heartRate)
            }
            logger.info("心率數據保存成功: 患者=$patientId, 心率=${data.heartRate}")
        } catch (e: Exception) {
            logger.error("保存心率數據失敗: 患者=$patientId", e)
            throw e
        }
    }
    
    /**
     * 保存體溫數據
     */
    suspend fun saveTemperatureData(patientId: String, data: TemperatureData) = withContext(Dispatchers.IO) {
        try {
            transaction {
                // 確保患者存在
                ensurePatientExists(patientId, data.deviceId)
                
                // 保存體溫記錄
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[recordType] = "temperature"
                    it[value] = data.temperature.toString()
                    it[unit] = data.unit
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                    it[additionalData] = json.encodeToString(data)
                }
                
                // 檢查異常體溫並創建警報
                checkTemperatureAlert(patientId, data.temperature)
            }
            logger.info("體溫數據保存成功: 患者=$patientId, 體溫=${data.temperature}")
        } catch (e: Exception) {
            logger.error("保存體溫數據失敗: 患者=$patientId", e)
            throw e
        }
    }
    
    /**
     * 保存尿布數據
     */
    suspend fun saveDiaperData(patientId: String, data: DiaperData) = withContext(Dispatchers.IO) {
        try {
            transaction {
                // 確保患者存在
                ensurePatientExists(patientId, data.deviceId)
                
                // 保存尿布記錄
                HealthRecords.insert {
                    it[HealthRecords.patientId] = patientId
                    it[recordType] = "diaper"
                    it[value] = data.status
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                    it[additionalData] = json.encodeToString(data)
                }
                
                // 如果尿布需要更換，創建提醒
                if (data.status == "wet" || data.status == "soiled") {
                    createDiaperChangeReminder(patientId)
                }
            }
            logger.info("尿布數據保存成功: 患者=$patientId, 狀態=${data.status}")
        } catch (e: Exception) {
            logger.error("保存尿布數據失敗: 患者=$patientId", e)
            throw e
        }
    }
    
    /**
     * 保存位置數據
     */
    suspend fun saveLocationData(patientId: String, data: LocationData) = withContext(Dispatchers.IO) {
        try {
            transaction {
                // 確保患者存在
                ensurePatientExists(patientId, data.deviceId)
                
                // 保存位置記錄
                LocationRecords.insert {
                    it[LocationRecords.patientId] = patientId
                    it[x] = data.x
                    it[y] = data.y
                    it[z] = data.z
                    it[accuracy] = data.accuracy
                    it[area] = data.area
                    it[deviceId] = data.deviceId
                    it[timestamp] = data.timestamp
                }
            }
            logger.info("位置數據保存成功: 患者=$patientId, 位置=(${data.x}, ${data.y})")
        } catch (e: Exception) {
            logger.error("保存位置數據失敗: 患者=$patientId", e)
            throw e
        }
    }
    
    /**
     * 更新設備狀態
     */
    suspend fun updateDeviceStatus(deviceId: String, isOnline: Boolean) = withContext(Dispatchers.IO) {
        try {
            transaction {
                val existingDevice = Devices.select { Devices.deviceId eq deviceId }.singleOrNull()
                
                if (existingDevice != null) {
                    Devices.update({ Devices.deviceId eq deviceId }) {
                        it[Devices.isOnline] = isOnline
                        it[lastHeartbeat] = LocalDateTime.now()
                        it[updatedAt] = LocalDateTime.now()
                    }
                } else {
                    // 創建新設備記錄
                    Devices.insert {
                        it[Devices.deviceId] = deviceId
                        it[deviceType] = "unknown"
                        it[Devices.isOnline] = isOnline
                        it[lastHeartbeat] = LocalDateTime.now()
                    }
                }
            }
            logger.info("設備狀態更新: 設備=$deviceId, 在線=$isOnline")
        } catch (e: Exception) {
            logger.error("更新設備狀態失敗: 設備=$deviceId", e)
            throw e
        }
    }
    
    /**
     * 獲取患者健康數據
     */
    suspend fun getPatientHealthData(
        patientId: String,
        recordType: String? = null,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        limit: Int = 100
    ): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        transaction {
            var query = HealthRecords.select { HealthRecords.patientId eq patientId }
            
            recordType?.let { query = query.andWhere { HealthRecords.recordType eq it } }
            startTime?.let { query = query.andWhere { HealthRecords.timestamp greaterEq it } }
            endTime?.let { query = query.andWhere { HealthRecords.timestamp lessEq it } }
            
            query.orderBy(HealthRecords.timestamp, SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    mapOf(
                        "id" to row[HealthRecords.id].value,
                        "patientId" to row[HealthRecords.patientId],
                        "recordType" to row[HealthRecords.recordType],
                        "value" to row[HealthRecords.value],
                        "unit" to row[HealthRecords.unit],
                        "quality" to row[HealthRecords.quality],
                        "deviceId" to row[HealthRecords.deviceId],
                        "timestamp" to row[HealthRecords.timestamp].toString(),
                        "additionalData" to row[HealthRecords.additionalData]
                    )
                }
        }
    }
    
    /**
     * 獲取患者位置數據
     */
    suspend fun getPatientLocationData(
        patientId: String,
        startTime: LocalDateTime? = null,
        endTime: LocalDateTime? = null,
        limit: Int = 100
    ): List<Map<String, Any?>> = withContext(Dispatchers.IO) {
        transaction {
            var query = LocationRecords.select { LocationRecords.patientId eq patientId }
            
            startTime?.let { query = query.andWhere { LocationRecords.timestamp greaterEq it } }
            endTime?.let { query = query.andWhere { LocationRecords.timestamp lessEq it } }
            
            query.orderBy(LocationRecords.timestamp, SortOrder.DESC)
                .limit(limit)
                .map { row ->
                    mapOf(
                        "id" to row[LocationRecords.id].value,
                        "patientId" to row[LocationRecords.patientId],
                        "x" to row[LocationRecords.x],
                        "y" to row[LocationRecords.y],
                        "z" to row[LocationRecords.z],
                        "accuracy" to row[LocationRecords.accuracy],
                        "area" to row[LocationRecords.area],
                        "deviceId" to row[LocationRecords.deviceId],
                        "timestamp" to row[LocationRecords.timestamp].toString()
                    )
                }
        }
    }
    
    /**
     * 獲取健康統計數據
     */
    suspend fun getHealthStats(patientId: String, days: Int = 7): HealthStats = withContext(Dispatchers.IO) {
        val startTime = LocalDateTime.now().minusDays(days.toLong())
        
        transaction {
            val heartRateRecords = HealthRecords.select {
                (HealthRecords.patientId eq patientId) and
                (HealthRecords.recordType eq "heart_rate") and
                (HealthRecords.timestamp greaterEq startTime)
            }.toList()
            
            val temperatureRecords = HealthRecords.select {
                (HealthRecords.patientId eq patientId) and
                (HealthRecords.recordType eq "temperature") and
                (HealthRecords.timestamp greaterEq startTime)
            }.toList()
            
            val diaperRecords = HealthRecords.select {
                (HealthRecords.patientId eq patientId) and
                (HealthRecords.recordType eq "diaper") and
                (HealthRecords.timestamp greaterEq startTime)
            }.orderBy(HealthRecords.timestamp, SortOrder.DESC).limit(1).toList()
            
            val avgHeartRate = heartRateRecords.mapNotNull { 
                it[HealthRecords.value].toDoubleOrNull() 
            }.average().takeIf { !it.isNaN() } ?: 0.0
            
            val avgTemperature = temperatureRecords.mapNotNull { 
                it[HealthRecords.value].toDoubleOrNull() 
            }.average().takeIf { !it.isNaN() } ?: 0.0
            
            val lastDiaperChange = diaperRecords.firstOrNull()?.get(HealthRecords.timestamp)
            
            HealthStats(
                patientId = patientId,
                averageHeartRate = avgHeartRate,
                averageTemperature = avgTemperature,
                lastDiaperChange = lastDiaperChange,
                dataCount = heartRateRecords.size + temperatureRecords.size + diaperRecords.size,
                timeRange = "${days}天"
            )
        }
    }
    
    /**
     * 確保患者存在
     */
    private fun ensurePatientExists(patientId: String, deviceId: String?) {
        val existingPatient = Patients.select { Patients.patientId eq patientId }.singleOrNull()
        
        if (existingPatient == null) {
            Patients.insert {
                it[Patients.patientId] = patientId
                it[name] = "患者 $patientId"
                it[room] = "未分配"
                it[Patients.deviceId] = deviceId
            }
            logger.info("創建新患者記錄: $patientId")
        }
    }
    
    /**
     * 檢查心率異常並創建警報
     */
    private fun checkHeartRateAlert(patientId: String, heartRate: Int) {
        val severity = when {
            heartRate < 60 || heartRate > 100 -> "medium"
            heartRate < 50 || heartRate > 120 -> "high"
            heartRate < 40 || heartRate > 140 -> "critical"
            else -> null
        }
        
        severity?.let {
            createAlert(patientId, "abnormal_heart_rate", it, "心率異常: $heartRate bpm")
        }
    }
    
    /**
     * 檢查體溫異常並創建警報
     */
    private fun checkTemperatureAlert(patientId: String, temperature: Double) {
        val severity = when {
            temperature < 36.0 || temperature > 37.5 -> "medium"
            temperature < 35.0 || temperature > 38.0 -> "high"
            temperature < 34.0 || temperature > 39.0 -> "critical"
            else -> null
        }
        
        severity?.let {
            createAlert(patientId, "abnormal_temperature", it, "體溫異常: ${temperature}°C")
        }
    }
    
    /**
     * 創建警報
     */
    private fun createAlert(patientId: String, alertType: String, severity: String, message: String) {
        Alerts.insert {
            it[Alerts.patientId] = patientId
            it[Alerts.alertType] = alertType
            it[Alerts.severity] = severity
            it[Alerts.message] = message
        }
        logger.warn("創建警報: 患者=$patientId, 類型=$alertType, 嚴重程度=$severity")
    }
    
    /**
     * 創建尿布更換提醒
     */
    private fun createDiaperChangeReminder(patientId: String) {
        RemindersTable.insert {
            it[RemindersTable.patientId] = patientId
            it[title] = "尿布更換提醒"
            it[description] = "患者需要更換尿布"
            it[reminderType] = "diaper_change"
            it[scheduledTime] = LocalDateTime.now()
            it[priority] = "high"
        }
        logger.info("創建尿布更換提醒: 患者=$patientId")
    }
}