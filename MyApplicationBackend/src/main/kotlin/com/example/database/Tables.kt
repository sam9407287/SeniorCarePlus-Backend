package com.example.database

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.javatime.datetime
import java.time.LocalDateTime
import java.util.*

/**
 * 设备表
 */
object Devices : UUIDTable() {
    val deviceId = varchar("device_id", 50).uniqueIndex()
    val lastX = double("last_x")
    val lastY = double("last_y")
    val lastZ = double("last_z").default(0.0)
    val accuracy = double("accuracy").default(0.0)
    val batteryLevel = integer("battery_level").nullable()
    val lastUpdated = datetime("last_updated").default(LocalDateTime.now())
    val area = varchar("area", 50).nullable()
}

/**
 * 患者表
 */
object Patients : UUIDTable() {
    val patientId = varchar("patient_id", 20).uniqueIndex()
    val nameZh = varchar("name_zh", 50)
    val nameEn = varchar("name_en", 50).default("")
    val room = varchar("room", 20)
    val deviceId = varchar("device_id", 50).references(Devices.deviceId).nullable()
    val createdAt = datetime("created_at").default(LocalDateTime.now())
    val updatedAt = datetime("updated_at").default(LocalDateTime.now())
}

/**
 * 患者事件表
 */
object PatientEvents : UUIDTable() {
    val patientId = reference("patient_id", Patients.id)
    val timestamp = datetime("timestamp").default(LocalDateTime.now())
    val type = varchar("type", 30)
    val description = varchar("description", 500)
    val deviceId = varchar("device_id", 50).nullable()
    val locationX = double("location_x").nullable()
    val locationY = double("location_y").nullable()
}

/**
 * 患者生命体征记录表
 */
object PatientVitalSigns : UUIDTable() {
    val patientId = reference("patient_id", Patients.id)
    val timestamp = datetime("timestamp").default(LocalDateTime.now())
    val temperature = double("temperature").nullable()  // 体温
    val heartRate = integer("heart_rate").nullable()    // 心率
    val diaperStatus = varchar("diaper_status", 20).nullable() // 尿布状态
    val bloodPressureSystolic = integer("blood_pressure_systolic").nullable() // 收缩压
    val bloodPressureDiastolic = integer("blood_pressure_diastolic").nullable() // 舒张压
    val oxygenSaturation = integer("oxygen_saturation").nullable() // 血氧饱和度
    val respirationRate = integer("respiration_rate").nullable() // 呼吸频率
} 