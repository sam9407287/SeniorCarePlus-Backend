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
            // 支援多種PostgreSQL連接格式
            var url = System.getenv("DATABASE_URL") 
                ?: System.getenv("DATABASE_PUBLIC_URL")
                ?: System.getenv("SUPABASE_DATABASE_URL")
                ?: "jdbc:postgresql://localhost:5432/seniorcareplus"
            
            // 轉換 Railway/Heroku 格式的 URL
            if (url.startsWith("postgres://")) {
                url = url.replace("postgres://", "jdbc:postgresql://")
            }
            
            // 從 URL 中提取用戶名和密碼（如果包含）
            var user = System.getenv("PGUSER") ?: System.getenv("DATABASE_USER") ?: "postgres"
            var password = System.getenv("PGPASSWORD") ?: System.getenv("DATABASE_PASSWORD") ?: "password"
            
            // 如果 URL 中包含用戶名和密碼，提取它們
            val urlPattern = Regex("jdbc:postgresql://([^:]+):([^@]+)@(.+)")
            val match = urlPattern.find(url)
            if (match != null) {
                user = match.groupValues[1]
                password = match.groupValues[2]
                url = "jdbc:postgresql://${match.groupValues[3]}"
            }
            
            logger.info("測試PostgreSQL連接...")
            logger.info("連接URL: ${url.replace(Regex(":[^:@]+@"), ":***@")}")
            logger.info("用戶: $user")
            
            val connection = DriverManager.getConnection(url, user, password)
            connection.close()
            logger.info("✅ PostgreSQL連接測試成功！")
            true
        } catch (e: Exception) {
            logger.error("❌ PostgreSQL連接測試失敗: ${e.message}", e)
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
            var databaseUrl = System.getenv("DATABASE_URL") 
                ?: System.getenv("DATABASE_PUBLIC_URL")
                ?: System.getenv("SUPABASE_DATABASE_URL")
                ?: "jdbc:postgresql://localhost:5432/seniorcareplus"
            
            // 轉換 Railway/Heroku 格式的 URL (postgres:// -> jdbc:postgresql://)
            if (databaseUrl.startsWith("postgres://")) {
                databaseUrl = databaseUrl.replace("postgres://", "jdbc:postgresql://")
                logger.info("🔄 轉換數據庫 URL 格式: postgres:// -> jdbc:postgresql://")
            }
            
            logger.info("📌 連接數據庫: ${databaseUrl.replace(Regex(":[^:@]+@"), ":***@")}")
            
            // 從 URL 中提取用戶名和密碼（如果包含）
            var username = System.getenv("PGUSER") ?: System.getenv("DATABASE_USER") ?: "postgres"
            var password = System.getenv("PGPASSWORD") ?: System.getenv("DATABASE_PASSWORD") ?: "password"
            
            val urlPattern = Regex("jdbc:postgresql://([^:]+):([^@]+)@(.+)")
            val match = urlPattern.find(databaseUrl)
            if (match != null) {
                username = match.groupValues[1]
                password = match.groupValues[2]
                databaseUrl = "jdbc:postgresql://${match.groupValues[3]}"
                logger.info("🔑 從 URL 中提取用戶認證信息")
            }
            
            logger.info("👤 數據庫用戶: $username")
            
            val config = HikariConfig().apply {
                jdbcUrl = databaseUrl
                driverClassName = "org.postgresql.Driver"
                this.username = username
                this.password = password
                
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
                
                // Supabase SSL配置
                if (jdbcUrl.contains("supabase")) {
                    addDataSourceProperty("sslmode", "require")
                }
            }
            
            val dataSource = HikariDataSource(config)
            
            // 連接到PostgreSQL數據庫
            Database.connect(dataSource)
            
            logger.info("PostgreSQL數據庫連接成功 (${config.jdbcUrl})")
            
            // 創建表格
            logger.info("⏳ 正在創建數據庫表格...")
            createTables()
            
            // 驗證表格創建
            transaction {
                val tableCount = exec("SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public'") {
                    if (it.next()) it.getInt(1) else 0
                }
                logger.info("✅ PostgreSQL數據庫表格創建完成！共 $tableCount 個表格")
            }
            
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
                // 場域管理表
                Homes,
                Floors,
                Gateways,
                Anchors,
                Tags,
                // 患者與健康數據表
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