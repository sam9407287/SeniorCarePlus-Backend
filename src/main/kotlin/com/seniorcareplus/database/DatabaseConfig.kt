package com.seniorcareplus.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import org.slf4j.LoggerFactory
import java.sql.DriverManager

/**
 * 數據庫配置
 */
object DatabaseConfig {
    private val logger = LoggerFactory.getLogger(DatabaseConfig::class.java)
    
    /**
     * 初始化數據庫
     */
    fun init() {
        // 嘗試連接PostgreSQL
        if (tryConnectPostgreSQL()) {
            initPostgreSQL()
        } else {
            logger.warn("PostgreSQL連接失敗，使用H2內存數據庫")
            initH2()
        }
    }
    
    /**
     * 測試PostgreSQL連接
     */
    private fun tryConnectPostgreSQL(): Boolean {
        return try {
            val url = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/seniorcareplus"
            val user = System.getenv("DATABASE_USER") ?: "postgres"
            val password = System.getenv("DATABASE_PASSWORD") ?: "password"
            
            logger.info("測試PostgreSQL連接...")
            val connection = DriverManager.getConnection(url, user, password)
            connection.close()
            logger.info("PostgreSQL連接測試成功")
            true
        } catch (e: Exception) {
            logger.warn("PostgreSQL連接測試失敗: ${e.message}")
            false
        }
    }
    
    /**
     * 初始化PostgreSQL
     */
    private fun initPostgreSQL() {
        try {
            logger.info("正在初始化PostgreSQL數據庫...")
            
            // 配置HikariCP連接池
            val config = HikariConfig().apply {
                jdbcUrl = System.getenv("DATABASE_URL") ?: "jdbc:postgresql://localhost:5432/seniorcareplus"
                driverClassName = "org.postgresql.Driver"
                username = System.getenv("DATABASE_USER") ?: "postgres"
                password = System.getenv("DATABASE_PASSWORD") ?: "password"
                
                // 連接池配置
                maximumPoolSize = 10
                minimumIdle = 2
                connectionTimeout = 30000
                idleTimeout = 600000
                maxLifetime = 1800000
                leakDetectionThreshold = 60000
                
                // 性能配置
                addDataSourceProperty("cachePrepStmts", "true")
                addDataSourceProperty("prepStmtCacheSize", "250")
                addDataSourceProperty("prepStmtCacheSqlLimit", "2048")
            }
            
            val dataSource = HikariDataSource(config)
            
            // 連接到PostgreSQL數據庫
            Database.connect(dataSource)
            
            logger.info("PostgreSQL數據庫連接成功")
            
            // 創建表格
            createTables()
            
            logger.info("PostgreSQL數據庫表格創建完成")
            
        } catch (e: Exception) {
            logger.error("PostgreSQL數據庫初始化失敗: ${e.message}")
            throw e
        }
    }
    
    /**
     * 初始化H2數據庫
     */
    private fun initH2() {
        try {
            logger.info("正在初始化H2文件數據庫...")
            Database.connect(
                url = "jdbc:h2:file:./data/seniorcareplus;AUTO_SERVER=TRUE;MODE=PostgreSQL",
                driver = "org.h2.Driver"
            )
            
            // 創建表格
            createTables()
            
            logger.info("H2文件數據庫初始化完成 (數據保存在 ./data/seniorcareplus.mv.db)")
            
        } catch (e: Exception) {
            logger.error("H2數據庫初始化失敗: ${e.message}")
            throw e
        }
    }
    
    /**
     * 創建表格
     */
    private fun createTables() {
        transaction {
            SchemaUtils.create(
                Patients,
                HealthRecords,
                LocationRecords,
                Devices,
                Alerts,
                RemindersTable
            )
        }
    }
    
    /**
     * 創建測試數據
     */
    fun createTestData() {
        try {
            transaction {
                // 插入測試患者數據
                val testPatients = listOf(
                    Triple("張三", "101", "device_001"),
                    Triple("李四", "102", "device_002"),
                    Triple("王五", "103", "device_003"),
                    Triple("趙六", "104", "device_004"),
                    Triple("陳七", "105", "device_005")
                )
                
                testPatients.forEach { (name, room, deviceId) ->
                    // 檢查患者是否已存在
                    val existingPatient = Patients.select { Patients.deviceId eq deviceId }.singleOrNull()
                    if (existingPatient == null) {
                        Patients.insert {
                            it[Patients.name] = name
                            it[Patients.room] = room
                            it[Patients.deviceId] = deviceId
                            it[age] = (65..85).random()
                            it[gender] = if ((0..1).random() == 0) "男" else "女"
                        }
                        logger.info("創建測試患者: $name (設備ID: $deviceId)")
                    }
                }
            }
            logger.info("測試數據創建完成")
        } catch (e: Exception) {
            logger.error("創建測試數據失敗: ${e.message}")
        }
    }
} 