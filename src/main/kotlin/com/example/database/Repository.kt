package com.example.database

import com.example.models.LocationData
import com.example.models.PatientEvent
import com.example.routes.PatientRecord
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.*

/**
 * 数据库操作仓库
 */
object Repository {
    private val logger = LoggerFactory.getLogger(this::class.java)
    
    /**
     * 保存或更新设备位置
     */
    fun saveDeviceLocation(locationData: LocationData): UUID {
        return transaction {
            // 检查设备是否已存在
            val existingDevice = Devices.select { Devices.deviceId eq locationData.deviceId }.firstOrNull()
            
            if (existingDevice == null) {
                // 创建新设备
                val result = Devices.insert {
                    it[deviceId] = locationData.deviceId
                    it[lastX] = locationData.x
                    it[lastY] = locationData.y
                    it[lastZ] = locationData.z
                    it[accuracy] = locationData.accuracy
                    it[batteryLevel] = locationData.batteryLevel
                    it[lastUpdated] = timestampToLocalDateTime(locationData.timestamp)
                    it[area] = locationData.area
                }
                
                val id = result.resultedValues?.firstOrNull()?.let { 
                    it.getOrNull(Devices.id)?.value 
                } ?: UUID.randomUUID()
                
                logger.info("创建新设备: ${locationData.deviceId}")
                id
            } else {
                // 获取设备ID
                val deviceId = existingDevice.getOrNull(Devices.id)?.value ?: UUID.randomUUID()
                
                // 更新现有设备
                Devices.update({ Devices.id eq deviceId }) {
                    it[lastX] = locationData.x
                    it[lastY] = locationData.y
                    it[lastZ] = locationData.z
                    it[accuracy] = locationData.accuracy
                    it[batteryLevel] = locationData.batteryLevel ?: existingDevice.getOrNull(Devices.batteryLevel) ?: 0
                    it[lastUpdated] = timestampToLocalDateTime(locationData.timestamp)
                    it[area] = locationData.area ?: existingDevice.getOrNull(Devices.area) ?: ""
                }
                
                logger.info("更新设备位置: ${locationData.deviceId}")
                deviceId
            }
        }
    }
    
    /**
     * 保存或更新患者信息
     */
    fun savePatient(patient: PatientRecord): UUID {
        return transaction {
            // 检查患者是否已存在
            val existingPatient = Patients.select { Patients.patientId eq patient.id }.firstOrNull()
            
            if (existingPatient == null) {
                // 创建新患者
                val result = Patients.insert {
                    it[patientId] = patient.id
                    it[nameZh] = patient.name
                    it[nameEn] = "" // 可以根据需要设置英文名
                    it[room] = patient.room
                    it[deviceId] = patient.currentLocation?.deviceId
                    it[createdAt] = LocalDateTime.now()
                    it[updatedAt] = LocalDateTime.now()
                }
                
                val id = result.resultedValues?.firstOrNull()?.let {
                    it.getOrNull(Patients.id)?.value 
                } ?: UUID.randomUUID()
                
                logger.info("创建新患者: ${patient.id}")
                id
            } else {
                // 获取患者ID
                val patientId = existingPatient.getOrNull(Patients.id)?.value ?: UUID.randomUUID()
                
                // 更新现有患者
                Patients.update({ Patients.id eq patientId }) {
                    it[nameZh] = patient.name
                    it[room] = patient.room
                    it[deviceId] = patient.currentLocation?.deviceId ?: existingPatient.getOrNull(Patients.deviceId)
                    it[updatedAt] = LocalDateTime.now()
                }
                
                logger.info("更新患者信息: ${patient.id}")
                patientId
            }
        }
    }
    
    /**
     * 添加患者事件
     */
    fun addPatientEvent(patientId: String, event: PatientEvent): UUID {
        return transaction {
            // 获取患者UUID
            val patientRow = Patients.select { Patients.patientId eq patientId }.firstOrNull()
                ?: throw IllegalArgumentException("患者不存在: $patientId")
                
            val patientUUID = patientRow.getOrNull(Patients.id)?.value ?: UUID.randomUUID()
            
            // 添加事件
            val result = PatientEvents.insert {
                it[PatientEvents.patientId] = patientUUID
                it[timestamp] = timestampToLocalDateTime(event.timestamp)
                it[type] = event.type
                it[description] = event.description
                it[deviceId] = event.locationData?.deviceId
                it[locationX] = event.locationData?.x
                it[locationY] = event.locationData?.y
            }
            
            val id = result.resultedValues?.firstOrNull()?.let {
                it.getOrNull(PatientEvents.id)?.value
            } ?: UUID.randomUUID()
            
            logger.info("添加患者事件: $patientId - ${event.type}")
            id
        }
    }
    
    /**
     * 记录患者生命体征
     */
    fun recordVitalSigns(patientId: String, temperature: Double? = null, 
                         heartRate: Int? = null, diaperStatus: String? = null): UUID {
        return transaction {
            // 获取患者UUID
            val patientRow = Patients.select { Patients.patientId eq patientId }.firstOrNull()
                ?: throw IllegalArgumentException("患者不存在: $patientId")
                
            val patientUUID = patientRow.getOrNull(Patients.id)?.value ?: UUID.randomUUID()
            
            // 记录生命体征
            val result = PatientVitalSigns.insert {
                it[PatientVitalSigns.patientId] = patientUUID
                it[PatientVitalSigns.timestamp] = LocalDateTime.now()
                it[PatientVitalSigns.temperature] = temperature
                it[PatientVitalSigns.heartRate] = heartRate
                it[PatientVitalSigns.diaperStatus] = diaperStatus
            }
            
            val id = result.resultedValues?.firstOrNull()?.let {
                it.getOrNull(PatientVitalSigns.id)?.value
            } ?: UUID.randomUUID()
            
            logger.info("记录患者生命体征: $patientId")
            id
        }
    }
    
    /**
     * 获取所有患者
     */
    fun getAllPatients(): List<Map<String, Any?>> {
        return transaction {
            Patients.selectAll().map { row ->
                mapOf(
                    "id" to row.getOrNull(Patients.patientId),
                    "nameZh" to row.getOrNull(Patients.nameZh),
                    "nameEn" to row.getOrNull(Patients.nameEn),
                    "room" to row.getOrNull(Patients.room),
                    "deviceId" to row.getOrNull(Patients.deviceId),
                    "createdAt" to row.getOrNull(Patients.createdAt)?.toString()
                )
            }
        }
    }
    
    /**
     * 获取患者的最新生命体征
     */
    fun getLatestVitalSigns(patientId: String): Map<String, Any?>? {
        return transaction {
            // 获取患者UUID
            val patientRow = Patients.select { Patients.patientId eq patientId }.firstOrNull()
                ?: return@transaction null
                
            val patientUUID = patientRow.getOrNull(Patients.id)?.value ?: return@transaction null
            
            // 获取最新生命体征
            PatientVitalSigns.select { PatientVitalSigns.patientId eq patientUUID }
                .orderBy(PatientVitalSigns.timestamp to SortOrder.DESC)
                .limit(1)
                .firstOrNull()
                ?.let { row ->
                    mapOf(
                        "patientId" to patientId,
                        "timestamp" to row.getOrNull(PatientVitalSigns.timestamp)?.toString(),
                        "temperature" to row.getOrNull(PatientVitalSigns.temperature),
                        "heartRate" to row.getOrNull(PatientVitalSigns.heartRate),
                        "diaperStatus" to row.getOrNull(PatientVitalSigns.diaperStatus),
                        "bloodPressureSystolic" to row.getOrNull(PatientVitalSigns.bloodPressureSystolic),
                        "bloodPressureDiastolic" to row.getOrNull(PatientVitalSigns.bloodPressureDiastolic),
                        "oxygenSaturation" to row.getOrNull(PatientVitalSigns.oxygenSaturation),
                        "respirationRate" to row.getOrNull(PatientVitalSigns.respirationRate)
                    )
                }
        }
    }
    
    /**
     * 获取患者的所有事件
     */
    fun getPatientEvents(patientId: String): List<Map<String, Any?>> {
        return transaction {
            // 获取患者UUID
            val patientRow = Patients.select { Patients.patientId eq patientId }.firstOrNull()
                ?: return@transaction emptyList<Map<String, Any?>>()
                
            val patientUUID = patientRow.getOrNull(Patients.id)?.value ?: return@transaction emptyList()
            
            // 获取事件
            PatientEvents.select { PatientEvents.patientId eq patientUUID }
                .orderBy(PatientEvents.timestamp to SortOrder.DESC)
                .map { row ->
                    mapOf(
                        "id" to row.getOrNull(PatientEvents.id)?.value?.toString(),
                        "timestamp" to row.getOrNull(PatientEvents.timestamp)?.toString(),
                        "type" to row.getOrNull(PatientEvents.type),
                        "description" to row.getOrNull(PatientEvents.description),
                        "deviceId" to row.getOrNull(PatientEvents.deviceId),
                        "locationX" to row.getOrNull(PatientEvents.locationX),
                        "locationY" to row.getOrNull(PatientEvents.locationY)
                    )
                }
        }
    }
    
    // 时间戳转LocalDateTime
    private fun timestampToLocalDateTime(timestamp: Long): LocalDateTime {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault())
    }
} 